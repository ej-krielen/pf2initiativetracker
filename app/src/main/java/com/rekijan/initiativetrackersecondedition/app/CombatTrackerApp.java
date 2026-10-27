package com.rekijan.initiativetrackersecondedition.app;

import android.app.Application;
import android.content.Context;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.data.EncounterStore;
import com.rekijan.initiativetrackersecondedition.core.data.KeyValueStore;
import com.rekijan.initiativetrackersecondedition.core.data.PrefsKeyValueStore;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterDefaults;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;
import com.rekijan.initiativetrackersecondedition.feature.party.Party;
import com.rekijan.initiativetrackersecondedition.feature.party.PartyBook;
import com.rekijan.initiativetrackersecondedition.feature.party.PartyStore;
import com.rekijan.initiativetrackersecondedition.feature.player.PlayerBook;
import com.rekijan.initiativetrackersecondedition.feature.profile.ProfileState;

import java.util.List;

/**
 * Holds the saved parties ({@link PartyBook}) and the fight of the active party for the whole
 * app, so it survives screen rotation. The active party is the {@link Encounter}: it is loaded on
 * first use (a party without saved characters starts with four PCs), saved when the app goes to
 * the background, and swapped in place when the user switches parties. A single-fight save is
 * turned into the first party (see PartyStore). The settings and the Remove Ads flag are in
 * {@link EncounterStore}.
 * <p>
 * Besides parties, the user can have player characters ({@link PlayerBook}), and
 * {@link ProfileState} remembers which of the two was opened last, so the app opens it again.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 16-5-2019
 */
public class CombatTrackerApp extends Application {

    private EncounterStore store;
    private PartyBook parties;
    private PlayerBook players;
    private ProfileState profile;
    private Encounter encounter;

    @Override
    public void onCreate() {
        super.onCreate();
        KeyValueStore values = new PrefsKeyValueStore(
                getSharedPreferences(EncounterStore.PREFS_NAME, Context.MODE_PRIVATE));
        store = new EncounterStore(values);
        String firstName = getString(R.string.party_first_name);
        parties = new PartyBook(new PartyStore(values, firstName), firstName);
        players = new PlayerBook(values);
        profile = new ProfileState(values);
    }

    public EncounterStore getStore() {
        return store;
    }

    public PartyBook getParties() {
        return parties;
    }

    public PlayerBook getPlayers() {
        return players;
    }

    // ---------------------------------------------------------------- Profiles

    /** @return True when a player character is the open profile; false for the party. */
    public boolean isPlayerOpen() {
        return profile.isPlayer() && players.contains(profile.getPlayerId());
    }

    /** @return The id of the open player character, or null when the party is open. */
    public String getOpenPlayerId() {
        return isPlayerOpen() ? profile.getPlayerId() : null;
    }

    /** Opens a party: it becomes the active party and the open profile. */
    public void openParty(String id) {
        switchParty(id);
        profile.openParty();
    }

    /** Opens a player character; the active party stays as it is, for when the user goes back. */
    public void openPlayer(String id) {
        if (players.contains(id)) profile.openPlayer(id);
    }

    /** Deletes a player character. Deleting the open one makes the party the open profile. */
    public void deletePlayer(String id) {
        boolean wasOpen = id.equals(getOpenPlayerId());
        players.delete(id);
        if (wasOpen) profile.openParty();
    }

    // ---------------------------------------------------------------- Parties

    public Encounter getEncounter() {
        if (encounter == null) {
            String id = parties.getActiveId();
            encounter = new Encounter(charactersOf(id), parties.loadRound(id));
        }
        return encounter;
    }

    public void saveEncounter() {
        if (encounter != null) {
            parties.save(parties.getActiveId(), encounter.getCharacters(), encounter.getRound());
        }
    }

    /** Saves the active party, then puts the other party's characters and round in the fight. */
    public void switchParty(String id) {
        if (parties.get(id) == null || id.equals(parties.getActiveId())) return;
        saveEncounter();
        parties.setActive(id);
        getEncounter().replace(charactersOf(id), parties.loadRound(id));
    }

    /** @return The new party (four PCs, round 1), or null when the limit or the name says no. */
    public Party createParty(String name) {
        return parties.create(name, CharacterDefaults.startingParty(this));
    }

    /** @return The copy, or null when the limit is reached. */
    public Party duplicateParty(String id) {
        if (id.equals(parties.getActiveId())) saveEncounter();
        return parties.duplicate(id);
    }

    /**
     * Deletes a party. When it was the active one, the fight switches to the party that became
     * active, without saving the deleted one first.
     *
     * @return True when deleted (the last party can't be).
     */
    public boolean deleteParty(String id) {
        boolean wasActive = id.equals(parties.getActiveId());
        String newActive = parties.delete(id);
        if (newActive == null) return false;
        if (wasActive) getEncounter().replace(charactersOf(newActive), parties.loadRound(newActive));
        return true;
    }

    /** @return How many characters the party has; the active one is counted live. */
    public int characterCount(String id) {
        if (id.equals(parties.getActiveId()) && encounter != null) return encounter.size();
        return parties.characterCount(id);
    }

    private List<CharacterModel> charactersOf(String id) {
        List<CharacterModel> characters = parties.loadCharacters(id);
        return characters != null ? characters : CharacterDefaults.startingParty(this);
    }
}
