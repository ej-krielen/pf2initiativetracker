package com.rekijan.initiativetrackersecondedition.core.data;

/**
 * The app-wide save file and the display settings. Uses the same SharedPreferences file as 1.x
 * ({@link #PREFS_NAME}) and the 1.x setting keys, so users keep their settings after updating, and
 * the 1.x fight is still there for
 * {@link com.rekijan.initiativetrackersecondedition.feature.party.PartyStore} to turn into the
 * first party. The parties themselves are saved by PartyStore; this class keeps the settings and
 * the Remove Ads flag.
 * <p>
 * The settings (hide condition descriptions, hide reactions) are for the whole app, not per
 * party: they are how the user likes to see the screen, not part of a fight.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public class EncounterStore {

    /** The 1.x SharedPreferences file, keep it as it is. */
    public static final String PREFS_NAME = "SHARED_PREF_TAG";
    // 1.x keys, keep them as they are
    static final String KEY_HIDE_DESCRIPTIONS = "SHARED_PREF_HIDE_DESC";
    static final String KEY_HIDE_REACTIONS = "SHARED_PREF_HIDE_REACTIONS";
    // New in 2.0, same key as Initiative Tracker, Mook Roller and Secret Roller
    static final String KEY_ADS_REMOVED = "ads_removed";

    private final KeyValueStore values;

    public EncounterStore(KeyValueStore values) {
        this.values = values;
    }

    /** @return True when the condition wizard hides the condition descriptions. */
    public boolean isHideDescriptions() {
        return values.getBoolean(KEY_HIDE_DESCRIPTIONS, false);
    }

    public void setHideDescriptions(boolean hide) {
        values.edit().putBoolean(KEY_HIDE_DESCRIPTIONS, hide).apply();
    }

    /** @return True when the character cards hide the reactions. */
    public boolean isHideReactions() {
        return values.getBoolean(KEY_HIDE_REACTIONS, false);
    }

    public void setHideReactions(boolean hide) {
        values.edit().putBoolean(KEY_HIDE_REACTIONS, hide).apply();
    }

    /** @return True once Remove Ads was bought (set by BillingManager, restored on every start). */
    public boolean isAdsRemoved() {
        return values.getBoolean(KEY_ADS_REMOVED, false);
    }

    public void setAdsRemoved(boolean removed) {
        values.edit().putBoolean(KEY_ADS_REMOVED, removed).apply();
    }
}
