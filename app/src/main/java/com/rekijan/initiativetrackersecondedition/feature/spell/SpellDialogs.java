package com.rekijan.initiativetrackersecondedition.feature.spell;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.util.DialogQueue;
import com.rekijan.initiativetrackersecondedition.core.util.Dialogs;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.condition.ConditionProvider;
import com.rekijan.initiativetrackersecondedition.feature.condition.ConditionSearchAdapter;
import com.rekijan.initiativetrackersecondedition.feature.condition.Conditions;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * The spell dialogs on the GM's encounter: Cast spell (caster, spell, conditions to add next to
 * it, targets with a value per added condition), a spell that ran out, which spells of a caster
 * who left the fight keep going, and ending a spell on all its targets. Ending one offers to
 * remove the conditions its cast added too. The rules are in {@link Spells} and {@link Encounter}.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-10-2026
 */
public final class SpellDialogs {

    /** Room for a column per added condition next to the target names. */
    private static final int MAX_ADDED_CONDITIONS = 3;

    private SpellDialogs() {
    }

    /** One condition the cast adds: its row in the dialog, and what each target gets. */
    private static final class Column {
        final View row;
        final MaterialAutoCompleteTextView field;
        final ConditionSearchAdapter search;
        final TextInputLayout lockMinLayout;
        final TextView lockMinInput;
        final TextInputLayout valueLayout;
        final TextView valueInput;
        boolean locked;
        /** Per combatant: the value, or 1/0 for ticked/unticked when the condition has no value. */
        final int[] values;
        /** Per combatant: true once its value was changed by hand, so the Value field leaves it alone. */
        final boolean[] changed;
        /** The Value field: what every target gets unless changed by hand; -1 while it isn't a valid value. */
        int defaultValue = 1;
        boolean valued;

        Column(View row, ConditionSearchAdapter search, int targets) {
            this.row = row;
            this.search = search;
            field = row.findViewById(R.id.spell_condition_dropdown);
            lockMinInput = row.findViewById(R.id.spell_condition_lock_min_editText);
            lockMinLayout = row.findViewById(R.id.spell_condition_lock_min_layout);
            valueInput = row.findViewById(R.id.spell_condition_value_editText);
            valueLayout = row.findViewById(R.id.spell_condition_value_layout);
            values = new int[targets];
            Arrays.fill(values, 1);
            changed = new boolean[targets];
        }

        String name() {
            return field.getText().toString().trim();
        }
    }

    /**
     * Cast spell. The caster is the character whose turn it is unless changed; the caster is also
     * in the target list, unticked, for spells on themselves. The active character's row is
     * outlined in gold. Add condition adds up to three conditions (type to search the condition
     * list, pre-remaster names too). A valued one has a Value field for every target and can be
     * locked while the spell lasts. Under each ticked target its added conditions show: a value
     * to raise or lower for that target (0: it doesn't get it), else a tick. The Cast button counts the ticked targets ("Cast on 3");
     * pressing it checks the name, the duration (empty, or 1 to 9999), the values and that there
     * is a target, and shows what's wrong.
     */
    public static void showCast(Context context, Encounter encounter) {
        List<CharacterModel> combatants = Spells.combatants(encounter);
        if (combatants.isEmpty()) {
            Toast.makeText(context, R.string.spell_need_characters, Toast.LENGTH_SHORT).show();
            return;
        }
        CharacterModel active = encounter.get(0);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_cast_spell, null);
        TextInputLayout nameLayout = view.findViewById(R.id.spell_name_layout);
        TextView nameInput = view.findViewById(R.id.spell_name_editText);
        TextInputLayout durationLayout = view.findViewById(R.id.spell_duration_layout);
        TextView durationInput = view.findViewById(R.id.spell_duration_editText);
        TextView descriptionInput = view.findViewById(R.id.spell_description_editText);
        TextView targetsError = view.findViewById(R.id.spell_targets_error);
        ViewGroup targetsContainer = view.findViewById(R.id.spell_targets_container);
        ViewGroup conditionsContainer = view.findViewById(R.id.spell_conditions_container);
        View valuesHint = view.findViewById(R.id.spell_targets_values_hint);
        View addConditionButton = view.findViewById(R.id.spell_add_condition_button);

