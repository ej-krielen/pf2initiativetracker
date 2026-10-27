package com.rekijan.initiativetrackersecondedition.core.data;

/**
 * The app-wide save file ({@link #PREFS_NAME}) and the display settings. The parties are saved in
 * the same file by {@link com.rekijan.initiativetrackersecondedition.feature.party.PartyStore};
 * this class keeps the settings and the Remove Ads flag. The file name and keys must stay the
 * same, or users lose their saves and settings.
 * <p>
 * The settings (hide condition descriptions, hide reactions) are for the whole app, not per
 * party: they are how the user likes to see the screen, not part of a fight.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public class EncounterStore {

    /** The SharedPreferences file with all saved data, keep the name. */
    public static final String PREFS_NAME = "SHARED_PREF_TAG";
    // Saved keys, keep them as they are
    static final String KEY_HIDE_DESCRIPTIONS = "SHARED_PREF_HIDE_DESC";
    static final String KEY_HIDE_REACTIONS = "SHARED_PREF_HIDE_REACTIONS";
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
