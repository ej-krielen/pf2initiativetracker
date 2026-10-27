package com.rekijan.initiativetrackersecondedition.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The Remove Ads flag: off for a fresh install or a single-fight save, kept once set, and stored
 * next to the saved keys without touching them.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public class EncounterStoreTest {

    @Test
    public void adsRemovedIsOffByDefault() {
        assertFalse(new EncounterStore(new MemoryKeyValueStore()).isAdsRemoved());
    }

    @Test
    public void adsRemovedSurvivesANewStore() {
        MemoryKeyValueStore values = new MemoryKeyValueStore();
        new EncounterStore(values).setAdsRemoved(true);
        assertTrue(new EncounterStore(values).isAdsRemoved());
    }

    @Test
    public void adsRemovedLeavesTheOneXKeysAlone() {
        MemoryKeyValueStore values = new MemoryKeyValueStore();
        values.edit().putInt("ROUND_COUNTER", 7).putBoolean(EncounterStore.KEY_HIDE_REACTIONS, true).apply();
        EncounterStore store = new EncounterStore(values);
        store.setAdsRemoved(true);
        assertEquals(7, values.getInt("ROUND_COUNTER", 0));
        assertTrue(store.isHideReactions());
        assertEquals("ads_removed", EncounterStore.KEY_ADS_REMOVED);
    }
}
