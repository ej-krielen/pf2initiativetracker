package com.rekijan.initiativetrackersecondedition.feature.condition;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.util.TextBinding;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;

/**
 * Edits the (de)buffs of one character, each with a remove button: name, value (for valued
 * conditions), duration and description, or for persistent damage also the damage type, damage
 * and flat check DC. Works directly on the character's own list.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class DebuffAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DEBUFF = 0;
    private static final int TYPE_PERSISTENT_DAMAGE = 1;

    private final CharacterModel character;
    private final Runnable onChanged;

    /**
     * @param onChanged Called after a (de)buff was removed, so the encounter list can redraw.
     */
    public DebuffAdapter(CharacterModel character, Runnable onChanged) {
        this.character = character;
        this.onChanged = onChanged;
    }

    static class DebuffViewHolder extends RecyclerView.ViewHolder {
        final EditText nameEditText;
        final EditText valueEditText;
        final TextView valueLabel;
        final EditText durationEditText;
        final EditText descriptionEditText;
        final Button removeButton;

        DebuffViewHolder(View itemView) {
            super(itemView);
            nameEditText = itemView.findViewById(R.id.debuff_name_editText);
            valueEditText = itemView.findViewById(R.id.debuff_value_editText);
            valueLabel = itemView.findViewById(R.id.debuff_value_textView);
            durationEditText = itemView.findViewById(R.id.debuff_duration_editText);
            descriptionEditText = itemView.findViewById(R.id.debuff_description_editText);
            removeButton = itemView.findViewById(R.id.remove_debuff_button);
        }
    }

    static class PersistentDamageViewHolder extends RecyclerView.ViewHolder {
        final EditText nameEditText;
        final EditText typeEditText;
        final EditText damageEditText;
        final EditText dcEditText;
        final EditText durationEditText;
        final EditText descriptionEditText;
        final Button removeButton;

        PersistentDamageViewHolder(View itemView) {
            super(itemView);
            nameEditText = itemView.findViewById(R.id.persistent_damage_name_editText);
            typeEditText = itemView.findViewById(R.id.persistent_damage_type_editText);
            damageEditText = itemView.findViewById(R.id.persistent_damage_value_editText);
            dcEditText = itemView.findViewById(R.id.persistent_damage_dc_editText);
            durationEditText = itemView.findViewById(R.id.persistent_damage_duration_editText);
            descriptionEditText = itemView.findViewById(R.id.persistent_damage_description_editText);
            removeButton = itemView.findViewById(R.id.remove_persistent_damage_button);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return character.getDebuffList().get(position).isPersistentDamage() ? TYPE_PERSISTENT_DAMAGE : TYPE_DEBUFF;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_PERSISTENT_DAMAGE) {
            return new PersistentDamageViewHolder(inflater.inflate(R.layout.item_persistent_damage, parent, false));
        }
        return new DebuffViewHolder(inflater.inflate(R.layout.item_debuff, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DebuffModel debuff = character.getDebuffList().get(position);
        if (holder instanceof PersistentDamageViewHolder pd) {
            TextBinding.bindText(pd.nameEditText, debuff.getName(), debuff::setName);
            TextBinding.bindText(pd.typeEditText, debuff.getDamageType(), debuff::setDamageType);
            TextBinding.bindText(pd.damageEditText, debuff.getDamageValue(), debuff::setDamageValue);
            TextBinding.bindInt(pd.dcEditText, debuff.getDifficultyClass(), 0, DebuffModel.MAX_DC,
                    debuff::setDifficultyClass);
            TextBinding.bindInt(pd.durationEditText, debuff.getDuration(), 0, DebuffModel.MAX_DURATION,
                    debuff::setDuration);
            TextBinding.bindText(pd.descriptionEditText, debuff.getDescription(), debuff::setDescription);
            pd.removeButton.setOnClickListener(v -> removeAt(pd.getBindingAdapterPosition()));
        } else if (holder instanceof DebuffViewHolder d) {
            TextBinding.bindText(d.nameEditText, debuff.getName(), debuff::setName);
            // The value field shows for valued conditions, and for any (de)buff that has a value
            int valueVisibility = debuff.getValue() > 0 || Conditions.isValued(debuff.getName())
                    ? View.VISIBLE : View.GONE;
            d.valueEditText.setVisibility(valueVisibility);
            d.valueLabel.setVisibility(valueVisibility);
            TextBinding.bindInt(d.valueEditText, debuff.getValue(), 0, DebuffModel.MAX_VALUE, debuff::setValue);
            TextBinding.bindInt(d.durationEditText, debuff.getDuration(), 0, DebuffModel.MAX_DURATION,
                    debuff::setDuration);
            TextBinding.bindText(d.descriptionEditText, debuff.getDescription(), debuff::setDescription);
            d.removeButton.setOnClickListener(v -> removeAt(d.getBindingAdapterPosition()));
        }
    }

    @Override
    public int getItemCount() {
        return character.getDebuffList().size();
    }

    private void removeAt(int position) {
        if (position == RecyclerView.NO_POSITION) return;
        character.getDebuffList().remove(position);
        notifyItemRemoved(position);
        onChanged.run();
    }
}
