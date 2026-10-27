package com.rekijan.initiativetrackersecondedition.feature.health;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.util.Dialogs;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel.RecoveryResult;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;

/**
 * The questions about hit points, dying and death: remove a dead monster, knock a PC out, dying
 * going up when damaged, recovery checks at the start of the turn and spending hero points to
 * stay alive. The rules themselves are in {@link CharacterModel} and {@link HpChange}.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 24-3-2021
 */
public final class HealthDialogs {

    /** Player Core, Hit Points, Healing, and Dying. */
    private static final String DYING_RULES_URL = "https://2e.aonprd.com/Rules.aspx?ID=2319";

    private HealthDialogs() {
    }

    /** Asks the follow-up question for a new hp value, see {@link HpChange#apply}. */
    public static void onHpChanged(Context context, Encounter encounter, CharacterModel character,
                                   HpChange.Outcome outcome) {
        encounter.notifyChanged();
        switch (outcome) {
            case OFFER_REMOVAL:
                Dialogs.builder(context)
                        .setTitle(R.string.dialog_delete_low_title)
                        .setMessage(R.string.dialog_delete_low)
                        .setPositiveButton(R.string.dialog_delete_low_positive, (d, w) -> encounter.remove(character))
                        .setNegativeButton(R.string.dialog_delete_low_negative,
                                (d, w) -> askKnockOut(context, encounter, character))
                        .show();
                break;
            case ASK_KNOCK_OUT:
                Dialogs.builder(context)
                        .setTitle(R.string.dialog_dying_pc_title)
                        .setMessage(R.string.dialog_dying_pc)
                        .setPositiveButton(R.string.dialog_dying_pc_positive,
                                (d, w) -> askKnockOut(context, encounter, character))
                        .setNegativeButton(R.string.dialog_pc_dying_negative, null)
                        .show();
                break;
            case RECOVERED_BY_HEALING:
                Toast.makeText(context, context.getString(R.string.toast_recovered_from_dying_by_healing,
                        character.getCharacterName()), Toast.LENGTH_LONG).show();
                break;
            case DAMAGED_WHILE_DYING:
                askDamageWhileDying(context, encounter, character);
                break;
            case NONE:
                break;
        }
    }

    /** The recovery check at the start of a dying character's turn. */
    public static void askRecoveryCheck(Context context, Encounter encounter, CharacterModel character) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_recovery, null);
        TextView dcTextView = view.findViewById(R.id.recovery_dialog_dc_label);
        dcTextView.setText(context.getString(R.string.dialog_recovery_dc, character.getRecoveryCheckDc()));

        AlertDialog dialog = Dialogs.builder(context)
                .setTitle(R.string.dialog_recovery_title)
                .setView(view)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();

        bindRecoveryButton(context, encounter, character, dialog,
                view.findViewById(R.id.recovery_dialog_crit_success_btn), RecoveryResult.CRITICAL_SUCCESS);
        bindRecoveryButton(context, encounter, character, dialog,
                view.findViewById(R.id.recovery_dialog_success_btn), RecoveryResult.SUCCESS);
        bindRecoveryButton(context, encounter, character, dialog,
                view.findViewById(R.id.recovery_dialog_failure_btn), RecoveryResult.FAILURE);
        bindRecoveryButton(context, encounter, character, dialog,
                view.findViewById(R.id.recovery_dialog_crit_failure_btn), RecoveryResult.CRITICAL_FAILURE);
        dialog.show();
    }

    /** Explains where to find the full death and dying rules, and opens them. */
    public static void showDyingRules(Context context) {
        Dialogs.builder(context)
                .setTitle(R.string.dialog_dying_rules_title)
                .setMessage(R.string.dialog_dying_rules)
                .setPositiveButton(R.string.dialog_about_ok, (d, w) -> Dialogs.openUrl(context, DYING_RULES_URL))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private static void bindRecoveryButton(Context context, Encounter encounter, CharacterModel character,
                                           AlertDialog dialog, View button, RecoveryResult result) {
        button.setOnClickListener(v -> {
            dialog.dismiss();
            boolean recovered = character.applyRecoveryCheck(result);
            encounter.notifyChanged();
            if (recovered) {
                Toast.makeText(context, context.getString(R.string.toast_recovered_from_dying_by_recovery_checks,
                        character.getCharacterName()), Toast.LENGTH_LONG).show();
            } else if (result == RecoveryResult.FAILURE || result == RecoveryResult.CRITICAL_FAILURE) {
                askHeroPoints(context, encounter, character);
            }
        });
    }

    /** Knocked out: dying 2 from a critical hit or critical failure, else dying 1. */
    private static void askKnockOut(Context context, Encounter encounter, CharacterModel character) {
        Dialogs.builder(context)
                .setTitle(R.string.dialog_dying_crit_title)
                .setMessage(R.string.dialog_dying_crit)
                .setPositiveButton(R.string.dialog_dying_crit_positive, (d, w) -> {
                    encounter.knockOut(character, 2);
                    askHeroPoints(context, encounter, character);
                })
                .setNegativeButton(R.string.dialog_dying_crit_negative, (d, w) -> {
                    encounter.knockOut(character, 1);
                    askHeroPoints(context, encounter, character);
                })
                .show();
    }

    /** Damage while dying: dying goes up by 2 on a critical, else by 1. */
    private static void askDamageWhileDying(Context context, Encounter encounter, CharacterModel character) {
        Dialogs.builder(context)
                .setTitle(R.string.dialog_dying_crit_title)
                .setMessage(R.string.dialog_dying_damaged_again)
                .setPositiveButton(R.string.dialog_dying_damaged_again_positive,
                        (d, w) -> increaseDying(context, encounter, character, 2))
                .setNegativeButton(R.string.dialog_dying_damaged_again_negative,
                        (d, w) -> increaseDying(context, encounter, character, 1))
                .show();
    }

    private static void increaseDying(Context context, Encounter encounter, CharacterModel character, int amount) {
        character.increaseDying(amount);
        encounter.notifyChanged();
        askHeroPoints(context, encounter, character);
    }

    /** Offers Heroic Recovery when the character has hero points, then checks for death. */
    private static void askHeroPoints(Context context, Encounter encounter, CharacterModel character) {
        if (character.getHeroPoints() <= 0) {
            askRemoveWhenDead(context, encounter, character);
            return;
        }
        Dialogs.builder(context)
                .setTitle(R.string.dialog_dying_hero_point_title)
                .setMessage(R.string.dialog_dying_hero_point)
                .setPositiveButton(R.string.dialog_dying_hero_point_positive, (d, w) -> {
                    character.useHeroPointsToStabilize();
                    encounter.notifyChanged();
                    Toast.makeText(context, context.getString(R.string.toast_recovered_from_dying_by_hero_points,
                            character.getCharacterName()), Toast.LENGTH_LONG).show();
                })
                .setNegativeButton(R.string.dialog_dying_hero_point_negative,
                        (d, w) -> askRemoveWhenDead(context, encounter, character))
                .show();
    }

    private static void askRemoveWhenDead(Context context, Encounter encounter, CharacterModel character) {
        if (!character.isAlreadyDead()) return;
        Dialogs.builder(context)
                .setTitle(R.string.dialog_delete_low_title)
                .setMessage(R.string.dialog_delete_low)
                .setPositiveButton(R.string.dialog_delete_low_positive, (d, w) -> encounter.remove(character))
                .setNegativeButton(R.string.dialog_delete_low_negative, null)
                .show();
    }
}
