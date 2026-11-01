package com.rekijan.initiativetrackersecondedition.feature.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetrackersecondedition.core.data.MemoryKeyValueStore;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;
import com.rekijan.initiativetrackersecondedition.feature.spell.Spells;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests the spell requests through Table sync: cast and end requests on a player's send, the
 * GM's copy with the PC's spells and answers (only written when there is something to say), the
 * cleaning of tampered values, the spells a caster has going, and the store keeping requests,
 * answers and spells.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 01-11-2026
 */
public class SpellRequestsTest {

    private static CharacterModel character(String name) {
        CharacterModel c = new CharacterModel();
        c.setCharacterName(name);
        return c;
    }

    private static SpellRequests.Cast fear() {
        SpellRequests.Cast cast = new SpellRequests.Cast();
        cast.at = 1000;
        cast.name = "Fear";
        cast.rounds = 3;
        cast.condition = "Frightened";
        cast.value = 1;
        cast.targets = "the two goblins in the burst";
        return cast;
    }

    @Test
    public void castAndEndTravelOnAPlayersSend() {
        PlayerSheet sheet = PlayerSheet.of(character("Kyra"));
        sheet.cast = fear();
        SpellRequests.End end = new SpellRequests.End();
        end.at = 2000;
        end.spellId = "abc";
        end.name = "Slow";
        sheet.end = end;

        Map<String, Object> map = sheet.toMap();
        PlayerSheet read = PlayerSheet.fromMap(map);

        assertNotNull(read.cast);
        assertEquals(1000, read.cast.at);
        assertEquals("Fear", read.cast.name);
        assertEquals(3, read.cast.rounds);
        assertEquals("Frightened", read.cast.condition);
        assertEquals(1, read.cast.value);
        assertEquals("the two goblins in the burst", read.cast.targets);
        assertEquals("abc", read.end.spellId);
        assertEquals("Slow", read.end.name);
    }

    @Test
    public void aSendWithoutRequestsHasNoRequestFields() {
        Map<String, Object> map = PlayerSheet.of(character("Kyra")).toMap();
        assertFalse(map.containsKey(SpellRequests.KEY_CAST));
        assertFalse(map.containsKey(SpellRequests.KEY_END));
        PlayerSheet read = PlayerSheet.fromMap(map);
        assertNull(read.cast);
        assertNull(read.end);
    }

    @Test
    public void requestsAreNotSheetChanges() {
        PlayerSheet gm = PlayerSheet.of(character("Kyra"));
        PlayerSheet sent = PlayerSheet.of(character("Kyra"));
        sent.cast = fear();
        assertTrue(SyncDiff.changes(gm, sent).isEmpty());
    }

    @Test
    public void tamperedRequestsAreCleanedOrDropped() {
        Map<String, Object> cast = new HashMap<>();
        cast.put("at", -5L);
        cast.put("name", "  Fear  ");
        cast.put("rounds", 99999L);
        cast.put("condition", "");
        cast.put("value", 7L);
        cast.put("targets", new String(new char[500]).replace('\0', 'x'));
        SpellRequests.Cast read = SpellRequests.Cast.read(cast);
        assertEquals(0, read.at);
        assertEquals("Fear", read.name);
        assertEquals(9999, read.rounds);
        // No condition, so no value
        assertEquals(0, read.value);
        assertEquals(SpellRequests.MAX_TARGETS_NOTE, read.targets.length());

        cast.put("name", "");
        assertNull(SpellRequests.Cast.read(cast));
        assertNull(SpellRequests.Cast.read("not a map"));

        Map<String, Object> end = new HashMap<>();
        end.put("at", 3L);
        end.put("spellId", "");
        end.put("name", "Slow");
        assertNull(SpellRequests.End.read(end));
    }

    @Test
    public void gmCopyOnlyWritesSpellFieldsWhenThereIsSomethingToSay() {
        GmCopy copy = GmCopy.of(character("Kyra"), 5);
        Map<String, Object> map = copy.toMap();
        assertFalse(map.containsKey(SpellRequests.KEY_SPELLS));
        assertFalse(map.containsKey(SpellRequests.KEY_CAST_ANSWER));
        assertFalse(map.containsKey(SpellRequests.KEY_END_ANSWER));

        SpellRequests.Spell spell = new SpellRequests.Spell();
        spell.id = "abc";
        spell.name = "Fear";
        spell.rounds = 2;
        spell.targets = 2;
        spell.added = "Frightened 1";
        copy.spells = Collections.singletonList(spell);
        copy.castAnswer = new SpellRequests.Answer(1000, true);
        copy.endAnswer = new SpellRequests.Answer(2000, false);

        GmCopy read = GmCopy.fromMap(copy.toMap());
        assertEquals(1, read.spells.size());
        assertEquals("abc", read.spells.get(0).id);
        assertEquals(2, read.spells.get(0).rounds);
        assertEquals(2, read.spells.get(0).targets);
        assertEquals("Frightened 1", read.spells.get(0).added);
        assertTrue(read.castAnswer.answers(1000));
        assertTrue(read.castAnswer.ok);
        assertFalse(read.castAnswer.answers(1001));
        assertTrue(read.endAnswer.answers(2000));
        assertFalse(read.endAnswer.ok);
    }

