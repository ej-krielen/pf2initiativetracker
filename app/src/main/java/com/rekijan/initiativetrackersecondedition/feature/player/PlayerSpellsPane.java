package com.rekijan.initiativetrackersecondedition.feature.player;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.util.DialogHost;
import com.rekijan.initiativetrackersecondedition.core.util.Dialogs;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.condition.ConditionProvider;
import com.rekijan.initiativetrackersecondedition.feature.condition.ConditionSearchAdapter;
import com.rekijan.initiativetrackersecondedition.feature.condition.Conditions;
import com.rekijan.initiativetrackersecondedition.feature.spell.Spells;
import com.rekijan.initiativetrackersecondedition.feature.sync.SpellRequests;
import com.rekijan.initiativetrackersecondedition.feature.sync.SyncFragment;
import com.rekijan.initiativetrackersecondedition.feature.sync.SyncManager;

import java.util.List;
import java.util.function.Consumer;

/**
 * The Spells tab of the player's sheet. A player can't pick targets, since they never see the
 * monsters, so casting is a request: Cast spell asks the GM to cast a spell (name, rounds,
 * optionally a condition from the list with a value, and a note about the targets) and the GM
 * picks the real targets. Below it the requests still waiting for the GM, and Your spells: the
 * spells this PC has going, as the GM's copy says, with rounds left, how many targets and End,
 * which asks the GM to end it. One cast and one end request wait at a time.
 * <p>
 * Outside a session only an explanation shows. The answers come in with the GM's copy
 * ({@link SyncManager}); the player screen redraws this pane then.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 01-11-2026
 */
public class PlayerSpellsPane {

    private final View pane;
    private final SyncManager sync;
    private final String characterId;

    /**
     * @param pane The sheet_spells_pane of the player's sheet.
     */
    public PlayerSpellsPane(View pane, SyncManager sync, String characterId) {
        this.pane = pane;
        this.sync = sync;
        this.characterId = characterId;
        pane.findViewById(R.id.spells_cast_button).setOnClickListener(v -> showCast(sync, characterId));
    }

