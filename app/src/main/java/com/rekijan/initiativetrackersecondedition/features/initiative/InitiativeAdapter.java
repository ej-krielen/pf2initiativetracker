package com.rekijan.initiativetrackersecondedition.features.initiative;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.common.listeners.EditTextWatcher;
import com.rekijan.initiativetrackersecondedition.common.listeners.HpTextWatcher;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.core.MainActivity;

/**
 * Custom RecyclerView.Adapter for the CharacterModel class.
 * Bridges the initiative list data to the UI rows.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 17-9-2015 Creation of this file
 */
public class InitiativeAdapter extends RecyclerView.Adapter<InitiativeAdapter.CharacterViewHolder> {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private final InitiativeManager initiativeManager;    private Context context;

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Constructor    ////////////////////////////////////////////////////////////////////////////////////////////////    public InitiativeAdapter (Context context, InitiativeManager initiativeManager) {        this.context = context;
        this.initiativeManager = initiativeManager;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods    ////////////////////////////////////////////////////////////////////////////////////////////////    public void updateActivityReference(Context context) {        this.context = context;
    }

    /**
     * Notifies the adapter that a specific character's data has changed.
     * This is more efficient than notifyDataSetChanged().
     *
     * @param character The character model that was updated.
     */
    public void notifyCharacterChanged(CharacterModel character) {
        int index = initiativeManager.getCharacters().indexOf(character);
        if (index != -1) {
            notifyItemChanged(index);
        }
    }

    public void remove(CharacterModel character) {
        int index = initiativeManager.getCharacters().indexOf(character);
        if (index != -1) {
            initiativeManager.removeCharacterAt(index);
            notifyItemRemoved(index);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// ViewHolder region    ////////////////////////////////////////////////////////////////////////////////////////////////    /**     * ViewHolder class for character items in the initiative list.
     */
    public static class CharacterViewHolder extends RecyclerView.ViewHolder {
        CardView characterCardView;
        EditText characterInitiativeEditText;
        EditText characterNameEditText;
        TextView characterHpEditText;
        TextView debuffOverviewTextView;
        Button showCharacterDetailButton;
        SwitchCompat reactionSwitch;
        TextView reactionsAvailableTextView;
        Button reactionsButton;
        Button collapseReactionButton;
        Button openReactionButton;
        TextView reactionLabel;
        TextView dyingLabel;
        ImageView dyingRulesImageView;
        ConstraintLayout dyingLayout;

        CharacterViewHolder(View itemView) {
            super(itemView);
            characterCardView = itemView.findViewById(R.id.initiative_character_cardView);
            characterInitiativeEditText = itemView.findViewById(R.id.initiative_character_initiative_editText);
            characterNameEditText = itemView.findViewById(R.id.initiative_character_name_editText);
            characterHpEditText = itemView.findViewById(R.id.initiative_character_hp_textView);
            showCharacterDetailButton = itemView.findViewById(R.id.initiative_character_detail_button);
            debuffOverviewTextView = itemView.findViewById(R.id.initiative_character_debuff_overview_textView);
            reactionSwitch = itemView.findViewById(R.id.initiative_character_reaction_available_switch);
            reactionsButton = itemView.findViewById(R.id.initiative_character_reactions_button);
            collapseReactionButton = itemView.findViewById(R.id.initiative_character_collapse_reaction_button);
            reactionsAvailableTextView = itemView.findViewById(R.id.initiative_character_reactions_available_textView);
            openReactionButton = itemView.findViewById(R.id.initiative_character_open_reaction_button);
            reactionLabel = itemView.findViewById(R.id.initiative_character_reaction_label);
            dyingLabel = itemView.findViewById(R.id.initiative_character_dying_label);
            dyingRulesImageView = itemView.findViewById(R.id.initiative_character_dying_rules_imageView);
            dyingLayout = itemView.findViewById(R.id.initiative_character_dying_layout);
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// End ViewHolder region    ////////////////////////////////////////////////////////////////////////////////////////////////    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Override methods    ////////////////////////////////////////////////////////////////////////////////////////////////    @NonNull    @Override
    public CharacterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_initiative_item, parent, false);
        return new CharacterViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final CharacterViewHolder holder, int position) {
        // Remove watcher if they exist to avoid double watchers
        EditTextWatcher oldInitiativeWatcher = (EditTextWatcher) holder.characterInitiativeEditText.getTag();
        if (oldInitiativeWatcher != null) {
            holder.characterInitiativeEditText.removeTextChangedListener(oldInitiativeWatcher);
        }
        EditTextWatcher oldNameWatcher = (EditTextWatcher) holder.characterNameEditText.getTag();
        if (oldNameWatcher != null) {
            holder.characterNameEditText.removeTextChangedListener(oldNameWatcher);
        }
        HpTextWatcher oldHPWatcher = (HpTextWatcher) holder.characterHpEditText.getTag();
        if (oldHPWatcher != null) {
            holder.characterHpEditText.setOnClickListener(null);
        }

        // Get the corresponding CharacterModel
        final CharacterModel character = initiativeManager.getCharacters().get(position);

        // Bind data using the delegate
        CharacterCardBinder.getInstance().bindCard(context, holder, character);

        // Set values TextViews and EditTexts (The simple stuff)
        holder.characterInitiativeEditText.setText("" + character.getInitiative());

        if (character.getCharacterName() != null) {
            holder.characterNameEditText.setText(character.getCharacterName());
        }

        holder.characterHpEditText.setText("" + character.getHp());

        //Add new text watchers
        EditTextWatcher newInitiativeWatcher = new EditTextWatcher(text ->
                character.setInitiative(EditTextWatcher.parseInt(text)));
        holder.characterInitiativeEditText.setTag(newInitiativeWatcher);
        holder.characterInitiativeEditText.addTextChangedListener(newInitiativeWatcher);

        EditTextWatcher newNameWatcher = new EditTextWatcher(character::setCharacterName);
        holder.characterNameEditText.setTag(newNameWatcher);
        holder.characterNameEditText.addTextChangedListener(newNameWatcher);

        HpTextWatcher newHPWatcher = new HpTextWatcher(character, this, context);
        holder.characterHpEditText.setTag(newHPWatcher);
        holder.characterHpEditText.setOnClickListener(newHPWatcher);

        holder.showCharacterDetailButton.setOnClickListener(view -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                ((MainActivity)context).replaceCharacterDetailFragment(currentPosition);
            }
        });

        holder.reactionsButton.setOnClickListener(view -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                ((MainActivity)context).replaceReactionDetailFragment(currentPosition);
            }
        });

        holder.collapseReactionButton.setOnClickListener(v -> {
            character.setReactionCollapsed(true);
            notifyItemChanged(holder.getBindingAdapterPosition());
        });

        holder.openReactionButton.setOnClickListener(v -> {
            character.setReactionCollapsed(false);
            notifyItemChanged(holder.getBindingAdapterPosition());
        });

        holder.reactionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> character.setReactionAvailable(isChecked));
    }

    @Override
    public int getItemCount() {
        return initiativeManager.getCharacters().size();
    }
}