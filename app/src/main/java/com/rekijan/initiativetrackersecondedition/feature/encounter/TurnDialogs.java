package com.rekijan.initiativetrackersecondedition.feature.encounter;

import android.content.Context;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.util.DialogHost;
import com.rekijan.initiativetrackersecondedition.core.util.DialogQueue;
import com.rekijan.initiativetrackersecondedition.core.util.Dialogs;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.condition.PersistentDamageDialogs;
import com.rekijan.initiativetrackersecondedition.feature.health.HealthDialogs;
import com.rekijan.initiativetrackersecondedition.feature.health.HpChange;
import com.rekijan.initiativetrackersecondedition.feature.spell.SpellDialogs;

import java.util.ArrayList;
import java.util.List;

/**
 * The questions the app asks when the turn moves on: persistent damage and expired (de)buffs at
 * the end of a turn; spells that ran out, fast healing, regeneration and recovery checks at the
 * start of the next.
 * They are asked one at a time, in the order of {@link Encounter.Turn#steps}. And after sorting:
 * reset the round counter?
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public final class TurnDialogs {

    private TurnDialogs() {
    }

    /** Asks the questions of the turn that ended, then those of the turn that starts. */
    public static void onTurnChanged(Context context, Encounter encounter, Encounter.Turn turn) {
        List<DialogQueue.Step> dialogs = new ArrayList<>();
        for (TurnStep step : turn.steps()) {
            dialogs.add(done -> show(context, encounter, step, done));
        }
        DialogQueue.run(dialogs);
    }

    /**
     * Asks to reset the round counter to 1 after sorting, with an extra warning when two
     * characters on the same side have the same initiative.
     */
    public static void askRoundReset(Context context, Encounter encounter, boolean duplicateInitiative) {
        String message = context.getString(R.string.dialog_reset_round_counter);
        if (duplicateInitiative) message += context.getString(R.string.dialog_reset_round_double_init);
        String text = message;
        DialogHost.show(c -> Dialogs.builder(c)
                .setTitle(R.string.dialog_reset_round_counter_title)
                .setMessage(text)
                .setPositiveButton(R.string.dialog_ok, (d, w) -> encounter.resetRound())
                .setNegativeButton(R.string.dialog_cancel, null)
                .create());
    }

    private static void show(Context context, Encounter encounter, TurnStep step, Runnable done) {
        CharacterModel character = step.character;
        switch (step.kind) {
            case PERSISTENT_DAMAGE:
                PersistentDamageDialogs.askEndOfTurn(context, encounter, character, step.debuff, done);
                break;
            case EXPIRED_DEBUFF:
                askRemoveExpiredDebuff(context, encounter, character, step.debuff, done);
                break;
            case EXPIRED_SPELL:
                SpellDialogs.askExpired(context, encounter, step.spell, done);
                break;
            case FAST_HEALING:
                askAutomaticHealing(context, encounter, character, character.getFastHealing(),
                        R.string.auto_healing_dialog_fast_healing_title,
                        R.string.auto_healing_dialog_fast_healing_message, done);
                break;
            case REGENERATION:
                askAutomaticHealing(context, encounter, character, character.getRegeneration(),
                        R.string.auto_healing_dialog_regeneration_title,
                        R.string.auto_healing_dialog_regeneration_message, done);
                break;
            case RECOVERY_CHECK:
                HealthDialogs.askRecoveryCheck(context, encounter, character, done);
                break;
        }
    }

    private static void askRemoveExpiredDebuff(Context context, Encounter encounter, CharacterModel character,
                                               DebuffModel debuff, Runnable done) {
        // Removed in the meantime, e.g. persistent damage ended by its flat check one step earlier
        if (!character.getDebuffList().contains(debuff)) {
            done.run();
            return;
        }
        DialogHost.show(c -> Dialogs.builder(c)
                .setTitle(R.string.dialog_debuff_expired_title)
                .setMessage(c.getString(R.string.debuff_expired, debuff.getDisplayName()))
                .setPositiveButton(R.string.dialog_debuff_remove, (d, w) -> {
                    character.getDebuffList().remove(debuff);
                    encounter.notifyChanged();
                })
                .setNegativeButton(R.string.dialog_debuff_keep, null)
                .setOnDismissListener(d -> done.run())
                .create());
    }

    private static void askAutomaticHealing(Context context, Encounter encounter, CharacterModel character,
                                            int healing, int titleRes, int messageRes, Runnable done) {
        DialogHost.show(c -> Dialogs.builder(c)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setPositiveButton(R.string.dialog_yes, (d, w) -> {
                    character.heal(healing);
                    boolean aboveMax = character.isAboveMaxHp();
                    HealthDialogs.onHpChanged(c, encounter, character,
                            HpChange.apply(character, character.getHp()),
                            aboveMax ? () -> askCapAtMaxHp(c, encounter, character, done) : done);
                })
                .setNegativeButton(R.string.dialog_no, (d, w) -> done.run())
                .setOnCancelListener(d -> done.run())
                .create());
    }

    private static void askCapAtMaxHp(Context context, Encounter encounter, CharacterModel character,
                                      Runnable done) {
        DialogHost.show(c -> Dialogs.builder(c)
                .setTitle(R.string.auto_healing_dialog_max_exceeded_title)
                .setMessage(R.string.auto_healing_dialog_max_exceeded_message)
                .setPositiveButton(R.string.dialog_yes, (d, w) -> {
                    character.capHpAtMaxHp();
                    encounter.notifyChanged();
                })
                .setNegativeButton(R.string.dialog_no, null)
                .setOnDismissListener(d -> done.run())
                .create());
    }
}
