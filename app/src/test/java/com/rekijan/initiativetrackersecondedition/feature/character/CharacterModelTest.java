package com.rekijan.initiativetrackersecondedition.feature.character;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel.RecoveryResult;

import org.junit.Test;

import java.util.List;

/**
 * Tests the turn and dying rules of {@link CharacterModel}: frightened and durations at the end
 * of the turn, reactions at the start, knocked out, recovery checks, wounded, doomed and hero
 * points.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public class CharacterModelTest {

    @Test
    public void newCharacterHasThePathfinderDefaults() {
        CharacterModel c = new CharacterModel();

        assertEquals(4, c.getMaxDyingValue());
        assertEquals(10, c.getRecoveryDC());
        assertEquals(1, c.getHeroPoints());
        assertTrue(c.isReactionAvailable());
    }

    @Test
    public void frightenedGoesDownAtTheEndOfTheTurnAndEndsAtZero() {
        CharacterModel c = new CharacterModel();
        DebuffModel frightened = new DebuffModel("Frightened", "", 0, 2);
        c.getDebuffList().add(frightened);

        c.endTurn();
        assertEquals(1, frightened.getValue());
        assertEquals(1, c.getDebuffList().size());

        c.endTurn();
        assertTrue(c.getDebuffList().isEmpty());
    }

    @Test
    public void otherValuedConditionsDoNotGoDown() {
        CharacterModel c = new CharacterModel();
        DebuffModel sickened = new DebuffModel("Sickened", "", 0, 2);
        DebuffModel frightenedWithoutValue = new DebuffModel("Frightened", "", 0, 0);
        c.getDebuffList().add(sickened);
        c.getDebuffList().add(frightenedWithoutValue);

        c.endTurn();

        assertEquals(2, sickened.getValue());
        assertEquals(2, c.getDebuffList().size());
    }

    @Test
    public void durationsTickAndReportWhatRanOut() {
        CharacterModel c = new CharacterModel();
        DebuffModel offGuard = new DebuffModel("Off-Guard", "", 1, 0);
        DebuffModel haste = new DebuffModel("Haste", "", 3, 0);
        DebuffModel noTimer = new DebuffModel("Cursed", "", 0, 0);
        c.getDebuffList().add(offGuard);
        c.getDebuffList().add(haste);
        c.getDebuffList().add(noTimer);

        List<DebuffModel> expired = c.endTurn();

        assertEquals(1, expired.size());
        assertEquals(offGuard, expired.get(0));
        assertEquals(0, offGuard.getDuration());
        assertEquals(2, haste.getDuration());
        assertEquals(0, noTimer.getDuration());
        // Expired ones stay until the user removes them
        assertEquals(3, c.getDebuffList().size());
    }

    @Test
    public void startOfTurnMakesAllReactionsAvailable() {
        CharacterModel c = new CharacterModel();
        c.setReactionAvailable(false);
        ReactionModel shieldBlock = new ReactionModel();
        shieldBlock.setAvailable(false);
        c.getReactionList().add(shieldBlock);

        c.startTurn();

        assertTrue(c.isReactionAvailable());
        assertTrue(shieldBlock.isAvailable());
        assertEquals(1, c.getAvailableReactionCount());
    }

    @Test
    public void knockOutAddsWoundedToDying() {
        CharacterModel c = new CharacterModel();
        c.setWoundedValue(1);

        c.knockOut(2);

        assertTrue(c.isDying());
        assertEquals(3, c.getDyingValue());
    }

    @Test
    public void recoveryCheckDcIsStartingDcPlusDying() {
        CharacterModel c = new CharacterModel();
        c.setWoundedValue(2);
        c.knockOut(1);

        assertEquals(13, c.getRecoveryCheckDc());
    }

    @Test
    public void recoveryChecksMoveDyingAndRecoveringGivesWounded() {
        CharacterModel c = new CharacterModel();
        c.setHp(0);
        c.knockOut(2);

        assertFalse(c.applyRecoveryCheck(RecoveryResult.FAILURE));
        assertEquals(3, c.getDyingValue());
        assertFalse(c.applyRecoveryCheck(RecoveryResult.SUCCESS));
        assertEquals(2, c.getDyingValue());
        assertTrue(c.applyRecoveryCheck(RecoveryResult.CRITICAL_SUCCESS));

        assertFalse(c.isDying());
        assertEquals(0, c.getDyingValue());
        assertEquals(1, c.getWoundedValue());
        assertEquals(0, c.getHp());
    }

    @Test
    public void doomedLowersTheDeathThreshold() {
        CharacterModel c = new CharacterModel();
        c.setDoomedValue(1);
        c.knockOut(2);
        assertFalse(c.isAlreadyDead());

        c.applyRecoveryCheck(RecoveryResult.FAILURE);

        assertEquals(3, c.getDeathThreshold());
        assertTrue(c.isAlreadyDead());
    }

    @Test
    public void heroPointsStabilizeWithoutWounded() {
        CharacterModel c = new CharacterModel();
        c.setHeroPoints(2);
        c.knockOut(2);

        c.useHeroPointsToStabilize();

        assertFalse(c.isDying());
        assertEquals(0, c.getDyingValue());
        assertEquals(0, c.getWoundedValue());
        assertEquals(0, c.getHeroPoints());
        assertEquals(0, c.getHp());
    }

    @Test
    public void healingCanBeCappedAtMaxHp() {
        CharacterModel c = new CharacterModel();
        c.setMaxHp(20);
        c.setHp(18);

        c.heal(5);
        assertTrue(c.isAboveMaxHp());
        c.capHpAtMaxHp();

        assertEquals(20, c.getHp());
    }

    @Test
    public void maxDyingStaysInRange() {
        CharacterModel c = new CharacterModel();
        c.setMaxDyingValue(10);
        assertEquals(CharacterModel.MAX_DYING_VALUE, c.getMaxDyingValue());
        c.setMaxDyingValue(0);
        assertEquals(1, c.getMaxDyingValue());
    }
}
