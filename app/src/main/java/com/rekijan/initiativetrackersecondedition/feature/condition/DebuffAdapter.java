package com.rekijan.initiativetrackersecondedition.feature.condition;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.util.TextBinding;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;
import com.rekijan.initiativetrackersecondedition.feature.spell.Spells;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Edits the (de)buffs of one character, each with a remove button: name, value (for valued
 * conditions), duration and description, or for persistent damage also the damage type, damage
 * and flat check DC. A spell shows who cast it, and for the GM an End on all targets button. On
 * the GM's details a valued condition can be locked. Works directly on the character's own list.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class DebuffAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DEBUFF = 0;
    private static final int TYPE_PERSISTENT_DAMAGE = 1;

    private final CharacterModel character;
    private final Runnable onChanged;
    @Nullable
    private final Consumer<DebuffModel> onEndSpell;
    @Nullable
    private final Encounter encounter;

    /**
     * @param onChanged  Called after every edit and after a (de)buff was removed, so the encounter
     *                   list can redraw and the change is saved.
     * @param onEndSpell Ends a spell on all its targets (GM only), or null to leave the button out.
     * @param encounter  The GM's fight, for the lock of a valued condition; null leaves the lock out.
     */
    public DebuffAdapter(CharacterModel character, Runnable onChanged, @Nullable Consumer<DebuffModel> onEndSpell,
                         @Nullable Encounter encounter) {
        this.character = character;
        this.onChanged = onChanged;
        this.onEndSpell = onEndSpell;
        this.encounter = encounter;
    }

    static class DebuffViewHolder extends RecyclerView.ViewHolder {
        final EditText nameEditText;
        final EditText valueEditText;
        final View valueLabel;
        final EditText durationEditText;
        final EditText descriptionEditText;
        final View removeButton;
        final View spellRow;
        final TextView casterTextView;
        final View endAllButton;
        final View lockBlock;
        final CompoundButton lockSwitch;
        final TextView lockSpellText;
        final View lockFields;
        final EditText lockMinEditText;
        final View lockRoundsLayout;
        final EditText lockRoundsEditText;
        final View lockCounterLayout;
        final AutoCompleteTextView lockCounterDropdown;

        DebuffViewHolder(View itemView) {
            super(itemView);
            nameEditText = itemView.findViewById(R.id.debuff_name_editText);
            valueEditText = itemView.findViewById(R.id.debuff_value_editText);
            valueLabel = itemView.findViewById(R.id.debuff_value_textView);
            durationEditText = itemView.findViewById(R.id.debuff_duration_editText);
            descriptionEditText = itemView.findViewById(R.id.debuff_description_editText);
            removeButton = itemView.findViewById(R.id.remove_debuff_button);
            spellRow = itemView.findViewById(R.id.debuff_spell_row);
            casterTextView = itemView.findViewById(R.id.debuff_caster_textView);
            endAllButton = itemView.findViewById(R.id.debuff_end_all_button);
            lockBlock = itemView.findViewById(R.id.debuff_lock_block);
            lockSwitch = itemView.findViewById(R.id.debuff_lock_switch);
            lockSpellText = itemView.findViewById(R.id.debuff_lock_spell_textView);
            lockFields = itemView.findViewById(R.id.debuff_lock_fields);
            lockMinEditText = itemView.findViewById(R.id.debuff_lock_min_editText);
            lockRoundsLayout = itemView.findViewById(R.id.debuff_lock_rounds_layout);
            lockRoundsEditText = itemView.findViewById(R.id.debuff_lock_rounds_editText);
            lockCounterLayout = itemView.findViewById(R.id.debuff_lock_counter_layout);
            lockCounterDropdown = itemView.findViewById(R.id.debuff_lock_counter_dropdown);
        }
    }

    static class PersistentDamageViewHolder extends RecyclerView.ViewHolder {
        final EditText nameEditText;
        final EditText typeEditText;
        final EditText damageEditText;
        final EditText dcEditText;
        final EditText durationEditText;
        final EditText descriptionEditText;
        final View removeButton;

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
            TextBinding.bindText(pd.nameEditText, debuff.getName(), edited(debuff::setName));
            TextBinding.bindText(pd.typeEditText, debuff.getDamageType(), edited(debuff::setDamageType));
            TextBinding.bindText(pd.damageEditText, debuff.getDamageValue(), edited(debuff::setDamageValue));
            TextBinding.bindInt(pd.dcEditText, debuff.getDifficultyClass(), 0, DebuffModel.MAX_DC,
                    editedInt(debuff::setDifficultyClass));
            TextBinding.bindInt(pd.durationEditText, debuff.getDuration(), 0, DebuffModel.MAX_DURATION,
                    editedInt(debuff::setDuration));
            TextBinding.bindText(pd.descriptionEditText, debuff.getDescription(), edited(debuff::setDescription));
            pd.removeButton.setOnClickListener(v -> removeAt(pd.getBindingAdapterPosition()));
        } else if (holder instanceof DebuffViewHolder d) {
            TextBinding.bindText(d.nameEditText, debuff.getName(), edited(debuff::setName));
            // The value field shows for valued conditions, and for any (de)buff that has a value
            int valueVisibility = debuff.getValue() > 0 || Conditions.isValued(debuff.getName())
                    ? View.VISIBLE : View.GONE;
            d.valueEditText.setVisibility(valueVisibility);
            d.valueLabel.setVisibility(valueVisibility);
            TextBinding.bindInt(d.valueEditText, debuff.getValue(), 0, DebuffModel.MAX_VALUE, editedInt(debuff::setValue));
            TextBinding.bindInt(d.durationEditText, debuff.getDuration(), 0, DebuffModel.MAX_DURATION,
                    editedInt(debuff::setDuration));
            TextBinding.bindText(d.descriptionEditText, debuff.getDescription(), edited(debuff::setDescription));
            d.removeButton.setOnClickListener(v -> removeAt(d.getBindingAdapterPosition()));

            String caster = debuff.getCasterName();
            d.spellRow.setVisibility(caster.isEmpty() ? View.GONE : View.VISIBLE);
            d.casterTextView.setText(d.itemView.getContext().getString(R.string.debuff_cast_by, caster));
            d.endAllButton.setVisibility(onEndSpell != null && debuff.getSpellId() != null ? View.VISIBLE : View.GONE);
            d.endAllButton.setOnClickListener(v -> {
                if (onEndSpell != null) onEndSpell.accept(debuff);
            });
            bindLock(d, debuff, valueVisibility == View.VISIBLE);
        }
    }

    /**
     * The lock of a valued condition, on the GM's details only: the switch, and while locked the
     * lowest value, then either the spell it lasts as long as, or its rounds and on whose turn they
     * count (this character at the end of their own turn, or another one at the start of theirs).
     */
    private void bindLock(DebuffViewHolder d, DebuffModel debuff, boolean valued) {
        d.lockBlock.setVisibility(encounter != null && valued ? View.VISIBLE : View.GONE);
        if (encounter == null || !valued) return;
        Context context = d.itemView.getContext();
        boolean locked = debuff.isLocked();
        d.lockSwitch.setOnCheckedChangeListener(null);
        d.lockSwitch.setChecked(locked);
        d.lockSwitch.setOnCheckedChangeListener((b, checked) -> {
            if (checked) {
                debuff.lock(1, 0, null, null);
            } else {
                debuff.unlock();
            }
            bindLock(d, debuff, true);
            onChanged.run();
        });
        d.lockFields.setVisibility(locked ? View.VISIBLE : View.GONE);
        if (!locked) {
            d.lockSpellText.setVisibility(View.GONE);
            d.lockCounterLayout.setVisibility(View.GONE);
            return;
        }
        TextBinding.bindInt(d.lockMinEditText, debuff.getLockMin(), 1, DebuffModel.MAX_VALUE,
                editedInt(debuff::setLockMin));
        boolean bySpell = debuff.getLockSpellId() != null;
        d.lockSpellText.setVisibility(bySpell ? View.VISIBLE : View.GONE);
        d.lockSpellText.setText(context.getString(R.string.debuff_lock_spell,
                Spells.spellName(encounter, debuff.getLockSpellId())));
        d.lockRoundsLayout.setVisibility(bySpell ? View.GONE : View.VISIBLE);
        d.lockCounterLayout.setVisibility(bySpell ? View.GONE : View.VISIBLE);
        if (bySpell) return;
        TextBinding.bindInt(d.lockRoundsEditText, debuff.getLockRounds(), 0, DebuffModel.MAX_DURATION,
                editedInt(debuff::setLockRounds));

        // This character first (null: their own turn), then everyone else in the fight
        List<String> labels = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        labels.add(context.getString(R.string.debuff_lock_counter_own, name(context, character)));
        ids.add(null);
        int selected = 0;
        for (CharacterModel c : Spells.combatants(encounter)) {
            if (c == character) continue;
            labels.add(context.getString(R.string.debuff_lock_counter_other, name(context, c)));
            ids.add(c.getStableId());
            if (c.getStableId().equals(debuff.getLockCounterId())) selected = ids.size() - 1;
        }
        d.lockCounterDropdown.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, labels));
        d.lockCounterDropdown.setText(labels.get(selected), false);
        d.lockCounterDropdown.setOnItemClickListener((parent, v, position, id) -> {
            debuff.setLockCounterId(ids.get(position));
            onChanged.run();
        });
    }

    private static String name(Context context, CharacterModel c) {
        String name = c.getCharacterName();
        return name == null || name.trim().isEmpty() ? context.getString(R.string.empty_character_name) : name;
    }

    @Override
    public int getItemCount() {
        return character.getDebuffList().size();
    }

    /** @return The setter, followed by the change callback. */
    private Consumer<String> edited(Consumer<String> setter) {
        return text -> {
            setter.accept(text);
            onChanged.run();
        };
    }

    private IntConsumer editedInt(IntConsumer setter) {
        return number -> {
            setter.accept(number);
            onChanged.run();
        };
    }

    private void removeAt(int position) {
        if (position == RecyclerView.NO_POSITION) return;
        character.getDebuffList().remove(position);
        notifyItemRemoved(position);
        onChanged.run();
    }
}
