package com.rekijan.initiativetrackersecondedition.feature.reaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.util.TextBinding;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.ReactionModel;

/**
 * Edits the custom reactions of one character: name, description, used or not, and a remove
 * button. Works directly on the character's own list.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class ReactionAdapter extends RecyclerView.Adapter<ReactionAdapter.ReactionViewHolder> {

    private final CharacterModel character;
    private final Runnable onChanged;

    /**
     * @param onChanged Called after every edit, and after a reaction was used, restored or removed,
     *                  so the card can redraw and the change is saved.
     */
    public ReactionAdapter(CharacterModel character, Runnable onChanged) {
        this.character = character;
        this.onChanged = onChanged;
    }

    static class ReactionViewHolder extends RecyclerView.ViewHolder {
        final EditText nameEditText;
        final EditText descriptionEditText;
        final CompoundButton availableSwitch;
        final View removeButton;

        ReactionViewHolder(View itemView) {
            super(itemView);
            nameEditText = itemView.findViewById(R.id.reaction_name_editText);
            descriptionEditText = itemView.findViewById(R.id.reaction_description_editText);
            availableSwitch = itemView.findViewById(R.id.reactions_is_available_switch);
            removeButton = itemView.findViewById(R.id.reaction_remove_button);
        }
    }

    @NonNull
    @Override
    public ReactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reaction, parent, false);
        return new ReactionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ReactionViewHolder holder, int position) {
        ReactionModel reaction = character.getReactionList().get(position);
        TextBinding.bindText(holder.nameEditText, reaction.getName(), text -> {
            reaction.setName(text);
            onChanged.run();
        });
        TextBinding.bindText(holder.descriptionEditText, reaction.getDescription(), text -> {
            reaction.setDescription(text);
            onChanged.run();
        });

        // Remove the old listener first, so setChecked does not change a recycled reaction
        holder.availableSwitch.setOnCheckedChangeListener(null);
        holder.availableSwitch.setChecked(reaction.isAvailable());
        holder.availableSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            reaction.setAvailable(isChecked);
            onChanged.run();
        });

        holder.removeButton.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            character.getReactionList().remove(pos);
            notifyItemRemoved(pos);
            onChanged.run();
        });
    }

    @Override
    public int getItemCount() {
        return character.getReactionList().size();
    }
}
