package com.rekijan.initiativetrackersecondedition.app;

import android.app.Application;
import android.content.Context;

import com.rekijan.initiativetrackersecondedition.core.data.EncounterStore;
import com.rekijan.initiativetrackersecondedition.core.data.PrefsKeyValueStore;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterDefaults;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;

import java.util.List;

/**
 * Holds the current fight for the whole app, so it survives screen rotation. It is loaded from
 * {@link EncounterStore} on first use (a fresh install starts with three PCs and a monster) and
 * saved when the app goes to the background.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 16-5-2019
 */
public class CombatTrackerApp extends Application {

    private EncounterStore store;
    private Encounter encounter;

    @Override
    public void onCreate() {
        super.onCreate();
        store = new EncounterStore(new PrefsKeyValueStore(
                getSharedPreferences(EncounterStore.PREFS_NAME, Context.MODE_PRIVATE)));
    }

    public EncounterStore getStore() {
        return store;
    }

    public Encounter getEncounter() {
        if (encounter == null) {
            List<CharacterModel> characters = store.loadCharacters();
            if (characters == null) characters = CharacterDefaults.startingParty(this);
            encounter = new Encounter(characters, store.loadRound());
        }
        return encounter;
    }

    public void saveEncounter() {
        if (encounter != null) store.save(encounter);
    }
}