    @Test
    public void aRejectionNeverCarriesThePlayersRequests() {
        PlayerSheet sent = PlayerSheet.of(character("Kyra"));
        sent.cast = fear();
        Map<String, Object> map = GmCopy.rejection(sent).toMap();
        assertFalse(map.containsKey(SpellRequests.KEY_CAST));
        assertFalse(map.containsKey(SpellRequests.KEY_END));
    }

    @Test
    public void noAnswerYetAnswersNothing() {
        assertFalse(new SpellRequests.Answer().answers(0));
        assertFalse(new SpellRequests.Answer().answers(1000));
    }

    @Test
    public void spellsOfListsWhatTheCasterHasGoing() {
        CharacterModel kyra = character("Kyra");
        CharacterModel valeros = character("Valeros");
        CharacterModel goblin1 = character("Goblin 1");
        CharacterModel goblin2 = character("Goblin 2");
        Encounter encounter = new Encounter(new ArrayList<>(Arrays.asList(kyra, valeros, goblin1, goblin2)), 1);

        Spells.AddedCondition frightened = new Spells.AddedCondition("Frightened", "", 0)
                .set(goblin1, 1).set(goblin2, 2);
        String fear = Spells.cast(encounter, kyra, Arrays.asList(goblin1, goblin2), "Fear", 3, "",
                Collections.singletonList(frightened));
        Spells.cast(encounter, kyra, Collections.singletonList(kyra), "Light", 0, "", Collections.emptyList());
        // Someone else's spell isn't Kyra's
        Spells.cast(encounter, valeros, Collections.singletonList(goblin1), "Bless", 10, "", Collections.emptyList());

        List<SpellRequests.Spell> spells = SpellRequests.spellsOf(encounter, kyra);
        assertEquals(2, spells.size());
        // In the order of their first target: Light is on Kyra, who comes before the goblins
        assertEquals("Light", spells.get(0).name);
        assertEquals(0, spells.get(0).rounds);
        assertEquals(1, spells.get(0).targets);
        SpellRequests.Spell second = spells.get(1);
        assertEquals(fear, second.id);
        assertEquals("Fear", second.name);
        assertEquals(3, second.rounds);
        assertEquals(2, second.targets);
        assertEquals("Frightened 1, Frightened 2", second.added);
        assertTrue(SpellRequests.contains(spells, fear));

        Spells.end(encounter, fear, true);
        assertFalse(SpellRequests.contains(SpellRequests.spellsOf(encounter, kyra), fear));
    }

    @Test
    public void storeKeepsRequestsAnswersAndSpells() {
        MemoryKeyValueStore values = new MemoryKeyValueStore();
        SyncStore store = new SyncStore(values);
        store.setPlayerCode("pc", "ABCDEF");
        store.savePlayerCast("pc", fear());
        SpellRequests.Spell spell = new SpellRequests.Spell();
        spell.id = "abc";
        spell.name = "Fear";
        store.savePlayerSpells("pc", Collections.singletonList(spell));

        SyncStore reopened = new SyncStore(values);
        assertEquals("Fear", reopened.loadPlayerCast("pc").name);
        assertEquals("abc", reopened.loadPlayerSpells("pc").get(0).id);
        assertEquals(1001, reopened.nextRequestAt("pc", 1001));
        // The clock went back: still later than the last one
        assertEquals(1002, reopened.nextRequestAt("pc", 5));

        // Leaving forgets the requests and the spells
        reopened.setPlayerCode("pc", null);
        assertNull(reopened.loadPlayerCast("pc"));
        assertTrue(reopened.loadPlayerSpells("pc").isEmpty());

        Map<String, SpellRequests.Answer> answers = new HashMap<>();
        answers.put("doc", new SpellRequests.Answer(1000, true));
        reopened.startHosting("GHJKLM", "party");
        reopened.saveCastAnswers(answers);
        assertTrue(new SyncStore(values).loadCastAnswers().get("doc").ok);
        // A new session starts without answers
        reopened.startHosting("NPQRST", "party");
        assertTrue(reopened.loadCastAnswers().isEmpty());
    }
}