    /** Shows the session state, the waiting requests and the spells. */
    public void render() {
        Context context = pane.getContext();
        boolean joined = sync.getPlayerCode(characterId) != null;
        pane.findViewById(R.id.spells_need_session_textView).setVisibility(joined ? View.GONE : View.VISIBLE);
        pane.findViewById(R.id.spells_joined_layout).setVisibility(joined ? View.VISIBLE : View.GONE);
        if (!joined) return;

        SpellRequests.Cast cast = sync.getPendingCast(characterId);
        SpellRequests.End end = sync.getPendingEnd(characterId);
        TextView castButton = pane.findViewById(R.id.spells_cast_button);
        castButton.setEnabled(cast == null);
        castButton.setText(cast == null ? R.string.player_spells_cast : R.string.player_spells_waiting);

        ViewGroup waiting = pane.findViewById(R.id.spells_waiting_container);
        waiting.removeAllViews();
        if (cast != null) {
            String line = context.getString(R.string.player_spells_pending_cast, cast.name) + " · " + rounds(context, cast.rounds);
            if (!cast.condition.isEmpty()) {
                line += " · " + (cast.value > 0 ? cast.condition + " " + cast.value : cast.condition);
            }
            if (!cast.targets.isEmpty()) line += "\n" + context.getString(R.string.spell_request_quoted, cast.targets);
            waiting.addView(waitingRow(context, waiting, line));
        }
        if (end != null) {
            waiting.addView(waitingRow(context, waiting, context.getString(R.string.player_spells_pending_end, end.name)));
        }
        pane.findViewById(R.id.spells_waiting_header).setVisibility(waiting.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        waiting.setVisibility(waiting.getChildCount() > 0 ? View.VISIBLE : View.GONE);

        List<SpellRequests.Spell> spells = sync.getPlayerSpells(characterId);
        ViewGroup list = pane.findViewById(R.id.spells_container);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(context);
        for (SpellRequests.Spell spell : spells) {
            View row = inflater.inflate(R.layout.item_player_spell, list, false);
            ((TextView) row.findViewById(R.id.player_spell_rounds)).setText(
                    spell.rounds > 0 ? String.valueOf(spell.rounds) : "–");
            ((TextView) row.findViewById(R.id.player_spell_name)).setText(spell.name);
            String detail = rounds(context, spell.rounds) + " · "
                    + context.getResources().getQuantityString(R.plurals.player_spells_targets, spell.targets, spell.targets);
            if (!spell.added.isEmpty()) detail += " · " + spell.added;
            ((TextView) row.findViewById(R.id.player_spell_detail)).setText(detail);
            boolean asked = end != null && end.spellId.equals(spell.id);
            View endButton = row.findViewById(R.id.player_spell_end_button);
            endButton.setVisibility(asked ? View.GONE : View.VISIBLE);
            // One end request at a time
            endButton.setEnabled(end == null);
            endButton.setContentDescription(context.getString(R.string.player_spells_end_label, spell.name));
            endButton.setOnClickListener(v -> askEnd(spell));
            row.findViewById(R.id.player_spell_end_asked).setVisibility(asked ? View.VISIBLE : View.GONE);
            list.addView(row);
        }
        pane.findViewById(R.id.spells_empty_textView).setVisibility(spells.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private View waitingRow(Context context, ViewGroup parent, String text) {
        TextView row = new TextView(context);
        row.setText(text);
        row.setTextAppearance(R.style.TextAppearance_Rekijan_Body);
        row.setTextColor(MaterialColors.getColor(parent,
                com.google.android.material.R.attr.colorOnSurfaceVariant));
        row.setBackgroundResource(R.drawable.bg_spell_waiting);
        int pad = context.getResources().getDimensionPixelSize(R.dimen.space_2);
        row.setPadding(pad, pad, pad, pad);
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = pad;
        row.setLayoutParams(params);
        return row;
    }

    private void askEnd(SpellRequests.Spell spell) {
        sync.requestEnd(characterId, spell, answerToast(pane.getContext()));
    }

    /** "3 rnds", "1 rnd" or "no timer". */
    private static String rounds(Context context, int rounds) {
        return rounds > 0 ? context.getResources().getQuantityString(R.plurals.spell_rounds, rounds, rounds)
                : context.getString(R.string.player_spells_no_timer);
    }

    /**
     * The Cast spell request. Kept through a rotation; Send to GM checks the name, the duration
     * (empty, or 1 to 9999), that a condition comes from the list and its value, then sends it.
     */
    private static void showCast(SyncManager sync, String characterId) {
        DialogHost.show(c -> {
            View view = LayoutInflater.from(c).inflate(R.layout.dialog_player_cast, null);
            TextInputLayout nameLayout = view.findViewById(R.id.player_cast_name_layout);
            TextView nameInput = view.findViewById(R.id.player_cast_name_editText);
            TextInputLayout durationLayout = view.findViewById(R.id.player_cast_duration_layout);
            TextView durationInput = view.findViewById(R.id.player_cast_duration_editText);
            MaterialAutoCompleteTextView conditionInput = view.findViewById(R.id.player_cast_condition_dropdown);
            TextInputLayout conditionLayout = view.findViewById(R.id.player_cast_condition_layout);
            TextInputLayout valueLayout = view.findViewById(R.id.player_cast_value_layout);
            TextView valueInput = view.findViewById(R.id.player_cast_value_editText);
            TextView targetsInput = view.findViewById(R.id.player_cast_targets_editText);
            ConditionSearchAdapter search = new ConditionSearchAdapter(c, ConditionProvider.standardConditions(c));
            conditionInput.setAdapter(search);

            durationLayout.setSuffixText(suffix(c, 0));
            durationInput.addTextChangedListener(afterChange(s -> {
                durationLayout.setError(null);
                durationLayout.setSuffixText(suffix(c, Spells.parseDuration(s)));
            }));
            nameInput.addTextChangedListener(afterChange(s -> nameLayout.setError(null)));
            valueInput.addTextChangedListener(afterChange(s -> valueLayout.setError(null)));
            conditionInput.addTextChangedListener(afterChange(s -> {
                conditionLayout.setError(null);
                valueLayout.setVisibility(Conditions.isValued(s) ? View.VISIBLE : View.GONE);
            }));
            valueLayout.setVisibility(Conditions.isValued(conditionInput.getText().toString()) ? View.VISIBLE : View.GONE);

            AlertDialog dialog = Dialogs.builder(c)
                    .setTitle(R.string.player_spells_cast)
                    .setIcon(R.drawable.ic_spellbook)
                    .setView(view)
                    .setPositiveButton(R.string.player_cast_send, null)
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .create();
            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                boolean ok = true;
                String name = nameInput.getText().toString().trim();
                if (name.isEmpty()) {
                    nameLayout.setError(c.getString(R.string.spell_error_name));
                    ok = false;
                }
                int rounds = Spells.parseDuration(durationInput.getText().toString());
                if (rounds < 0) {
                    durationLayout.setError(c.getString(R.string.spell_error_duration, DebuffModel.MAX_DURATION));
                    ok = false;
                }
                String typed = conditionInput.getText().toString().trim();
                ConditionProvider.Condition condition = typed.isEmpty() ? null : search.find(typed);
                if (!typed.isEmpty() && condition == null) {
                    conditionLayout.setError(c.getString(R.string.player_cast_condition_error));
                    ok = false;
                }
                int value = 0;
                if (condition != null && Conditions.isValued(condition.name)) {
                    value = parseValue(valueInput.getText().toString());
                    if (value < 1) {
                        valueLayout.setError(c.getString(R.string.player_cast_value_error, DebuffModel.MAX_VALUE));
                        ok = false;
                    }
                }
                if (!ok) return;
                SpellRequests.Cast cast = new SpellRequests.Cast();
                cast.name = name;
                cast.rounds = rounds;
                cast.condition = condition != null ? condition.name : "";
                cast.value = value;
                cast.targets = targetsInput.getText().toString();
                sync.requestCast(characterId, cast, answerToast(c));
                dialog.dismiss();
            }));
            return dialog;
        });
    }

    /** Tells the player whether the request reached the GM; the screen redraws with the sync state. */
    private static SyncManager.Callback answerToast(Context context) {
        Context app = context.getApplicationContext();
        return new SyncManager.Callback() {
            @Override
            public void onSuccess() {
                Toast.makeText(app, R.string.player_spell_request_sent, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNotFound() {
                Toast.makeText(app, R.string.sync_session_gone, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(app, SyncFragment.errorMessage(e), Toast.LENGTH_LONG).show();
            }
        };
    }

    /** @return 1 to {@link DebuffModel#MAX_VALUE}, or -1 for anything else. */
    private static int parseValue(String text) {
        String t = text == null ? "" : text.trim();
        if (!t.matches("\\d{1,2}")) return -1;
        int value = Integer.parseInt(t);
        return value >= 1 && value <= DebuffModel.MAX_VALUE ? value : -1;
    }

    private static String suffix(Context context, int rounds) {
        return context.getResources().getQuantityString(R.plurals.spell_rounds_suffix, rounds == 1 ? 1 : 2);
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
}
