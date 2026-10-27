package com.rekijan.initiativetrackersecondedition.feature.party;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetrackersecondedition.core.data.MemoryKeyValueStore;
import com.rekijan.initiativetrackersecondedition.core.data.OneXSave;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.character.ReactionModel;

import org.junit.Test;

import java.util.List;

/**
 * Tests {@link PartyStore}, mainly the migration: a single-fight save must become the first
 * party with all its characters and its round counter, and nothing of that save may be lost.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public class PartyStoreTest {

    /** A short single-fight save under GSON_TAG, with the unused id fields. */
    private static final String SAVE_FROM_1_X = "["
            + "{\"id\":0,\"party_id\":0,\"initiative\":18,\"initiativeBonus\":4,\"hp\":21,\"maxHp\":27,"
            + "\"isFirstInRound\":false,\"isPC\":true,\"characterName\":\"Merisiel\","
            + "\"debuffList\":[{\"name\":\"Haste\",\"duration\":3,\"description\":\"+1 attack\"}]},"
            + "{\"id\":0,\"party_id\":0,\"initiative\":9,\"initiativeBonus\":1,\"hp\":-2,\"maxHp\":15,"
            + "\"isFirstInRound\":true,\"isPC\":false,\"characterName\":\"Goblin\"}"
            + "]";

    private static MemoryKeyValueStore saveFrom1x() {
        MemoryKeyValueStore values = new MemoryKeyValueStore();
        values.edit()
                .putString(PartyStore.KEY_1X_CHARACTERS, SAVE_FROM_1_X)
                .putInt(PartyStore.KEY_1X_ROUND, 7)
                .apply();
        return values;
    }

    @Test
    public void saveFrom1xBecomesTheFirstParty() {
        MemoryKeyValueStore values = saveFrom1x();
        PartyBook book = new PartyBook(new PartyStore(values, "My party"), "unused");

        assertEquals(1, book.getParties().size());
        Party first = book.getActive();
        assertEquals("My party", first.getName());
        assertEquals(7, book.loadRound(first.getId()));

        List<CharacterModel> characters = book.loadCharacters(first.getId());
        assertEquals(2, characters.size());
        assertEquals("Merisiel", characters.get(0).getCharacterName());
        assertEquals(21, characters.get(0).getHp());
        assertEquals("Haste", characters.get(0).getDebuffList().get(0).getName());
        assertEquals("Goblin", characters.get(1).getCharacterName());
        assertTrue(characters.get(1).isFirstInRound());
    }

    @Test
    public void oldKeysAreKeptAsSafetyNet() {
        MemoryKeyValueStore values = saveFrom1x();
        new PartyStore(values, "My party");

        assertEquals(SAVE_FROM_1_X, values.getString(PartyStore.KEY_1X_CHARACTERS, null));
        assertEquals(7, values.getInt(PartyStore.KEY_1X_ROUND, 0));
    }

    @Test
    public void migrationRunsOnce() {
        MemoryKeyValueStore values = saveFrom1x();
        PartyStore store = new PartyStore(values, "My party");
        String id = store.loadActiveId();
        store.saveFight(id, "[]", 2);

        // Next start: the party is not overwritten by the single-fight save again
        PartyStore again = new PartyStore(values, "My party");
        assertEquals(1, again.loadParties().size());
        assertEquals(id, again.loadActiveId());
        assertEquals("[]", again.loadCharactersJson(id));
        assertEquals(2, again.loadRound(id));
    }

    @Test
    public void unreadable1xSaveIsStillCopied() {
        MemoryKeyValueStore values = new MemoryKeyValueStore();
        values.edit().putString(PartyStore.KEY_1X_CHARACTERS, "{not json").apply();
        PartyStore store = new PartyStore(values, "My party");

        assertEquals("{not json", store.loadCharactersJson(store.loadActiveId()));
    }

    @Test
    public void freshInstallGetsOneEmptyParty() {
        MemoryKeyValueStore values = new MemoryKeyValueStore();
        PartyBook book = new PartyBook(new PartyStore(values, "My party"), "unused");

        assertEquals(1, book.getParties().size());
        assertNull(book.loadCharacters(book.getActiveId()));
        assertEquals(PartyStore.FIRST_ROUND, book.loadRound(book.getActiveId()));
    }

    /**
     * The real single-fight save becomes party 1 with every PF2e field intact, also after the
     * party is saved again and loaded on the next start. The single-fight keys stay as they were.
     */
    @Test
    public void realPf2SaveFrom1xBecomesPartyOneIntact() throws Exception {
        MemoryKeyValueStore values = OneXSave.load();
        String oldJson = values.getString(PartyStore.KEY_1X_CHARACTERS, null);
        PartyBook book = new PartyBook(new PartyStore(values, "My party"), "unused");
        String id = book.getActiveId();

        assertEquals(1, book.getUsed());
        assertEquals(2, book.loadRound(id));
        assertPf2FieldsIntact(book.loadCharacters(id));

        // Save the party (as on going to the background) and start again
        book.save(id, book.loadCharacters(id), book.loadRound(id));
        PartyBook again = new PartyBook(new PartyStore(values, "My party"), "unused");
        assertEquals(id, again.getActiveId());
        assertEquals(2, again.loadRound(id));
        assertPf2FieldsIntact(again.loadCharacters(id));

        assertEquals(oldJson, values.getString(PartyStore.KEY_1X_CHARACTERS, null));
        assertEquals(2, values.getInt(PartyStore.KEY_1X_ROUND, 0));
    }

    private static void assertPf2FieldsIntact(List<CharacterModel> characters) {
        assertEquals(4, characters.size());

        CharacterModel valeros = characters.get(0);
        assertEquals("Valeros", valeros.getCharacterName());
        assertTrue(valeros.isFirstInRound());
        assertEquals(1, valeros.getHeroPoints());
        assertEquals(4, valeros.getMaxDyingValue());
        assertEquals(10, valeros.getRecoveryDC());

        CharacterModel seoni = characters.get(1);
        DebuffModel frightened = seoni.getDebuffList().get(0);
        assertEquals("Frightened", frightened.getName());
        assertEquals(2, frightened.getValue());
        DebuffModel fire = seoni.getDebuffList().get(1);
        assertTrue(fire.isPersistentDamage());
        assertEquals("fire", fire.getDamageType());
        assertEquals("1d6", fire.getDamageValue());
        assertEquals(15, fire.getDifficultyClass());

        CharacterModel kyra = characters.get(2);
        assertTrue(kyra.isDying());
        assertEquals(2, kyra.getDyingValue());
        assertEquals(1, kyra.getWoundedValue());
        assertEquals(0, kyra.getDoomedValue());
        ReactionModel shieldBlock = kyra.getReactionList().get(0);
        assertEquals("Shield Block", shieldBlock.getName());
        assertEquals("Reduce damage by hardness", shieldBlock.getDescription());
        assertFalse(shieldBlock.isAvailable());

        assertFalse(characters.get(3).isPC());
    }

    @Test
    public void packLevelIsSavedAndClamped() {
        MemoryKeyValueStore values = new MemoryKeyValueStore();
        PartyStore store = new PartyStore(values, "My party");
        store.savePackLevel(2);
        assertEquals(2, new PartyStore(values, "My party").loadPackLevel());
        store.savePackLevel(12);
        assertEquals(PartySlots.MAX_PACK_LEVEL, store.loadPackLevel());
    }
}
