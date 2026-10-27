package com.rekijan.initiativetrackersecondedition.feature.health;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.health.HpChange.Outcome;

import org.junit.Test;

/**
 * Tests {@link HpChange}: what a new hp value means for PCs, NPCs and dying characters.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public class HpChangeTest {

    private static CharacterModel character(boolean pc, int hp) {
        CharacterModel c = new CharacterModel();
        c.setPC(pc);
        c.setMaxHp(20);
        c.setHp(hp);
        return c;
    }

    @Test
    public void normalHpChangeNeedsNothing() {
        CharacterModel c = character(true, 10);
        assertEquals(Outcome.NONE, HpChange.apply(c, 4));
        assertEquals(4, c.getHp());
    }

    @Test
    public void npcAtZeroIsOfferedForRemoval() {
        CharacterModel c = character(false, 5);
        assertEquals(Outcome.OFFER_REMOVAL, HpChange.apply(c, -3));
        assertEquals(-3, c.getHp());
    }

    @Test
    public void pcAtZeroStopsAtZeroAndCanBeKnockedOut() {
        CharacterModel c = character(true, 5);
        assertEquals(Outcome.ASK_KNOCK_OUT, HpChange.apply(c, -8));
        assertEquals(0, c.getHp());
    }

    @Test
    public void healingADyingCharacterEndsDyingWithWounded() {
        CharacterModel c = character(true, 0);
        c.knockOut(1);

        assertEquals(Outcome.RECOVERED_BY_HEALING, HpChange.apply(c, 6));
        assertFalse(c.isDying());
        assertEquals(1, c.getWoundedValue());
        assertEquals(6, c.getHp());
    }

    @Test
    public void damageWhileDyingKeepsHpAtZero() {
        CharacterModel c = character(true, 0);
        c.knockOut(1);

        assertEquals(Outcome.DAMAGED_WHILE_DYING, HpChange.apply(c, -5));
        assertEquals(0, c.getHp());
    }
}
