package com.rekijan.initiativetrackersecondedition.core.data;

import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;

import java.util.List;

/**
 * Saves and loads the current fight and the display settings. Uses the same SharedPreferences
 * file ({@link #PREFS_NAME}) and keys as 1.x, so users keep their characters, round counter and
 * settings after updating.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public class EncounterStore {

    /** The 1.x SharedPreferences file, keep it as it is. */
    public static final String PREFS_NAME = "SHARED_PREF_TAG";
    // 1.x keys, keep them as they are
    static final String KEY_CHARACTERS = "GSON_TAG";
    static final String KEY_ROUND = "ROUND_COUNTER";
    static final String KEY_HIDE_DESCRIPTIONS = "SHARED_PREF_HIDE_DESC";
    static final String KEY_HIDE_REACTIONS = "SHARED_PREF_HIDE_REACTIONS";

    private final KeyValueStore values;

    public EncounterStore(KeyValueStore values) {
        this.values = values;
    }

    /** @return The saved characters, or null when nothing was saved yet. */
    public List<CharacterModel> loadCharacters() {
        return EncounterJson.fromJson(values.getString(KEY_CHARACTERS, null));
    }

    /** @return The saved round; 1.x started a fresh install at round 1. */
    public int loadRound() {
        return values.getInt(KEY_ROUND, 1);
    }

    public void save(Encounter encounter) {
        values.edit()
                .putString(KEY_CHARACTERS, EncounterJson.toJson(encounter.getCharacters()))
                .putInt(KEY_ROUND, encounter.getRound())
                .apply();
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
}
