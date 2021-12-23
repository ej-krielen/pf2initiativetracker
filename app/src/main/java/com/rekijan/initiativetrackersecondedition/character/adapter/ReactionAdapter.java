package com.rekijan.initiativetrackersecondedition.character.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;

import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.character.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.character.model.ReactionModel;
import com.rekijan.initiativetrackersecondedition.listeners.GenericTextWatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom RecyclerView.Adapter for the ReactionModel class
 *
 * @author Erik-Jan Krielen ej.krielen@gmail.com
 * @since 1-6-2019
 */
public class ReactionAdapter extends RecyclerView.Adapter<ReactionAdapter.ReactionViewHolder> {

    // Field for the list of ReactionModel
    private final ArrayList<ReactionModel> reactions = new ArrayList<>();
    // Reference to character to which the debuffs belong
    private final CharacterModel character;

    // C'tor
    public ReactionAdapter(CharacterModel character) {
        this.character = character;
    }

    public void add(ReactionModel reactions) {
        this.reactions.add(reactions);
    }

    public void addAll(List<ReactionModel> reactions) {
        this.reactions.addAll(reactions);
    }

    /**
     * Remove a reaction based on position
     *
     * @param position
     */
    public void remove(int position) {
        if (reactions.size() > position) {
            reactions.remove(position);
        }
        notifyDataSetChanged();
    }

    /**
     * Remove based on the debuff given
     *
     * @param reaction
     */
    public void remove(ReactionModel reaction) {
        for (int i = 0; i < reactions.size(); i++) {
            if (reactions.get(i) == reaction) {
                remove(i);
            }
        }
    }

    public void removeAll() {
        reactions.clear();
    }

    public ArrayList<ReactionModel> getList() {
        return reactions;
    }

    /* ViewHolder region */
    public static class ReactionViewHolder extends RecyclerView.ViewHolder {
        CardView reactionCardView;
        EditText reactionNameEditText;
        EditText reactionDescriptionEditText;
        SwitchCompat reactionAvailableSwitch;
        Button reactionRemoveButton;

        ReactionViewHolder(View itemView) {
            super(itemView);
            reactionCardView = itemView.findViewById(R.id.reaction_cardView);
            reactionNameEditText = itemView.findViewById(R.id.reaction_name_editText);
            reactionDescriptionEditText = itemView.findViewById(R.id.reaction_description_editText);
            reactionAvailableSwitch = itemView.findViewById(R.id.reactions_is_available_switch);
            reactionRemoveButton = itemView.findViewById(R.id.reaction_remove_button);
        }
    }
    /* End of Viewholder region */

    @Override
    public ReactionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.reactions_card, parent, false);
        return new ReactionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ReactionViewHolder holder, int position) {
        //Remove watcher if they exist to avoid double watchers
        GenericTextWatcher oldReactionNameWatcher = (GenericTextWatcher) holder.reactionNameEditText.getTag();
        if (oldReactionNameWatcher != null) {
            holder.reactionNameEditText.removeTextChangedListener(oldReactionNameWatcher);
        }

        GenericTextWatcher oldReactionDescriptionWatcher = (GenericTextWatcher) holder.reactionDescriptionEditText.getTag();
        if (oldReactionDescriptionWatcher != null) {
            holder.reactionDescriptionEditText.removeTextChangedListener(oldReactionDescriptionWatcher);
        }

        //Get corresponding reaction
        final ReactionModel reaction = reactions.get(position);

        //Set TextView and EditText values
        holder.reactionNameEditText.setText(reaction.getName());
        holder.reactionDescriptionEditText.setText(reaction.getDescription());

        //Add new text watchers
        GenericTextWatcher newReactionNameWatcher = new GenericTextWatcher(reaction, holder.reactionNameEditText);
        holder.reactionNameEditText.setTag(newReactionNameWatcher);
        holder.reactionNameEditText.addTextChangedListener(newReactionNameWatcher);

        GenericTextWatcher newReactionDescriptionWatcher = new GenericTextWatcher(reaction, holder.reactionDescriptionEditText);
        holder.reactionDescriptionEditText.setTag(newReactionDescriptionWatcher);
        holder.reactionDescriptionEditText.addTextChangedListener(newReactionDescriptionWatcher);

        holder.reactionAvailableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    reaction.setAvailable(true);
                } else {
                    reaction.setAvailable(false);
                }
            }
        });

        holder.reactionAvailableSwitch.setChecked(reaction.isAvailable());


        holder.reactionRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Remove from adapter and CharacterModel
                character.getReactionList().remove(holder.getAdapterPosition());
                remove(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return reactions.size();
    }
}