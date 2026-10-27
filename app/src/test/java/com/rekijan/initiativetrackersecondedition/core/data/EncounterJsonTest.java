package com.rekijan.initiativetrackersecondedition.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.character.ReactionModel;

import org.junit.Test;

import java.util.List;


/**
 * Tests {@link EncounterJson} against a real single-fight save (see {@link OneXSave}). It must
 * load with its characters, conditions, reactions, dying track and round intact.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public class EncounterJsonTest {

    @Test
    public void loadsRealSaveFrom1x() throws Exception {
        MemoryKeyValueStore values = OneXSave.load();
        List<CharacterModel> characters = EncounterJson.fromJson(values.getString("GSON_TAG", null));

        assertEquals(2, values.getInt("ROUND_COUNTER", 0));
        assertEquals(4, characters.size());

        CharacterModel valeros = characters.get(0);
        assertEquals("Valeros", valeros.getCharacterName());
        assertTrue(valeros.isPC());
        assertTrue(valeros.isFirstInRound());
        assertEquals(6, valeros.getHp());
        assertEquals("10", valeros.getAc());
        assertEquals("Fort 10 Ref 10 \n Will 10", valeros.getSaves());
        assertEquals(1, valeros.getHeroPoints());
        assertEquals(4, valeros.getMaxDyingValue());
        assertEquals(10, valeros.getRecoveryDC());

        CharacterModel seoni = characters.get(1);
        assertEquals("Seoni", seoni.getCharacterName());
        assertEquals(12, seoni.getInitiative());
        assertEquals(2, seoni.getDebuffList().size());
        // Saved as "Frightened 2" in the name; the value is split off
        DebuffModel frightened = seoni.getDebuffList().get(0);
        assertEquals("Frightened", frightened.getName());
        assertEquals(2, frightened.getValue());
        assertEquals("Frightened 2", frightened.getDisplayName());
        assertFalse(frightened.isPersistentDamage());
        DebuffModel fire = seoni.getDebuffList().get(1);
        assertTrue(fire.isPersistentDamage());
        assertEquals("fire", fire.getDamageType());
        assertEquals("1d6", fire.getDamageValue());
        assertEquals(15, fire.getDifficultyClass());
        assertEquals(0, fire.getValue());

        CharacterModel kyra = characters.get(2);
        assertEquals("Kyra", kyra.getCharacterName());
        assertTrue(kyra.isDying());
        assertEquals(2, kyra.getDyingValue());
        assertEquals(1, kyra.getWoundedValue());
        assertEquals(1, kyra.getReactionList().size());
        ReactionModel shieldBlock = kyra.getReactionList().get(0);
        assertEquals("Shield Block", shieldBlock.getName());
        assertEquals("Reduce damage by hardness", shieldBlock.getDescription());
        assertFalse(shieldBlock.isAvailable());

        CharacterModel goblin = characters.get(3);
        assertEquals("Goblin Boss", goblin.getCharacterName());
        assertFalse(goblin.isPC());
    }

    @Test
    public void savingKeepsTheOtherKeysAndLoadsBackTheSame() throws Exception {
        MemoryKeyValueStore values = OneXSave.load();
        int keys = values.size();
        List<CharacterModel> characters = EncounterJson.fromJson(values.getString("GSON_TAG", null));

        String json = EncounterJson.toJson(characters);
        List<CharacterModel> again = EncounterJson.fromJson(json);

        assertEquals(json, EncounterJson.toJson(again));
        assertEquals(keys, values.size());
        // The value is saved in the overrideValue field
        assertTrue(json.contains("\"overrideValue\":2"));
    }

    @Test
    public void unreadableSaveGivesNull() {
        assertNull(EncounterJson.fromJson(null));
        assertNull(EncounterJson.fromJson(""));
        assertNull(EncounterJson.fromJson("not json"));
    }

    @Test
    public void saveWithoutListsLoadsWithEmptyLists() {
        List<CharacterModel> characters = EncounterJson.fromJson("[{\"characterName\":\"Old\",\"hp\":3}]");

        assertEquals(1, characters.size());
        assertTrue(characters.get(0).getDebuffList().isEmpty());
        assertTrue(characters.get(0).getReactionList().isEmpty());
    }

    @Test
    public void storeKeepsThe1xSettingKeys() {
        MemoryKeyValueStore values = new MemoryKeyValueStore();
        EncounterStore store = new EncounterStore(values);

        assertFalse(store.isHideDescriptions());
        assertFalse(store.isHideReactions());
        store.setHideDescriptions(true);
        store.setHideReactions(true);

        assertTrue(values.getBoolean("SHARED_PREF_HIDE_DESC", false));
        assertTrue(values.getBoolean("SHARED_PREF_HIDE_REACTIONS", false));
    }
}
