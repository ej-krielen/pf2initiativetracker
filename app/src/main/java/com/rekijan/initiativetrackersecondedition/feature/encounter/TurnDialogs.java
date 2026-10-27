package com.rekijan.initiativetrackersecondedition.feature.encounter;

import android.content.Context;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.util.Dialogs;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.condition.PersistentDamageDialogs;
import com.rekijan.initiativetrackersecondedition.feature.health.HealthDialogs;
import com.rekijan.initiativetrackersecondedition.feature.health.HpChange;

/**
 * The questions the app asks when the turn moves on: persistent damage and expired (de)buffs at
 * the end of a turn; fast healing, regeneration and recovery checks at the start of the next. And
 * after sorting: reset the round counter?
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public final class TurnDialogs {

    private TurnDialogs() {
    }

    /** Asks the questions that belong to the end of one turn and the start of the next. */
    public static void onTurnChanged(Context context, Encounter encounter, Encounter.Turn turn) {
        for (DebuffModel debuff : turn.persistentDamage) {
            PersistentDamageDialogs.askEndOfTurn(context, encounter, turn.ended, debuff);
        }
        for (DebuffModel debuff : turn.expiredDebuffs) {
            askRemoveExpiredDebuff(context, encounter, turn.ended, debuff);
        }

        CharacterModel active = turn.active;
        if (active.getFastHealing() > 0) {
            askAutomaticHealing(context, encounter, active, active.getFastHealing(),
                    R.string.auto_healing_dialog_fast_healing_title, R.string.auto_healing_dialog_fast_healing_message);
        }
        if (active.getRegeneration() > 0) {
            askAutomaticHealing(context, encounter, active, active.getRegeneration(),
                    R.string.auto_healing_dialog_regeneration_title, R.string.auto_healing_dialog_regeneration_message);
        }
        if (turn.recoveryCheck) {
            HealthDialogs.askRecoveryCheck(context, encounter, active);
        }
    }

    /**
     * Asks to reset the round counter to 1 after sorting, with an extra warning when two
     * characters on the same side have the same initiative.
     */
    public static void askRoundReset(Context context, Encounter encounter, boolean duplicateInitiative) {
        String message = context.getString(R.string.dialog_reset_round_counter);
        if (duplicateInitiative) message += context.getString(R.string.dialog_reset_round_double_init);
        Dialogs.builder(context)
                .setTitle(R.string.dialog_reset_round_counter_title)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_ok, (d, w) -> encounter.resetRound())
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private static void askRemoveExpiredDebuff(Context context, Encounter encounter,
                                               CharacterModel character, DebuffModel debuff) {
        Dialogs.builder(context)
                .setTitle(R.string.dialog_debuff_expired_title)
                .setMessage(context.getString(R.string.debuff_expired, debuff.getDisplayName()))
                .setPositiveButton(R.string.dialog_debuff_remove, (d, w) -> {
                    character.getDebuffList().remove(debuff);
                    encounter.notifyChanged();
                })
                .setNegativeButton(R.string.dialog_debuff_keep, null)
                .show();
    }

    private static void askAutomaticHealing(Context context, Encounter encounter, CharacterModel character,
                                            int healing, int titleRes, int messageRes) {
        Dialogs.builder(context)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setPositiveButton(R.string.dialog_yes, (d, w) -> {
                    character.heal(healing);
                    if (character.isAboveMaxHp()) askCapAtMaxHp(context, encounter, character);
                    HealthDialogs.onHpChanged(context, encounter, character,
                            HpChange.apply(character, character.getHp()));
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    private static void askCapAtMaxHp(Context context, Encounter encounter, CharacterModel character) {
        Dialogs.builder(context)
                .setTitle(R.string.auto_healing_dialog_max_exceeded_title)
                .setMessage(R.string.auto_healing_dialog_max_exceeded_message)
                .setPositiveButton(R.string.dialog_yes, (d, w) -> {
                    character.capHpAtMaxHp();
                    encounter.notifyChanged();
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }
}