        // Caster: every combatant, the active one by default
        List<String> casterNames = new ArrayList<>();
        int casterIndex = 0;
        for (int i = 0; i < combatants.size(); i++) {
            CharacterModel c = combatants.get(i);
            casterNames.add(c == active
                    ? context.getString(R.string.spell_caster_active, name(context, c)) : name(context, c));
            if (c == active) casterIndex = i;
        }
        int[] caster = {casterIndex};
        MaterialAutoCompleteTextView casterDropdown = view.findViewById(R.id.spell_caster_dropdown);
        casterDropdown.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, casterNames));
        casterDropdown.setText(casterNames.get(casterIndex), false);
        casterDropdown.setOnItemClickListener((parent, v, position, id) -> caster[0] = position);

        List<MaterialCheckBox> boxes = new ArrayList<>();
        List<ViewGroup> valueCells = new ArrayList<>();
        List<Column> columns = new ArrayList<>();
        AlertDialog dialog = Dialogs.builder(context)
                .setTitle(R.string.action_cast_spell)
                .setIcon(R.drawable.ic_spellbook)
                .setView(view)
                .setPositiveButton(R.string.spell_cast, null)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();

        // Under every ticked target, its added conditions: a value to raise or lower, or a tick
        Runnable renderColumns = () -> {
            valuesHint.setVisibility(columns.isEmpty() ? View.GONE : View.VISIBLE);
            addConditionButton.setVisibility(columns.size() >= MAX_ADDED_CONDITIONS ? View.GONE : View.VISIBLE);
            for (int t = 0; t < valueCells.size(); t++) {
                ViewGroup cells = valueCells.get(t);
                cells.removeAllViews();
                int target = t;
                for (Column column : columns) {
                    if (column.name().isEmpty()) continue;
                    if (column.valued) {
                        cells.addView(valueCell(inflater, cells, column, target));
                    } else {
                        View cell = inflater.inflate(R.layout.item_spell_tick_cell, cells, false);
                        CompoundButton tick = cell.findViewById(R.id.spell_tick_checkBox);
                        tick.setText(column.name());
                        tick.setChecked(column.values[target] > 0);
                        tick.setOnCheckedChangeListener((b, checked) -> column.values[target] = checked ? 1 : 0);
                        cells.addView(cell);
                    }
                }
                boolean show = boxes.get(target).isChecked() && cells.getChildCount() > 0;
                cells.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        };

        Runnable updateCastLabel = () -> {
            int count = 0;
            for (int i = 0; i < boxes.size(); i++) {
                if (boxes.get(i).isChecked()) count++;
                ViewGroup cells = valueCells.get(i);
                cells.setVisibility(boxes.get(i).isChecked() && cells.getChildCount() > 0 ? View.VISIBLE : View.GONE);
            }
            if (count > 0) targetsError.setVisibility(View.GONE);
            View positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positive instanceof TextView) {
                ((TextView) positive).setText(count == 0 ? context.getString(R.string.spell_cast)
                        : context.getResources().getQuantityString(R.plurals.spell_cast_on, count, count));
            }
        };

        for (CharacterModel c : combatants) {
            View row = inflater.inflate(R.layout.item_spell_target, targetsContainer, false);
            MaterialCheckBox box = row.findViewById(R.id.spell_target_checkBox);
            box.setText(name(context, c));
            box.setOnCheckedChangeListener((b, checked) -> updateCastLabel.run());
            TextView type = row.findViewById(R.id.spell_target_type);
            type.setText(c.isPC() ? R.string.char_card_pc : R.string.char_card_npc);
            type.setTextColor(ContextCompat.getColor(context, c.isPC() ? R.color.rk_pc_marker : R.color.rk_npc_marker));
            if (c == active) {
                row.findViewById(R.id.spell_target_active).setVisibility(View.VISIBLE);
                row.setBackgroundResource(R.drawable.bg_spell_target_active);
            }
            boxes.add(box);
            valueCells.add(row.findViewById(R.id.spell_target_values));
            targetsContainer.addView(row);
        }

        List<ConditionProvider.Condition> conditionList = ConditionProvider.standardConditions(context);
        addConditionButton.setOnClickListener(v -> {
            View row = inflater.inflate(R.layout.item_spell_condition, conditionsContainer, false);
            Column column = new Column(row, new ConditionSearchAdapter(context, conditionList), combatants.size());
            ImageButton lockButton = row.findViewById(R.id.spell_condition_lock_button);
            View valueRow = row.findViewById(R.id.spell_condition_value_row);
            View lockText = row.findViewById(R.id.spell_condition_lock_text);
            Runnable showLock = () -> {
                valueRow.setVisibility(column.valued ? View.VISIBLE : View.GONE);
                column.lockMinLayout.setVisibility(column.locked ? View.VISIBLE : View.GONE);
                lockText.setVisibility(column.valued && column.locked ? View.VISIBLE : View.GONE);
                lockButton.setImageTintList(ColorStateList.valueOf(MaterialColors.getColor(lockButton,
                        column.locked ? com.google.android.material.R.attr.colorSecondary
                                : com.google.android.material.R.attr.colorOnSurfaceVariant)));
                lockButton.setContentDescription(context.getString(R.string.spell_condition_lock));
            };
            column.field.setAdapter(column.search);
            column.field.addTextChangedListener(afterChange(s -> {
                boolean valued = Conditions.isValued(s);
                if (valued != column.valued) {
                    column.valued = valued;
                    showLock.run();
                }
                renderColumns.run();
            }));
            lockButton.setOnClickListener(b -> {
                column.locked = !column.locked;
                showLock.run();
            });
            column.lockMinInput.addTextChangedListener(afterChange(s -> clearError(column.lockMinLayout)));
            // The Value field sets every target that wasn't changed by hand
            column.valueInput.addTextChangedListener(afterChange(s -> {
                clearError(column.valueLayout);
                column.defaultValue = parseValue(s);
                if (column.defaultValue < 0) return;
                for (int i = 0; i < column.values.length; i++) {
                    if (!column.changed[i]) column.values[i] = column.defaultValue;
                }
                renderColumns.run();
            }));
            row.findViewById(R.id.spell_condition_remove_button).setOnClickListener(b -> {
                columns.remove(column);
                conditionsContainer.removeView(row);
                renderColumns.run();
            });
            showLock.run();
            columns.add(column);
            conditionsContainer.addView(row);
            renderColumns.run();
            column.field.requestFocus();
        });
        renderColumns.run();

        // "3 rnds", "1 rnd": the suffix follows the number
        durationInput.addTextChangedListener(afterChange(s -> {
            clearError(durationLayout);
            durationLayout.setSuffixText(suffix(context, Spells.parseDuration(s)));
        }));
        durationLayout.setSuffixText(suffix(context, 0));
        nameInput.addTextChangedListener(afterChange(s -> clearError(nameLayout)));

        dialog.setOnShowListener(d -> {
            updateCastLabel.run();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String spellName = nameInput.getText().toString().trim();
                int rounds = Spells.parseDuration(durationInput.getText().toString());
                List<CharacterModel> targets = new ArrayList<>();
                for (int i = 0; i < boxes.size(); i++) {
                    if (boxes.get(i).isChecked()) targets.add(combatants.get(i));
                }
                boolean ok = true;
                List<Spells.AddedCondition> added = new ArrayList<>();
                for (Column column : columns) {
                    if (column.name().isEmpty()) continue;
                    if (column.valued && column.defaultValue < 0) {
                        column.valueLayout.setError(context.getString(R.string.spell_error_value, DebuffModel.MAX_VALUE));
                        ok = false;
                    }
                    int lockMin = 0;
                    if (column.valued && column.locked) {
                        lockMin = parseValue(column.lockMinInput.getText().toString());
                        if (lockMin < 1) {
                            column.lockMinLayout.setError(context.getString(R.string.spell_error_value,
                                    DebuffModel.MAX_VALUE));
                            ok = false;
                        }
                    }
                    ConditionProvider.Condition known = column.search.find(column.name());
                    Spells.AddedCondition a = new Spells.AddedCondition(
                            known != null ? known.name : column.name(), known != null ? known.description : "",
                            Math.max(0, lockMin));
                    for (int i = 0; i < combatants.size(); i++) {
                        if (boxes.get(i).isChecked()) a.set(combatants.get(i), Math.max(0, column.values[i]));
                    }
                    added.add(a);
                }
                if (spellName.isEmpty()) {
                    nameLayout.setError(context.getString(R.string.spell_error_name));
                    ok = false;
                }
                if (rounds < 0) {
                    durationLayout.setError(context.getString(R.string.spell_error_duration, DebuffModel.MAX_DURATION));
                    ok = false;
                }
                if (targets.isEmpty()) {
                    targetsError.setVisibility(View.VISIBLE);
                    ok = false;
                }
                if (!ok) return;
                Spells.cast(encounter, combatants.get(caster[0]), targets, spellName, rounds,
                        descriptionInput.getText().toString(), added);
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    /**
     * A spell the active character cast ran out at the start of their turn: "Fear ended on Ogre
     * and Goblin". Remove takes it off all of them, and the conditions its cast added too while
     * that box stays ticked. Keep leaves everything (the spell stays at 0 rounds).
     */
    public static void askExpired(Context context, Encounter encounter, List<Encounter.Expired> group,
                                  Runnable onDone) {
        Runnable done = DialogQueue.once(onDone);
        List<Encounter.Expired> still = new ArrayList<>();
        for (Encounter.Expired e : group) {
            if (e.character.getDebuffList().contains(e.condition)) still.add(e);
        }
        // Ended or removed in the meantime
        if (still.isEmpty()) {
            done.run();
            return;
        }
        List<String> names = new ArrayList<>();
        for (Encounter.Expired e : still) names.add(name(context, e.character));
        String spell = spellName(context, still.get(0).condition.getName());
        View view = endView(context, context.getString(R.string.spell_expired, spell, TextUtils.join(", ", names)),
                Spells.addedConditionNames(still));
        CompoundButton alsoRemove = view.findViewById(R.id.spell_end_added_checkBox);
        Dialogs.builder(context)
                .setTitle(R.string.dialog_spell_expired_title)
                .setIcon(R.drawable.ic_spellbook)
                .setView(view)
                .setPositiveButton(R.string.spell_expired_remove, (d, w) ->
                        Spells.removeExpired(encounter, still, alsoRemove.isChecked()))
                .setNegativeButton(R.string.dialog_debuff_keep, null)
                .setOnDismissListener(d -> done.run())
                .show();
    }

    /**
     * Asks about every caster that left the fight, one at a time (which of their spells keep
     * going as spell cards), then runs {@code onDone}. Does nothing but that when no caster left.
     */
    public static void askCastersGone(Context context, Encounter encounter, Runnable onDone) {
        List<DialogQueue.Step> steps = new ArrayList<>();
        for (Encounter.CasterGone gone : encounter.takeCastersGone()) {
            steps.add(done -> askCasterGone(context, encounter, gone, done));
        }
        steps.add(done -> onDone.run());
        DialogQueue.run(steps);
    }

    /**
     * A caster left the fight: tick the spells that keep going on their initiative as spell cards
     * (all ticked to start with, so nothing is lost by just closing it); the rest end, with the
     * conditions their casts added.
     */
    private static void askCasterGone(Context context, Encounter encounter, Encounter.CasterGone gone,
                                      Runnable onDone) {
        Runnable done = DialogQueue.once(onDone);
        String caster = gone.casterName.isEmpty() ? context.getString(R.string.empty_character_name) : gone.casterName;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_caster_gone, null);
        ((TextView) view.findViewById(R.id.caster_gone_message)).setText(
                context.getString(R.string.spell_caster_gone_message, caster));
        ViewGroup list = view.findViewById(R.id.caster_gone_list);
        List<MaterialCheckBox> boxes = new ArrayList<>();
        List<String> spellIds = new ArrayList<>();
        for (CharacterModel card : gone.spellCards) {
            String spellId = Spells.spellOfCard(encounter, card);
            MaterialCheckBox box = (MaterialCheckBox) inflater.inflate(R.layout.item_approval_change, list, false);
            box.setText(context.getString(R.string.spell_caster_gone_item, name(context, card),
                    targetNames(context, encounter, spellId)));
            box.setChecked(true);
            list.addView(box);
            boxes.add(box);
            spellIds.add(spellId);
        }
        Dialogs.builder(context)
                .setTitle(context.getString(R.string.spell_caster_gone_title, caster))
                .setIcon(R.drawable.ic_spellbook)
                .setView(view)
                .setPositiveButton(R.string.spell_caster_gone_done, (d, w) -> {
                    for (int i = 0; i < boxes.size(); i++) {
                        if (!boxes.get(i).isChecked()) Spells.end(encounter, spellIds.get(i), true);
                    }
                })
                .setOnDismissListener(d -> done.run())
                .show();
    }

    /**
     * Ends a spell on every target, after asking ("Slow comes off Ogre 1, Ogre 2 and Goblin"),
     * with a ticked box to remove the conditions its cast added too. Used from a spell condition
     * on the character details, and for a spell card in the order.
     */
    public static void askEnd(Context context, Encounter encounter, String spellId, String spellName,
                              boolean fromSpellCard, @Nullable Runnable afterEnd) {
        String spell = spellName(context, spellName);
        String targets = targetNames(context, encounter, spellId);
        View view = endView(context, context.getString(fromSpellCard ? R.string.spell_end_card_message
                : R.string.spell_end_message, spell, targets), Spells.addedConditionNames(encounter, spellId));
        CompoundButton alsoRemove = view.findViewById(R.id.spell_end_added_checkBox);
        Dialogs.builder(context)
                .setTitle(context.getString(R.string.spell_end_title, spell))
                .setIcon(R.drawable.ic_spellbook)
                .setView(view)
                .setPositiveButton(R.string.spell_end, (d, w) -> {
                    Spells.end(encounter, spellId, alsoRemove.isChecked());
                    if (afterEnd != null) afterEnd.run();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    /** @return "Ogre 1, Ogre 2, Goblin": who has this spell. */
    public static String targetNames(Context context, Encounter encounter, String spellId) {
        List<String> names = new ArrayList<>();
        for (CharacterModel c : Spells.targetsOf(encounter, spellId)) names.add(name(context, c));
        return TextUtils.join(", ", names);
    }

    /** The message, and the "Also remove Frightened, Fleeing" box when conditions were added (else hidden). */
    private static View endView(Context context, String message, @Nullable String addedConditions) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_spell_end, null);
        ((TextView) view.findViewById(R.id.spell_end_message)).setText(message);
        if (addedConditions != null) {
            CompoundButton box = view.findViewById(R.id.spell_end_added_checkBox);
            box.setText(context.getString(R.string.spell_also_remove, addedConditions));
            box.setVisibility(View.VISIBLE);
        }
        return view;
    }

    /**
     * "Frightened − 2 +" under a target: lowering or raising it (0 to {@link DebuffModel#MAX_VALUE})
     * marks it as changed by hand. A value other than the condition's Value gets a gold outline.
     */
    private static View valueCell(LayoutInflater inflater, ViewGroup parent, Column column, int target) {
        View cell = inflater.inflate(R.layout.item_spell_value_cell, parent, false);
        TextView name = cell.findViewById(R.id.spell_value_name);
        TextView value = cell.findViewById(R.id.spell_value_text);
        name.setText(column.name());
        Runnable show = () -> {
            int v = Math.max(0, column.values[target]);
            value.setText(String.valueOf(v));
            cell.setBackgroundResource(v != column.defaultValue
                    ? R.drawable.bg_spell_value_cell_changed : R.drawable.bg_spell_value_cell);
            cell.setContentDescription(column.name() + " " + v);
        };
        cell.findViewById(R.id.spell_value_minus).setOnClickListener(b -> {
            column.values[target] = Math.max(0, Math.max(0, column.values[target]) - 1);
            column.changed[target] = true;
            show.run();
        });
        cell.findViewById(R.id.spell_value_plus).setOnClickListener(b -> {
            column.values[target] = Math.min(DebuffModel.MAX_VALUE, Math.max(0, column.values[target]) + 1);
            column.changed[target] = true;
            show.run();
        });
        show.run();
        return cell;
    }

    /** @return 0 to {@link DebuffModel#MAX_VALUE}, or -1 for anything else (an empty field too). */
    private static int parseValue(String text) {
        String t = text == null ? "" : text.trim();
        if (!t.matches("\\d{1,2}")) return -1;
        int value = Integer.parseInt(t);
        return value <= DebuffModel.MAX_VALUE ? value : -1;
    }

    private static void clearError(TextInputLayout layout) {
        layout.setError(null);
        layout.setErrorEnabled(false);
    }

    private static TextWatcher afterChange(Consumer<String> onChange) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onChange.accept(s.toString());
            }
        };
    }

    private static String suffix(Context context, int rounds) {
        return context.getResources().getQuantityString(R.plurals.spell_rounds_suffix, rounds == 1 ? 1 : 2);
    }

    private static String spellName(Context context, String name) {
        return TextUtils.isEmpty(name) ? context.getString(R.string.approval_condition_unnamed) : name;
    }

    private static String name(Context context, CharacterModel c) {
        String name = c.getCharacterName();
        return TextUtils.isEmpty(name) ? context.getString(R.string.empty_character_name) : name;
    }
}
