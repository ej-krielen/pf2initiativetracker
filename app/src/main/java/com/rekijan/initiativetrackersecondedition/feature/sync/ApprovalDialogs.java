package com.rekijan.initiativetrackersecondedition.feature.sync;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.color.MaterialColors;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.util.Dialogs;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;

import java.util.ArrayList;
import java.util.List;

/**
 * The GM's popups for Table sync requests. A new player: "Valeros wants to join" with their
 * stats, Add to party or Reject, or Link to a PC the GM ran so far. A known player: a checklist
 * with one row per change, all ticked; the GM unticks what they don't accept and taps Apply,
 * or Refuse all. Neither can be dismissed without an answer, since the player waits for it.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-10-2026
 */
public final class ApprovalDialogs {

    /** What the GM decided. */
    public interface Listener {
        void onApprove(List<SyncDiff.Change> accepted);

        void onRefuse();

        /** A new player is linked to this PC instead of being added. */
        void onLink(CharacterModel pc);
    }

    /** Values longer than this are shown as "changed" with the new text below. */
    private static final int SHORT_VALUE = 24;

    private ApprovalDialogs() {
    }

    /**
     * A new player. When the party has PCs they could be linked to, the GM picks one of those or
     * a new PC; a PC with the same name is picked at first ({@link SyncRules#defaultLink}).
     *
     * @return The shown dialog, so the caller knows when it is gone.
     */
    public static AlertDialog showJoin(Context context, SyncManager.Request request, String partyName,
                                       Listener listener) {
        PlayerSheet sheet = request.sheet;
        String stats = context.getString(R.string.approval_join_stats, sheet.initiative, sheet.hp, sheet.maxHp,
                sheet.ac.isEmpty() ? "–" : sheet.ac);
        String conditions = context.getResources().getQuantityString(
                R.plurals.approval_join_conditions, sheet.conditions.size(), sheet.conditions.size());
        String message = context.getString(R.string.approval_join_message, sheet.name, partyName)
                + "\n\n" + stats + "\n" + conditions;
        if (request.removedBefore) message += "\n\n" + context.getString(R.string.approval_join_removed_before);

        List<CharacterModel> candidates = request.linkCandidates;
        if (candidates.isEmpty()) {
            return Dialogs.builder(context)
                    .setTitle(R.string.approval_join_title)
                    .setMessage(message)
                    .setCancelable(false)
                    .setNegativeButton(R.string.approval_reject, (d, w) -> listener.onRefuse())
                    .setPositiveButton(R.string.approval_add, (d, w) -> listener.onApprove(new ArrayList<>()))
                    .show();
        }

        View content = LayoutInflater.from(context).inflate(R.layout.dialog_join, null);
        ((TextView) content.findViewById(R.id.join_message_textView)).setText(message);
        RadioGroup choices = content.findViewById(R.id.join_choices);
        for (int i = 0; i <= candidates.size(); i++) {
            RadioButton choice = new RadioButton(context);
            choice.setId(View.generateViewId());
            choice.setTag(i);
            choice.setMinHeight(context.getResources().getDimensionPixelSize(R.dimen.touch_target));
            choice.setText(i < candidates.size() ? linkText(context, candidates.get(i))
                    : context.getString(R.string.approval_add_new));
            choices.addView(choice);
        }
        int first = SyncRules.defaultLink(candidates, sheet.name);
        choices.check(choices.getChildAt(first >= 0 ? first : candidates.size()).getId());

        AlertDialog dialog = Dialogs.builder(context)
                .setTitle(R.string.approval_join_title)
                .setView(content)
                .setCancelable(false)
                .setNegativeButton(R.string.approval_reject, (d, w) -> listener.onRefuse())
                .setPositiveButton(R.string.approval_next, (d, w) -> {
                    int picked = (int) content.findViewById(choices.getCheckedRadioButtonId()).getTag();
                    if (picked < candidates.size()) {
                        listener.onLink(candidates.get(picked));
                    } else {
                        listener.onApprove(new ArrayList<>());
                    }
                })
                .create();
        Runnable updateNext = () -> {
            Button next = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (next == null) return;
            int picked = (int) content.findViewById(choices.getCheckedRadioButtonId()).getTag();
            next.setText(picked < candidates.size() ? R.string.approval_next : R.string.approval_add);
        };
        choices.setOnCheckedChangeListener((group, id) -> updateNext.run());
        dialog.setOnShowListener(d -> updateNext.run());
        dialog.show();
        return dialog;
    }

