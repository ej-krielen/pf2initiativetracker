package com.rekijan.initiativetrackersecondedition.character.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.character.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.character.model.DebuffModel;
import com.rekijan.initiativetrackersecondedition.helper.DialogHelper;
import com.rekijan.initiativetrackersecondedition.helper.HitPointAndDyingChangeHelper;
import com.rekijan.initiativetrackersecondedition.listeners.GenericTextWatcher;
import com.rekijan.initiativetrackersecondedition.listeners.HpTextWatcher;
import com.rekijan.initiativetrackersecondedition.ui.activities.MainActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Custom RecyclerView.Adapter for the CharacterModel class
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 17-9-2015 Creation of this file
 */
public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder> {

    // Field for the list of CharacterModels
    private ArrayList<CharacterModel> characters = new ArrayList<>();
    // Passing along the activity is needed to build and populate dialogs
    private final Context context;

    // C'tor
    public CharacterAdapter(Context context) {
        this.context = context;
    }

    public void add(CharacterModel character) {
        characters.add(character);
    }

    public void addAll(List<CharacterModel> characters) {
        this.characters.addAll(characters);
    }

    /**
     * Remove a character based on position, flag another with isFirstRound if the one being removed is currently flagged thus
     * @param position
     */
    public void remove(int position) {
        if (characters.size() > position)
            if (characters.get(position).isFirstRound()) {
                {
                    if (position-1 < 0) {
                        characters.get(characters.size()-1).setIsFirstRound(true);
                    } else {
                        characters.get(position-1).setIsFirstRound(true);
                    }
                }
                characters.remove(position);
            } else {
                characters.remove(position);
            }
        this.notifyDataSetChanged();
    }

    /**
     * Remove based on the character given
     * @param character
     */
    public void remove(CharacterModel character) {
        for (int i = 0; i < characters.size(); i++) {
            if (characters.get(i) == character) {
                remove(i);
            }
        }
    }

    public ArrayList<CharacterModel> getList() {
        return characters;
    }

    /**
     * Sorts the {@link #characters} list by {@link CharacterModel#getInitiative()}. High to low, monsters going before PC if tied. <br>
     *     And make the first character the first in the round
     * @return a boolean if double initiatives were found to alert to user of this fact.
     */
    public boolean sortInitiative() {
        Collections.sort(characters, new Comparator<CharacterModel>() {
            @SuppressWarnings("UseCompareMethod")
            public int compare(CharacterModel o1, CharacterModel o2) {
                int firstCompare = o2.getInitiative() - o1.getInitiative();
                if (firstCompare != 0) {
                    return firstCompare;
                } else {
                    return Boolean.valueOf(o1.isPC()).compareTo(o2.isPC());
                }
            }
        });

        for (CharacterModel c : characters) {
            c.setIsFirstRound(false);
        }
        characters.get(0).setIsFirstRound(true);
        this.notifyDataSetChanged();

        return checkForDoubleInitiative();
    }

    /**
     * Checks if there are CharacterModels with the same initiative. Monsters go before PCs, so if one is tied with the other it does not count.
     * @return a boolean if double initiatives were found to alert to user of this fact.
     */
    private boolean checkForDoubleInitiative() {

        boolean doubleInitiativeDetected = false;
        for (CharacterModel m : characters) {
            if (doubleInitiativeDetected) break;
            for (CharacterModel m2 : characters) {
                if (m != m2 && m2.getInitiative() == m.getInitiative() && m.isPC() == m2.isPC()) {
                    doubleInitiativeDetected = true;
                    break;
                }
            }
        }
        return doubleInitiativeDetected;
    }

    /**
     * Sets the CharacterModel at the param to be the first in the round (used to track when a new round begins)
     * @param pos
     */
    public void setFirstInRound(int pos){
        for (CharacterModel c : characters) {
            c.setIsFirstRound(false);
        }
        characters.get(pos).setIsFirstRound(true);
        this.notifyDataSetChanged();
    }

    /**
     * Each {@link CharacterModel} is pushed down the list by one, bottom one becomes the top.<br>
     *     Also handles stuff that happen at the start of the round for the CharacterModel whose turn it is now.
     * @return true if its the first character in the round so the round counter can be updated
     * @param context
     */
    public boolean nextTurn(Context context) {
        //Create temporary list
        ArrayList<CharacterModel> newList = new ArrayList<>();
        //Add all the items in the new order
        for (int i = 1; i < characters.size(); i++) {
            newList.add(characters.get(i));
        }
        newList.add(characters.get(0));

        //Fill the old list with the new temporary one
        characters = newList;
        //Update the top character whose turn it is now
        CharacterModel characterModel = characters.get(0);
        characterModel.updateDebuffs(context);
        HitPointAndDyingChangeHelper.getInstance().automaticHealingCheck(characterModel, context);
        characterModel.setReactionAvailable(true);
        //TODO also set custom reactions to true
        if (characterModel.isDying()) {
            HitPointAndDyingChangeHelper.getInstance().promptRecoveryCheckDialog(characterModel, context);
        }
        this.notifyDataSetChanged();
        return characters.get(0).isFirstRound();
    }

    public void removeAll() {
        characters.clear();
    }

    /* ViewHolder region */
    public static class CharacterViewHolder extends RecyclerView.ViewHolder {
        CardView characterCardView;
        EditText characterInitiativeEditText;
        EditText characterNameEditText;
        TextView characterHpEditText;
        TextView debuffOverviewTextView;
        Button showCharacterDetailButton;
        SwitchCompat reactionSwitch;
        Button reactionsButton;
        Button collapseReactionButton;
        Button openReactionButton;
        TextView reactionLabel;
        TextView dyingLabel;
        ImageView dyingRulesImageView;
        ConstraintLayout dyingTextLayout;
        ConstraintLayout dyingImageLayout;

        CharacterViewHolder(View itemView) {
            super(itemView);
            characterCardView = itemView.findViewById(R.id.character_cardView);
            characterInitiativeEditText = itemView.findViewById(R.id.character_initiative_editText);
            characterNameEditText = itemView.findViewById(R.id.character_name_editText);
            characterHpEditText = itemView.findViewById(R.id.debuff_duration_editText);
            showCharacterDetailButton = itemView.findViewById(R.id.show_character_detail_button);
            debuffOverviewTextView = itemView.findViewById(R.id.debuff_description_editText);
            reactionSwitch = itemView.findViewById(R.id.is_reaction_available_switch);
            reactionsButton = itemView.findViewById(R.id.other_reactions_button);
            collapseReactionButton = itemView.findViewById(R.id.collapse_reaction_button);
            openReactionButton = itemView.findViewById(R.id.open_reaction_button);
            reactionLabel = itemView.findViewById(R.id.reaction_label_textView);
            dyingLabel = itemView.findViewById(R.id.char_card_dying_label);
            dyingRulesImageView = itemView.findViewById(R.id.show_dying_rules_imageView);
            dyingTextLayout = itemView.findViewById(R.id.dying_text_layout);
            dyingImageLayout = itemView.findViewById(R.id.dying_image_layout);
        }
    }
    /* End of Viewholder region */


    @Override
    public CharacterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.character_card, parent, false);
        return new CharacterViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final CharacterViewHolder holder, int position) {
        //Remove watcher if they exist to avoid double watchers
        GenericTextWatcher oldInitiativeWatcher = (GenericTextWatcher) holder.characterInitiativeEditText.getTag();
        if (oldInitiativeWatcher != null) {
            holder.characterInitiativeEditText.removeTextChangedListener(oldInitiativeWatcher);
        }
        GenericTextWatcher oldNameWatcher = (GenericTextWatcher) holder.characterNameEditText.getTag();
        if (oldNameWatcher != null) {
            holder.characterNameEditText.removeTextChangedListener(oldNameWatcher);
        }
        HpTextWatcher oldHPWatcher = (HpTextWatcher) holder.characterHpEditText.getTag();
        if (oldHPWatcher != null) {
            holder.characterHpEditText.setOnClickListener(null);
        }

        // Get the corresponding CharacterModel
        final CharacterModel character = characters.get(position);

        // Set background color depending on whether the character is a PC or NPC
        int color = character.isPC() ? context.getResources().getColor(R.color.pcColor) : context.getResources().getColor(R.color.npcColor);
        if (character.isDying()) {
            color = context.getResources().getColor(R.color.characterDying);
        }
        ((CardView) holder.itemView).setCardBackgroundColor(color);

        // Set values TextViews and EditTexts
        holder.characterInitiativeEditText.setText(String.valueOf(character.getInitiative()));

        if (character.getCharacterName() != null) {
            holder.characterNameEditText.setText(character.getCharacterName());
        }

        holder.characterHpEditText.setText(String.valueOf(character.getHp()));

        //Add new text watchers
        GenericTextWatcher newInitiativeWatcher = new GenericTextWatcher(character, holder.characterInitiativeEditText);
        holder.characterInitiativeEditText.setTag(newInitiativeWatcher);
        holder.characterInitiativeEditText.addTextChangedListener(newInitiativeWatcher);

        GenericTextWatcher newNameWatcher = new GenericTextWatcher(character, holder.characterNameEditText);
        holder.characterNameEditText.setTag(newNameWatcher);
        holder.characterNameEditText.addTextChangedListener(newNameWatcher);

        HpTextWatcher newHPWatcher = new HpTextWatcher(character, this, context);
        holder.characterHpEditText.setTag(newHPWatcher);
        holder.characterHpEditText.setOnClickListener(newHPWatcher);

        ArrayList<DebuffModel> debuffs = character.getDebuffList();
        if (debuffs.size() > 0) {
            holder.debuffOverviewTextView.setVisibility(View.VISIBLE);
            StringBuilder debuffString = new StringBuilder();
            debuffString.append(context.getResources().getString(R.string.char_card_debuffs));
            for (DebuffModel d: debuffs) {
                debuffString.append(" ");
                debuffString.append(d.getName());
                debuffString.append(" (");
                debuffString.append(d.getDuration());
                debuffString.append("), ");
            }
            debuffString.delete(debuffString.length()-2, debuffString.length()); //Remove last comma and space
            holder.debuffOverviewTextView.setText(debuffString.toString());
        } else {
            holder.debuffOverviewTextView.setVisibility(View.GONE); //Don't show TextView if list is empty
        }

        holder.showCharacterDetailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity)context).replaceCharacterDetailFragment(holder.getAdapterPosition());
            }
        });

        //Start of reaction section

        holder.collapseReactionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.collapseReactionButton.setVisibility(View.INVISIBLE);
                holder.reactionSwitch.setVisibility(View.GONE);
                holder.reactionLabel.setVisibility(View.GONE);
                holder.reactionsButton.setVisibility(View.GONE);
                holder.openReactionButton.setVisibility(View.VISIBLE);
                character.setReactionCollapsed(true);
            }
        });

        holder.openReactionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.openReactionButton.setVisibility(View.INVISIBLE);
                holder.reactionSwitch.setVisibility(View.VISIBLE);
                holder.reactionLabel.setVisibility(View.VISIBLE);
                holder.reactionsButton.setVisibility(View.VISIBLE);
                holder.collapseReactionButton.setVisibility(View.VISIBLE);
                character.setReactionCollapsed(false);
            }
        });

        holder.reactionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    character.setReactionAvailable(true);
                } else {
                    character.setReactionAvailable(false);
                }
            }
        });

        if (character.isDying()) {
            holder.collapseReactionButton.setVisibility(View.GONE);
            holder.reactionSwitch.setVisibility(View.GONE);
            holder.reactionLabel.setVisibility(View.GONE);
            holder.reactionsButton.setVisibility(View.GONE);
            holder.openReactionButton.setVisibility(View.GONE);
        } else if (character.isReactionCollapsed()) {
            holder.collapseReactionButton.setVisibility(View.INVISIBLE);
            holder.reactionSwitch.setVisibility(View.GONE);
            holder.reactionLabel.setVisibility(View.GONE);
            holder.reactionsButton.setVisibility(View.GONE);
            holder.openReactionButton.setVisibility(View.VISIBLE);
        } else {
            holder.openReactionButton.setVisibility(View.INVISIBLE);
            holder.reactionSwitch.setVisibility(View.VISIBLE);
            holder.reactionLabel.setVisibility(View.VISIBLE);
            holder.reactionsButton.setVisibility(View.VISIBLE);
            holder.collapseReactionButton.setVisibility(View.VISIBLE);
        }

        holder.reactionSwitch.setChecked(character.isReactionAvailable());

        //TODO add button if multiple reactions are active and show a list in popup to switch them on/off
        holder.reactionsButton.setVisibility(View.GONE);

        //End of reaction section

        // Begin of Dying section
        if (character.isDying()) {
            holder.dyingLabel.setVisibility(View.VISIBLE);
            holder.dyingRulesImageView.setVisibility(View.VISIBLE);
            int currentDyingValue = character.getDyingValue();
            int maxDyingValue = character.getMaxDyingValue() - character.getDoomedValue();
            String labelString = context.getString(R.string.char_card_dying_label, currentDyingValue, maxDyingValue,
                    character.getWoundedValue(), character.getDoomedValue(), (character.getRecoveryDC()+currentDyingValue));
            holder.dyingLabel.setText(labelString);

            holder.dyingTextLayout.setVisibility(View.VISIBLE);
            holder.dyingImageLayout.setVisibility(View.VISIBLE);

            holder.dyingRulesImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogHelper.getInstance().deathDyingRulesDialog(context);
                }
            });

            for (int i = 0; i < holder.dyingTextLayout.getChildCount(); i++) {
                //Only show up the the max dying value
                if (i >= character.getMaxDyingValue()) {
                    holder.dyingTextLayout.getChildAt(i).setVisibility(View.GONE);
                    holder.dyingImageLayout.getChildAt(i).setVisibility(View.GONE);
                }

                // Mark dying value
                if (i < (character.getDyingValue())) {
                    holder.dyingTextLayout.getChildAt(i).setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.dying_dying_rounded_corners, context.getTheme()));
                    ((ImageView)holder.dyingImageLayout.getChildAt(i)).setImageResource(R.drawable.ic_dying_dying);
                } else {
                    holder.dyingTextLayout.getChildAt(i).setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.dying_healthy_rounded_corners, context.getTheme()));
                    ((ImageView)holder.dyingImageLayout.getChildAt(i)).setImageResource(R.drawable.ic_dying_healthy);
                }

                //Mark doomed value
                if (character.getMaxDyingValue() - i <= character.getDoomedValue()) {
                    holder.dyingTextLayout.getChildAt(i).setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.dying_doomed_rounded_corners, context.getTheme()));
                    ((ImageView)holder.dyingImageLayout.getChildAt(i)).setImageResource(R.drawable.ic_dying_doomed);
                    ((TextView)holder.dyingTextLayout.getChildAt(i)).setText("X");
                }
            }

        } else {
            holder.dyingLabel.setVisibility(View.GONE);
            holder.dyingRulesImageView.setVisibility(View.GONE);
            holder.dyingTextLayout.setVisibility(View.GONE);
            holder.dyingImageLayout.setVisibility(View.GONE);
        }

        // End of Dying section
    }

    @Override
    public int getItemCount() {
        return characters.size();
    }
}