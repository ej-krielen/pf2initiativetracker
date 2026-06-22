package com.rekijan.initiativetrackersecondedition.features.debuffs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.data.model.DebuffModel;
import com.rekijan.initiativetrackersecondedition.common.listeners.EditTextWatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Custom RecyclerView.Adapter for editing a character's list of debuffs and conditions.
 * Supports multiple row types for standard debuffs and complex persistent damage.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class DebuffAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Constants
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static final int TYPE_DEBUFF = 0;
    private static final int TYPE_PERSISTENT_DAMAGE = 1;
    private static final int TYPE_POISON = 2;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Members
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final ArrayList<DebuffModel> debuffs = new ArrayList<>();
    private final CharacterModel character;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Constructor
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public DebuffAdapter(CharacterModel character) {
        this.character = character;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Public helpers
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void add(DebuffModel debuffs) {
        this.debuffs.add(debuffs);
    }

    public void addAll(List<DebuffModel> debuffs) {
        this.debuffs.addAll(debuffs);
    }

    public void remove(int position) {
        if (debuffs.size() > position) {
            debuffs.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void remove(DebuffModel debuff) {
        for (int i = 0; i < debuffs.size(); i++) {
            if (debuffs.get(i) == debuff) {
                remove(i);
                break;
            }
        }
    }

    public void removeAll() {
        int size = debuffs.size();
        debuffs.clear();
        notifyItemRangeRemoved(0, size);
    }

    public ArrayList<DebuffModel> getList() {
        return debuffs;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// ViewHolder region
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * ViewHolder for standard conditions with name, duration, and description.
     */
    public static class DebuffViewHolder extends RecyclerView.ViewHolder {
        CardView debuffCardView;
        EditText debuffNameEditText;
        EditText debuffDurationEditText;
        EditText debuffDescriptionEditText;
        Button removeDebuffButton;

        DebuffViewHolder(View itemView) {
            super(itemView);
            debuffCardView = itemView.findViewById(R.id.debuff_cardView);
            debuffNameEditText = itemView.findViewById(R.id.debuff_name_editText);
            debuffDurationEditText = itemView.findViewById(R.id.debuff_duration_editText);
            debuffDescriptionEditText = itemView.findViewById(R.id.debuff_description_editText);
            removeDebuffButton = itemView.findViewById(R.id.remove_debuff_button);
        }
    }

    /**
     * Specialized ViewHolder for persistent damage with additional fields for damage type and DC.
     */
    public static class PersistentDamageViewHolder extends RecyclerView.ViewHolder {
        CardView persistentDamageCardView;
        EditText persistentDamageNameEditText;
        EditText persistentDamageTypeEditText;
        EditText persistentDamageValueEditText;
        EditText persistentDamageDcEditText;
        EditText persistentDamageDurationEditText;
        EditText persistentDamageDescriptionEditText;
        Button removePersistentDamageButton;

        PersistentDamageViewHolder(View itemView) {
            super(itemView);
            persistentDamageCardView = itemView.findViewById(R.id.persistent_damage_cardView);
            persistentDamageNameEditText = itemView.findViewById(R.id.persistent_damage_name_editText);
            persistentDamageTypeEditText = itemView.findViewById(R.id.persistent_damage_type_editText);
            persistentDamageValueEditText = itemView.findViewById(R.id.persistent_damage_value_editText);
            persistentDamageDcEditText = itemView.findViewById(R.id.persistent_damage_dc_editText);
            persistentDamageDurationEditText = itemView.findViewById(R.id.persistent_damage_duration_editText);
            persistentDamageDescriptionEditText = itemView.findViewById(R.id.persistent_damage_description_editText);
            removePersistentDamageButton = itemView.findViewById(R.id.remove_persistent_damage_button);
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// End of ViewHolder region
    ////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Override methods
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_DEBUFF) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_debuff, parent, false);
            return new DebuffViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_persistent_damage_create, parent, false);
            return new PersistentDamageViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final DebuffModel debuff = debuffs.get(holder.getBindingAdapterPosition());

        if (holder instanceof DebuffViewHolder debuffHolder) {
            bindDebuffRow(debuffHolder, debuff);
        } else if (holder instanceof PersistentDamageViewHolder persistentDamageHolder) {
            bindPersistentDamageRow(persistentDamageHolder, debuff);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isPersistentDamage(position))
            return TYPE_PERSISTENT_DAMAGE;
        return TYPE_DEBUFF;
    }

    @Override
    public int getItemCount() {
        return debuffs.size();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Private helpers
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void bindDebuffRow(DebuffViewHolder holder, DebuffModel debuff) {
        setupField(holder.debuffNameEditText, debuff.getName(), debuff::setName);
        setupIntField(holder.debuffDurationEditText, debuff.getDuration(), debuff::setDuration);
        setupField(holder.debuffDescriptionEditText, debuff.getDescription(), debuff::setDescription);

        holder.removeDebuffButton.setOnClickListener(view -> removeAtPosition(holder.getBindingAdapterPosition()));
    }

    private void bindPersistentDamageRow(PersistentDamageViewHolder holder, DebuffModel debuff) {
        setupField(holder.persistentDamageNameEditText, debuff.getName(), debuff::setName);
        setupField(holder.persistentDamageTypeEditText, debuff.getDamageType(), debuff::setDamageType);
        setupField(holder.persistentDamageValueEditText, debuff.getDamageValue(), debuff::setDamageValue);
        setupIntField(holder.persistentDamageDcEditText, debuff.getDifficultyClass(), debuff::setDifficultyClass);
        setupIntField(holder.persistentDamageDurationEditText, debuff.getDuration(), debuff::setDuration);
        setupField(holder.persistentDamageDescriptionEditText, debuff.getDescription(), debuff::setDescription);

        holder.removePersistentDamageButton.setOnClickListener(view -> removeAtPosition(holder.getBindingAdapterPosition()));
    }

    private void removeAtPosition(int position) {
        if (position != RecyclerView.NO_POSITION) {
            character.getDebuffList().remove(position);
            remove(position);
        }
    }

    private void setupField(EditText editText, String value, Consumer<String> callback) {
        editText.setText(value);

        // Remove old watcher if it exists to avoid double watchers
        Object oldWatcher = editText.getTag();
        if (oldWatcher instanceof EditTextWatcher) {
            editText.removeTextChangedListener((EditTextWatcher) oldWatcher);
        }

        // Add new watcher
        EditTextWatcher newWatcher = new EditTextWatcher(callback);
        editText.setTag(newWatcher);
        editText.addTextChangedListener(newWatcher);
    }

    private void setupIntField(EditText editText, int value, Consumer<Integer> callback) {
        setupField(editText, String.valueOf(value), text -> callback.accept(EditTextWatcher.parseInt(text)));
    }

    private boolean isPersistentDamage(int position) {
        return debuffs.get(position).isPersistentDamage();
    }
}