    /** E.g. "Link to Kyra (yours, not synced)". */
    private static String linkText(Context context, CharacterModel pc) {
        String name = TextUtils.isEmpty(pc.getCharacterName())
                ? context.getString(R.string.empty_character_name) : pc.getCharacterName();
        String kind = context.getString(SyncRules.isLinked(pc.getSyncId())
                ? R.string.approval_link_player_left : R.string.approval_link_your_pc);
        return context.getString(R.string.approval_link_to, name) + " (" + kind + ")";
    }

    /** @return The shown dialog, so the caller knows when it is gone. */
    public static AlertDialog showChanges(Context context, String name, List<SyncDiff.Change> changes, Listener listener) {
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_approval, null);
        LinearLayout list = content.findViewById(R.id.approval_list);
        List<CheckBox> boxes = new ArrayList<>();
        int muted = MaterialColors.getColor(content, com.google.android.material.R.attr.colorOnSurfaceVariant);
        for (SyncDiff.Change change : changes) {
            CheckBox box = (CheckBox) LayoutInflater.from(context).inflate(R.layout.item_approval_change, list, false);
            box.setText(text(context, change, muted));
            box.setChecked(true);
            list.addView(box);
            boxes.add(box);
        }

        AlertDialog dialog = Dialogs.builder(context)
                .setTitle(context.getResources().getQuantityString(R.plurals.approval_changes_title,
                        changes.size(), name, changes.size()))
                .setView(content)
                .setCancelable(false)
                .setNegativeButton(R.string.approval_refuse_all, (d, w) -> listener.onRefuse())
                .setPositiveButton(R.string.approval_apply, null)
                .create();
        Runnable updateApply = () -> {
            int count = 0;
            for (CheckBox b : boxes) if (b.isChecked()) count++;
            Button apply = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (apply != null) apply.setText(context.getString(R.string.approval_apply_count, count));
        };
        for (CheckBox b : boxes) b.setOnCheckedChangeListener((v, checked) -> updateApply.run());
        dialog.setOnShowListener(d -> {
            updateApply.run();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                List<SyncDiff.Change> accepted = new ArrayList<>();
                for (int i = 0; i < boxes.size(); i++) {
                    if (boxes.get(i).isChecked()) accepted.add(changes.get(i));
                }
                // Answer first, then close: closing shows the next request
                if (accepted.isEmpty()) {
                    // Nothing ticked is the same as refusing all
                    listener.onRefuse();
                } else {
                    listener.onApprove(accepted);
                }
                dialog.dismiss();
            });
        });
        dialog.show();
        return dialog;
    }

    /** @return The change as a line, with a smaller muted line below for long values or a description. */
    static CharSequence text(Context context, SyncDiff.Change change, int mutedColor) {
        String title;
        String detail = null;
        switch (change.kind) {
            case CONDITION_ADDED:
                title = change.condition.getDuration() > 0
                        ? context.getString(R.string.approval_condition_added, name(context, change.condition),
                        change.condition.getDuration())
                        : context.getString(R.string.approval_condition_added_no_duration, name(context, change.condition));
                detail = conditionDetail(context, change.condition);
                break;
            case CONDITION_REMOVED:
                title = context.getString(R.string.approval_condition_removed,
                        change.from.isEmpty() ? context.getString(R.string.approval_condition_unnamed) : change.from);
                break;
            case CONDITION_CHANGED:
                title = change.condition.getDuration() > 0
                        ? context.getString(R.string.approval_condition_changed, name(context, change.condition),
                        change.condition.getDuration())
                        : context.getString(R.string.approval_condition_changed_no_duration, name(context, change.condition));
                detail = conditionDetail(context, change.condition);
                break;
            case REACTION_ADDED:
                title = context.getString(R.string.approval_reaction_added, reactionName(context, change.reaction.getName()));
                detail = change.reaction.getDescription();
                break;
            case REACTION_REMOVED:
                title = context.getString(R.string.approval_reaction_removed, reactionName(context, change.from));
                break;
            case REACTION_CHANGED:
                title = context.getString(R.string.approval_reaction_changed, reactionName(context, change.reaction.getName()),
                        context.getString(change.reaction.isAvailable() ? R.string.sync_value_available : R.string.sync_value_used));
                detail = change.reaction.getDescription();
                break;
            default: {
                String field = context.getString(label(change.field));
                String from = value(context, change.field, change.from);
                String to = value(context, change.field, change.to);
                if (from.length() <= SHORT_VALUE && to.length() <= SHORT_VALUE) {
                    title = context.getString(R.string.approval_field_change, field,
                            from.isEmpty() ? "–" : from, to.isEmpty() ? "–" : to);
                } else {
                    title = context.getString(R.string.approval_field_changed, field);
                    detail = change.to;
                }
                break;
            }
        }
        SpannableStringBuilder sb = new SpannableStringBuilder(title);
        if (detail != null && !detail.isEmpty()) {
            int start = sb.length();
            sb.append('\n').append(detail.length() > 120 ? detail.substring(0, 120) + "…" : detail);
            sb.setSpan(new ForegroundColorSpan(mutedColor), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new RelativeSizeSpan(0.85f), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    /** @return The condition's name with its value, e.g. "Frightened 2". */
    private static String name(Context context, DebuffModel d) {
        String name = d.getName().trim().isEmpty() ? context.getString(R.string.approval_condition_unnamed) : d.getName();
        return d.getValue() > 0 ? context.getString(R.string.approval_condition_value, name, d.getValue()) : name;
    }

    /** @return For persistent damage its damage and flat check DC, otherwise the description. */
    private static String conditionDetail(Context context, DebuffModel d) {
        if (!d.isPersistentDamage()) return d.getDescription();
        return context.getString(R.string.approval_persistent_detail, d.getDamageValue(), d.getDamageType(),
                d.getDifficultyClass());
    }

    private static String reactionName(Context context, String name) {
        return name == null || name.trim().isEmpty() ? context.getString(R.string.approval_reaction_unnamed) : name;
    }

    /** @return The value as the GM reads it: yes/no for dying, available/used for the reaction. */
    private static String value(Context context, SyncDiff.Field field, String raw) {
        if (!field.isSwitch()) return raw;
        boolean on = Boolean.parseBoolean(raw);
        if (field == SyncDiff.Field.REACTION) {
            return context.getString(on ? R.string.sync_value_available : R.string.sync_value_used);
        }
        return context.getString(on ? R.string.sync_value_yes : R.string.sync_value_no);
    }

    @StringRes
    static int label(SyncDiff.Field field) {
        switch (field) {
            case NAME: return R.string.sync_field_name;
            case INITIATIVE: return R.string.sync_field_initiative;
            case INITIATIVE_BONUS: return R.string.char_detail_initiative_bonus;
            case HP: return R.string.sync_field_hp;
            case MAX_HP: return R.string.char_detail_max_hp;
            case FAST_HEALING: return R.string.char_detail_fast_healing;
            case REGENERATION: return R.string.char_detail_regeneration;
            case HERO_POINTS: return R.string.char_detail_hero_points;
            case DYING: return R.string.sync_field_dying;
            case DYING_VALUE: return R.string.char_detail_dying;
            case MAX_DYING: return R.string.char_detail_max_dying;
            case WOUNDED: return R.string.char_detail_wounded;
            case DOOMED: return R.string.char_detail_doomed;
            case RECOVERY_DC: return R.string.char_detail_recovery_dc;
            case AC: return R.string.approval_field_ac;
            case SAVES: return R.string.approval_field_saves;
            case SKILLS: return R.string.char_detail_skills;
            case ATTACK_ROUTINE: return R.string.char_detail_attack_routine;
            case NOTES: return R.string.char_detail_notes;
            case REACTION: return R.string.sync_field_reaction;
            case REACTIONS: return R.string.sheet_reactions_header;
            default: return R.string.sheet_tab_conditions;
        }
    }
}
