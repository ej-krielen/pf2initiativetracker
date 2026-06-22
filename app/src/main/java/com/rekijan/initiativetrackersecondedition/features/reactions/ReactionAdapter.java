package com.rekijan.initiativetrackersecondedition.features.reactions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.data.model.ReactionModel;
import com.rekijan.initiativetrackersecondedition.common.listeners.EditTextWatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom RecyclerView.Adapter for editing a character's list of reactions.
 * Allows toggling availability and editing names/descriptions.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class ReactionAdapter extends RecyclerView.Adapter<ReactionAdapter.ReactionViewHolder> {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private final ArrayList<ReactionModel> reactions = new ArrayList<>();    private final CharacterModel character;

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Constructor    ////////////////////////////////////////////////////////////////////////////////////////////////    public ReactionAdapter(CharacterModel character) {        this.character = character;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Public helpers    ////////////////////////////////////////////////////////////////////////////////////////////////    public void add(ReactionModel reaction) {        this.reactions.add(reaction);
        notifyItemInserted(reactions.size() - 1);
    }

    public void addAll(List<ReactionModel> reactions) {
        int startPos = this.reactions.size();
        this.reactions.addAll(reactions);
        notifyItemRangeInserted(startPos, reactions.size());
    }

    public void remove(int position) {
        if (reactions.size() > position) {
            reactions.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void removeAll() {
        int size = reactions.size();
        reactions.clear();
        notifyItemRangeRemoved(0, size);
    }

    public ArrayList<ReactionModel> getList() {
        return reactions;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// ViewHolder region    ////////////////////////////////////////////////////////////////////////////////////////////////    /**     * ViewHolder for reaction items.
     */
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
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// End of ViewHolder region    ////////////////////////////////////////////////////////////////////////////////////////////////    @NonNull    @Override
    public ReactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_reactions, parent, false);
        return new ReactionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ReactionViewHolder holder, int position) {
        //Remove watcher if they exist to avoid double watchers
        EditTextWatcher oldReactionNameWatcher = (EditTextWatcher) holder.reactionNameEditText.getTag();
        if (oldReactionNameWatcher != null) {
            holder.reactionNameEditText.removeTextChangedListener(oldReactionNameWatcher);
        }

        EditTextWatcher oldReactionDescriptionWatcher = (EditTextWatcher) holder.reactionDescriptionEditText.getTag();
        if (oldReactionDescriptionWatcher != null) {
            holder.reactionDescriptionEditText.removeTextChangedListener(oldReactionDescriptionWatcher);
        }

        //Get corresponding reaction
        final ReactionModel reaction = reactions.get(position);

        //Set TextView and EditText values
        holder.reactionNameEditText.setText(reaction.getName());
        holder.reactionDescriptionEditText.setText(reaction.getDescription());

        //Add new text watchers
        EditTextWatcher newReactionNameWatcher = new EditTextWatcher(reaction::setName);
        holder.reactionNameEditText.setTag(newReactionNameWatcher);
        holder.reactionNameEditText.addTextChangedListener(newReactionNameWatcher);

        EditTextWatcher newReactionDescriptionWatcher = new EditTextWatcher(reaction::setDescription);
        holder.reactionDescriptionEditText.setTag(newReactionDescriptionWatcher);
        holder.reactionDescriptionEditText.addTextChangedListener(newReactionDescriptionWatcher);

        holder.reactionAvailableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> reaction.setAvailable(isChecked));

        holder.reactionAvailableSwitch.setChecked(reaction.isAvailable());

        holder.reactionRemoveButton.setOnClickListener(view -> {
            //Remove from adapter and CharacterModel
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos != RecyclerView.NO_POSITION) {
                character.getReactionList().remove(adapterPos);
                remove(adapterPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reactions.size();
    }
}