package com.rekijan.initiativetrackersecondedition.character.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.character.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.character.model.DebuffModel;
import com.rekijan.initiativetrackersecondedition.listeners.GenericTextWatcher;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom RecyclerView.Adapter for the DebuffModel class
 *
 * @author Erik-Jan Krielen ej.krielen@gmail.com
 * @since 1-6-2019
 */
public class PersistentDamageAdapter extends RecyclerView.Adapter<PersistentDamageAdapter.DebuffViewHolder> {

    private final Context context;
    // Field for the list of DebuffModels
    private ArrayList<DebuffModel> debuffs = new ArrayList<>();
    // Reference to character to which the debuffs belong
    private final CharacterModel character;

    // C'tor
    public PersistentDamageAdapter(CharacterModel character, Context context) {
        this.character = character;
        this.context = context;
    }

    public void add(DebuffModel debuffs) {
        this.debuffs.add(debuffs);
    }

    public void addAll(List<DebuffModel> debuffs) {
        this.debuffs.addAll(debuffs);
        for (DebuffModel d: debuffs) {
            d.setOverrideValue(-1);
        }
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

    public int applyAllDamage() {
        int hpAdjustment = 0;
        for (DebuffModel d: debuffs) {
            int adjustValue = getValue(d.getDamageValue());
            if (d.getOverrideValue() != -1) {
                adjustValue = d.getOverrideValue();
            }
            if (adjustValue == -1) adjustValue = 0;
            hpAdjustment += adjustValue;

        }
        return hpAdjustment;
    }


    /* ViewHolder region */
    public static class DebuffViewHolder extends RecyclerView.ViewHolder {
        CardView persistentDamageCardView;
        TextView nameTextView;
        TextView valuesTextView;
        TextView descriptionTextView;
        TextView overrideWarningTextView;
        EditText overrideEditText;
        Button applyButton;

        DebuffViewHolder(View itemView) {
            super(itemView);
            persistentDamageCardView = itemView.findViewById(R.id.persistent_damage_apply_cardView);
            nameTextView = itemView.findViewById(R.id.persistent_damage_apply_name_textView);
            valuesTextView = itemView.findViewById(R.id.persistent_damage_apply_values_textView);
            descriptionTextView = itemView.findViewById(R.id.persistent_damage_apply_description_textView);
            overrideWarningTextView = itemView.findViewById(R.id.persistent_damage_apply_override_warning_textView);
            overrideEditText = itemView.findViewById(R.id.persistent_damage_apply_overridden_editText);
            applyButton = itemView.findViewById(R.id.persistent_damage_apply_do_button);

        }
    }
    /* End of Viewholder region */

    @Override
    public DebuffViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_persistent_damage_apply, parent, false);
        return new DebuffViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final DebuffViewHolder holder, int position) {

        //Get corresponding debuff
        final DebuffModel debuff = debuffs.get(position);

        //Set TextView and EditText values
        holder.nameTextView.setText(debuff.getName());
        String values = String.format(context.getString(R.string.apply_persistent_values), debuff.getDamageType(), debuff.getDamageValue(), debuff.getDifficultyClass(), debuff.getDuration());
        holder.valuesTextView.setText(values);
        holder.descriptionTextView.setText(debuff.getDescription());
        if (TextUtils.isEmpty(debuff.getDescription())) holder.descriptionTextView.setVisibility(View.GONE);
        if (getValue((debuff.getDamageValue())) != -1) {
            holder.overrideWarningTextView.setVisibility(View.GONE);
        } else {
            holder.overrideWarningTextView.setVisibility(View.VISIBLE);
        }
        if (debuff.getOverrideValue() != -1) holder.overrideEditText.setText(debuff.getOverrideValue());

        //Remove watcher if they exist to avoid double watchers
        GenericTextWatcher oldNameWatcher = (GenericTextWatcher) holder.overrideEditText.getTag();
        if (oldNameWatcher != null) {
            holder.overrideEditText.removeTextChangedListener(oldNameWatcher);
        }
        //Add new text watchers
        GenericTextWatcher newNameWatcher = new GenericTextWatcher(debuff, holder.overrideEditText);
        holder.overrideEditText.setTag(newNameWatcher);
        holder.overrideEditText.addTextChangedListener(newNameWatcher);


        holder.applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int adjustValue = getValue(debuff.getDamageValue());
                if (debuff.getOverrideValue() != -1) adjustValue = debuff.getOverrideValue();
                if (adjustValue == -1) adjustValue = 0;
                character.setHp(character.getHp()-adjustValue);
                remove(holder.getAdapterPosition());
                notifyDataSetChanged();

            }
        });
    }

    private int getValue(String text)
    {
        if (!TextUtils.isDigitsOnly(text)) return -1;
        BigInteger maxInt = BigInteger.valueOf(Integer.MAX_VALUE);
        BigInteger value = new BigInteger(TextUtils.isEmpty(text) ? "0" : text);

        if (value.compareTo(maxInt) > 0)
        {
            value = maxInt;
        }
        return value.intValue();
    }

    @Override
    public int getItemCount() {
        return debuffs.size();
    }
}