package com.rekijan.initiativetrackersecondedition.feature.condition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;

import org.junit.Test;

/**
 * Tests {@link Conditions}: valued conditions, pre-remaster names and splitting the value off a
 * 1.x condition name.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public class ConditionsTest {

    @Test
    public void legacyNamesMapToTheRemasterName() {
        assertEquals(Conditions.OFF_GUARD, Conditions.key("Flat Footed"));
        assertEquals(Conditions.OFF_GUARD, Conditions.key("flat-footed"));
        assertEquals(Conditions.OFF_GUARD, Conditions.key("  Off-Guard "));
        assertEquals(Conditions.FRIGHTENED, Conditions.key("FRIGHTENED"));
    }

    @Test
    public void knowsTheValuedConditions() {
        assertTrue(Conditions.isValued("Frightened"));
        assertTrue(Conditions.isValued("stupefied"));
        assertFalse(Conditions.isValued("Off-Guard"));
        assertFalse(Conditions.isValued("Quickened"));
    }

    @Test
    public void splitsTheValueOffA1xName() {
        DebuffModel d = new DebuffModel("Frightened 2", "", 0, 0);
        Conditions.upgradeLegacyValue(d);

        assertEquals("Frightened", d.getName());
        assertEquals(2, d.getValue());
    }

    @Test
    public void leavesOtherNamesAlone() {
        DebuffModel custom = new DebuffModel("Bless 1", "", 0, 0);
        DebuffModel offGuard = new DebuffModel("Flat Footed", "", 1, 0);
        DebuffModel alreadyValued = new DebuffModel("Clumsy 1", "", 0, 3);

        Conditions.upgradeLegacyValue(custom);
        Conditions.upgradeLegacyValue(offGuard);
        Conditions.upgradeLegacyValue(alreadyValued);

        assertEquals("Bless 1", custom.getName());
        assertEquals("Flat Footed", offGuard.getName());
        assertEquals("Clumsy 1", alreadyValued.getName());
        assertEquals(3, alreadyValued.getValue());
    }

    @Test
    public void persistentDamageIsNeverACondition() {
        DebuffModel fire = DebuffModel.persistentDamage("Frightened 2", "fire", "2", 15, 0, "");
        Conditions.upgradeLegacyValue(fire);

        assertEquals("Frightened 2", fire.getName());
        assertFalse(Conditions.is(fire, Conditions.FRIGHTENED));
    }
}
