package com.rekijan.initiativetrackersecondedition.character.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.character.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.character.model.DebuffModel;
import com.rekijan.initiativetrackersecondedition.listeners.GenericTextWatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom RecyclerView.Adapter for the DebuffModel class
 *
 * @author Erik-Jan Krielen ej.krielen@gmail.com
 * @since 1-6-2019
 */
public class DebuffAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DEBUFF = 0;
    private static final int TYPE_PERSISTENT_DAMAGE = 1;
    private static final int TYPE_POISON = 2;

    // Field for the list of DebuffModels
    private final ArrayList<DebuffModel> debuffs = new ArrayList<>();
    // Reference to character to which the debuffs belong
    private final CharacterModel character;

    // C'tor
    public DebuffAdapter(CharacterModel character) {
        this.character = character;
    }

    public void add(DebuffModel debuffs) {
        this.debuffs.add(debuffs);
    }

    public void addAll(List<DebuffModel> debuffs) {
        this.debuffs.addAll(debuffs);
    }

    /**
     * Remove a debuff based on position
     * @param position
     */
    public void remove(int position) {
        if (debuffs.size() > position) {
            debuffs.remove(position);
        }
        notifyDataSetChanged();
    }

    /**
     * Remove based on the debuff given
     * @param debuff
     */
    public void remove(DebuffModel debuff) {
        for (int i = 0; i < debuffs.size(); i++) {
            if (debuffs.get(i) == debuff) {
                remove(i);
            }
        }
    }

    public void removeAll() {
        debuffs.clear();
    }

    public ArrayList<DebuffModel> getList() {
        return debuffs;
    }

    /* ViewHolder region */
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
    /* End of Viewholder region */

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_DEBUFF)
        {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_debuff, parent, false);
            return new DebuffViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_persistent_damage_create, parent, false);
            return new PersistentDamageViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof DebuffViewHolder)
        {
            DebuffViewHolder debuffHolder = (DebuffViewHolder) holder;
            //Remove watcher if they exist to avoid double watchers
            GenericTextWatcher oldDebuffNameWatcher = (GenericTextWatcher) debuffHolder.debuffNameEditText.getTag();
            if (oldDebuffNameWatcher != null) {
                debuffHolder.debuffNameEditText.removeTextChangedListener(oldDebuffNameWatcher);
            }
            GenericTextWatcher oldDebuffDurationWatcher = (GenericTextWatcher) debuffHolder.debuffDurationEditText.getTag();
            if (oldDebuffDurationWatcher != null) {
                debuffHolder.debuffDurationEditText.removeTextChangedListener(oldDebuffDurationWatcher);
            }
            GenericTextWatcher oldDebuffDescriptionWatcher = (GenericTextWatcher) debuffHolder.debuffDescriptionEditText.getTag();
            if (oldDebuffDescriptionWatcher != null) {
                debuffHolder.debuffDescriptionEditText.removeTextChangedListener(oldDebuffDescriptionWatcher);
            }

            //Get corresponding debuff
            final DebuffModel debuff = debuffs.get(position);

            //Set TextView and EditText values
            debuffHolder.debuffNameEditText.setText(debuff.getName());
            debuffHolder.debuffDurationEditText.setText(String.valueOf(debuff.getDuration()));
            debuffHolder.debuffDescriptionEditText.setText(debuff.getDescription());

            //Add new text watchers
            GenericTextWatcher newDebuffNameWatcher = new GenericTextWatcher(debuff, debuffHolder.debuffNameEditText);
            debuffHolder.debuffNameEditText.setTag(newDebuffNameWatcher);
            debuffHolder.debuffNameEditText.addTextChangedListener(newDebuffNameWatcher);

            GenericTextWatcher newDebuffDurationWatcher = new GenericTextWatcher(debuff, debuffHolder.debuffDurationEditText);
            debuffHolder.debuffDurationEditText.setTag(newDebuffDurationWatcher);
            debuffHolder.debuffDurationEditText.addTextChangedListener(newDebuffDurationWatcher);

            GenericTextWatcher newDebuffDescriptionWatcher = new GenericTextWatcher(debuff, debuffHolder.debuffDescriptionEditText);
            debuffHolder.debuffDescriptionEditText.setTag(newDebuffDescriptionWatcher);
            debuffHolder.debuffDescriptionEditText.addTextChangedListener(newDebuffDescriptionWatcher);

            debuffHolder.removeDebuffButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Remove from adapter and CharacterModel
                    character.getDebuffList().remove(holder.getAdapterPosition());
                    remove(holder.getAdapterPosition());
                }
            });
        }

        if (holder instanceof PersistentDamageViewHolder)
        {
            PersistentDamageViewHolder persistentDamageHolder = (PersistentDamageViewHolder) holder;
            //Remove watcher if they exist to avoid double watchers
            GenericTextWatcher oldPersistentDamageNameWatcher = (GenericTextWatcher) persistentDamageHolder.persistentDamageNameEditText.getTag();
            if (oldPersistentDamageNameWatcher != null) {
                persistentDamageHolder.persistentDamageNameEditText.removeTextChangedListener(oldPersistentDamageNameWatcher);
            }
            GenericTextWatcher oldPersistentDamageTypeWatcher = (GenericTextWatcher) persistentDamageHolder.persistentDamageTypeEditText.getTag();
            if (oldPersistentDamageTypeWatcher != null) {
                persistentDamageHolder.persistentDamageTypeEditText.removeTextChangedListener(oldPersistentDamageTypeWatcher);
            }
            GenericTextWatcher oldPersistentDamageValueWatcher = (GenericTextWatcher) persistentDamageHolder.persistentDamageValueEditText.getTag();
            if (oldPersistentDamageValueWatcher != null) {
                persistentDamageHolder.persistentDamageValueEditText.removeTextChangedListener(oldPersistentDamageValueWatcher);
            }
            GenericTextWatcher oldPersistentDamageDcWatcher = (GenericTextWatcher) persistentDamageHolder.persistentDamageDcEditText.getTag();
            if (oldPersistentDamageDcWatcher != null) {
                persistentDamageHolder.persistentDamageDcEditText.removeTextChangedListener(oldPersistentDamageDcWatcher);
            }
            GenericTextWatcher oldPersistentDamageDurationWatcher = (GenericTextWatcher) persistentDamageHolder.persistentDamageDurationEditText.getTag();
            if (oldPersistentDamageDurationWatcher != null) {
                persistentDamageHolder.persistentDamageDurationEditText.removeTextChangedListener(oldPersistentDamageDurationWatcher);
            }
            GenericTextWatcher oldPersistentDamageDescriptionWatcher = (GenericTextWatcher) persistentDamageHolder.persistentDamageDescriptionEditText.getTag();
            if (oldPersistentDamageDescriptionWatcher != null) {
                persistentDamageHolder.persistentDamageDescriptionEditText.removeTextChangedListener(oldPersistentDamageDescriptionWatcher);
            }

            //Get corresponding debuff
            final DebuffModel debuff = debuffs.get(position);

            //Set TextView and EditText values
            persistentDamageHolder.persistentDamageNameEditText.setText(debuff.getName());
            persistentDamageHolder.persistentDamageTypeEditText.setText(debuff.getDamageType());
            persistentDamageHolder.persistentDamageValueEditText.setText(debuff.getDamageValue());
            persistentDamageHolder.persistentDamageDcEditText.setText(String.valueOf(debuff.getDifficultyClass()));
            persistentDamageHolder.persistentDamageDurationEditText.setText(String.valueOf(debuff.getDuration()));
            persistentDamageHolder.persistentDamageDescriptionEditText.setText(debuff.getDescription());

            //Add new text watchers
            GenericTextWatcher newPersistentDamageNameWatcher = new GenericTextWatcher(debuff, persistentDamageHolder.persistentDamageNameEditText);
            persistentDamageHolder.persistentDamageNameEditText.setTag(newPersistentDamageNameWatcher);
            persistentDamageHolder.persistentDamageNameEditText.addTextChangedListener(newPersistentDamageNameWatcher);

            GenericTextWatcher newPersistentDamageTypeWatcher = new GenericTextWatcher(debuff, persistentDamageHolder.persistentDamageTypeEditText);
            persistentDamageHolder.persistentDamageTypeEditText.setTag(newPersistentDamageTypeWatcher);
            persistentDamageHolder.persistentDamageTypeEditText.addTextChangedListener(newPersistentDamageTypeWatcher);

            GenericTextWatcher newPersistentDamageValueWatcher = new GenericTextWatcher(debuff, persistentDamageHolder.persistentDamageValueEditText);
            persistentDamageHolder.persistentDamageValueEditText.setTag(newPersistentDamageValueWatcher);
            persistentDamageHolder.persistentDamageValueEditText.addTextChangedListener(newPersistentDamageValueWatcher);

            GenericTextWatcher newPersistentDamageDcWatcher = new GenericTextWatcher(debuff, persistentDamageHolder.persistentDamageDcEditText);
            persistentDamageHolder.persistentDamageDcEditText.setTag(newPersistentDamageDcWatcher);
            persistentDamageHolder.persistentDamageDcEditText.addTextChangedListener(newPersistentDamageDcWatcher);

            GenericTextWatcher newPersistentDamageDurationWatcher = new GenericTextWatcher(debuff, persistentDamageHolder.persistentDamageDurationEditText);
            persistentDamageHolder.persistentDamageDurationEditText.setTag(newPersistentDamageDurationWatcher);
            persistentDamageHolder.persistentDamageDurationEditText.addTextChangedListener(newPersistentDamageDurationWatcher);

            GenericTextWatcher newPersistentDamageDescriptionWatcher = new GenericTextWatcher(debuff, persistentDamageHolder.persistentDamageDescriptionEditText);
            persistentDamageHolder.persistentDamageDescriptionEditText.setTag(newPersistentDamageDescriptionWatcher);
            persistentDamageHolder.persistentDamageDescriptionEditText.addTextChangedListener(newPersistentDamageDescriptionWatcher);

            persistentDamageHolder.removePersistentDamageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Remove from adapter and CharacterModel
                    character.getDebuffList().remove(holder.getAdapterPosition());
                    remove(holder.getAdapterPosition());
                }
            });
        }


    }

    @Override
    public int getItemViewType(int position) {
        if (isPersistentDamage(position))
            return TYPE_PERSISTENT_DAMAGE;
        return TYPE_DEBUFF;
    }

    private boolean isPersistentDamage(int position) {
        return debuffs.get(position).isPersistentDamage();
    }

    @Override
    public int getItemCount() {
        return debuffs.size();
    }
}