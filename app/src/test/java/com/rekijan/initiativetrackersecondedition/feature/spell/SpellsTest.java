package com.rekijan.initiativetrackersecondedition.feature.spell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetrackersecondedition.core.data.EncounterJson;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;
import com.rekijan.initiativetrackersecondedition.feature.encounter.TurnStep;
import com.rekijan.initiativetrackersecondedition.feature.sync.PlayerSheet;
import com.rekijan.initiativetrackersecondedition.feature.sync.SyncDiff;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tests spells: casting on several targets with an added condition, counting down at the start of
 * the caster's turn while the added condition follows its own rule, ending (with or without the
 * added condition), a caster leaving the fight (spell cards), the order of the turn questions,
 * saving and loading, saves without spells and Table sync.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-10-2026
 */
public class SpellsTest {

    private static CharacterModel character(String name, int initiative) {
        CharacterModel c = new CharacterModel();
        c.setCharacterName(name);
        c.setInitiative(initiative);
        return c;
    }

    /** Kyra (caster, active and first in round), Valeros, Ogre, Goblin, in that order. */
    private CharacterModel kyra, valeros, ogre, goblin;
    private Encounter encounter;

    private void setUpParty() {
        kyra = character("Kyra", 20);
        valeros = character("Valeros", 15);
        ogre = character("Ogre", 12);
        goblin = character("Goblin", 8);
        kyra.setFirstInRound(true);
        encounter = new Encounter(new ArrayList<>(Arrays.asList(kyra, valeros, ogre, goblin)), 1);
    }

    private static DebuffModel only(CharacterModel c) {
        assertEquals(1, c.getDebuffList().size());
        return c.getDebuffList().get(0);
    }

    private static DebuffModel named(CharacterModel c, String name) {
        for (DebuffModel d : c.getDebuffList()) if (d.getName().equals(name)) return d;
        return null;
    }

    private static List<String> names(Encounter e) {
        List<String> names = new ArrayList<>();
        for (CharacterModel c : e.getCharacters()) names.add(c.getCharacterName());
        return names;
    }

    private static final List<Spells.AddedCondition> NONE = Collections.emptyList();

    /** One added condition: every listed target gets this value (or 1 for a tick). */
    private static List<Spells.AddedCondition> added(String name, int value, CharacterModel... targets) {
        Spells.AddedCondition a = new Spells.AddedCondition(name, "", 0);
        for (CharacterModel t : targets) a.set(t, value);
        return Collections.singletonList(a);
    }

    @Test
    public void castPutsTheSpellOnEveryTarget() {
        setUpParty();
        String spell = Spells.cast(encounter, kyra, Arrays.asList(valeros, ogre, goblin), "  Bless ", 3,
                "+1 attack", NONE);

        assertNotNull(spell);
        for (CharacterModel target : Arrays.asList(valeros, ogre, goblin)) {
            DebuffModel d = only(target);
            assertEquals("Bless", d.getName());
            assertEquals(3, d.getDuration());
            assertEquals(0, d.getValue());
            assertEquals(kyra.getStableId(), d.getCasterId());
            assertEquals("Kyra", d.getCasterName());
            assertEquals(spell, d.getSpellId());
        }
        assertTrue(kyra.getDebuffList().isEmpty());
        assertEquals(Arrays.asList(valeros, ogre, goblin), Spells.targetsOf(encounter, spell));
    }

    @Test
    public void castAddsTheChosenConditionNextToTheSpell() {
        setUpParty();
        String slow = Spells.cast(encounter, kyra, Arrays.asList(ogre, goblin), "Slow", 10, "",
                Collections.singletonList(new Spells.AddedCondition("Slowed", "Fewer actions", 0).set(ogre, 1).set(goblin, 1)));

        for (CharacterModel target : Arrays.asList(ogre, goblin)) {
            assertEquals(2, target.getDebuffList().size());
            DebuffModel slowed = target.getDebuffList().get(1);
            assertEquals("Slowed", slowed.getName());
            assertEquals("Fewer actions", slowed.getDescription());
            assertEquals(1, slowed.getValue());
            assertEquals(0, slowed.getDuration());
            assertFalse(slowed.hasCaster());
            assertNull(slowed.getSpellId());
            assertEquals(slow, slowed.getAddedBySpellId());
        }
        assertEquals("Slowed", Spells.addedConditionNames(encounter, slow));
        // Only the spell itself makes a character a target
        assertEquals(Arrays.asList(ogre, goblin), Spells.targetsOf(encounter, slow));
    }

    @Test
    public void castWithoutTargetsDoesNothing() {
        setUpParty();
        assertNull(Spells.cast(encounter, kyra, Collections.emptyList(), "Bless", 3, "", NONE));
    }

    @Test
    public void durationFieldIsEmptyOrOneTo9999() {
        assertEquals(0, Spells.parseDuration(""));
        assertEquals(0, Spells.parseDuration("  "));
        assertEquals(1, Spells.parseDuration("1"));
        assertEquals(9999, Spells.parseDuration("9999"));
        assertEquals(-1, Spells.parseDuration("0"));
        assertEquals(-1, Spells.parseDuration("10000"));
        assertEquals(-1, Spells.parseDuration("-3"));
        assertEquals(-1, Spells.parseDuration("3x"));
        assertEquals(-1, Spells.parseDuration("99999999999"));
    }

    @Test
    public void castClampsTheDuration() {
        setUpParty();
        Spells.cast(encounter, kyra, Collections.singletonList(ogre), "Bless", 99999, "", NONE);
        assertEquals(DebuffModel.MAX_DURATION, only(ogre).getDuration());
    }

    @Test
    public void spellCardsCanNotCastOrBeTargeted() {
        setUpParty();
        Spells.cast(encounter, kyra, Collections.singletonList(ogre), "Bless", 2, "", NONE);
        encounter.remove(kyra);
        assertEquals(Arrays.asList(valeros, ogre, goblin), Spells.combatants(encounter));
    }

    @Test
    public void spellCountsDownAtTheStartOfTheCastersTurnOnly() {
        setUpParty();
        Spells.cast(encounter, kyra, Arrays.asList(valeros, ogre, goblin), "Bless", 2, "", NONE);

        // Kyra, Valeros, Ogre end their turns: nothing counts down
        for (int i = 0; i < 3; i++) {
            Encounter.Turn turn = encounter.nextTurn();
            assertTrue(turn.expiredDebuffs.isEmpty());
            assertTrue(turn.expiredSpells.isEmpty());
            assertEquals(2, only(valeros).getDuration());
        }

        // Goblin ends, Kyra's turn starts: every target goes down
        Encounter.Turn turn = encounter.nextTurn();
        assertSame(kyra, turn.active);
        assertTrue(turn.expiredSpells.isEmpty());
        assertEquals(1, only(goblin).getDuration());
        assertEquals(1, only(valeros).getDuration());

        // Round two: ends at the start of Kyra's turn, on all three, asked once
        for (int i = 0; i < 3; i++) encounter.nextTurn();
        turn = encounter.nextTurn();
        assertSame(kyra, turn.active);
        assertEquals(3, turn.expiredSpells.size());
        assertSame(valeros, turn.expiredSpells.get(0).character);
        assertSame(ogre, turn.expiredSpells.get(1).character);
        assertSame(goblin, turn.expiredSpells.get(2).character);
        assertEquals(0, only(ogre).getDuration());
        assertEquals(3, encounter.getRound());
        List<TurnStep> steps = turn.steps();
        assertEquals(1, steps.size());
        assertEquals(TurnStep.Kind.EXPIRED_SPELL, steps.get(0).kind);
        assertEquals(3, steps.get(0).spell.size());
    }

    @Test
    public void ownConditionsStillCountAtTheEndOfTheirOwnTurn() {
        setUpParty();
        valeros.getDebuffList().add(new DebuffModel("Off-Guard", "", 2, 0));
        Spells.cast(encounter, kyra, Collections.singletonList(valeros), "Bless", 5, "", NONE);

        encounter.nextTurn(); // Valeros's turn starts
        Encounter.Turn turn = encounter.nextTurn(); // and ends

        assertSame(valeros, turn.ended);
        assertEquals(1, named(valeros, "Off-Guard").getDuration());
        assertEquals(5, named(valeros, "Bless").getDuration());
    }

    @Test
    public void addedFrightenedDropsAtTheEndOfItsOwnTurnApartFromTheSpell() {
        setUpParty();
        Spells.cast(encounter, kyra, Collections.singletonList(valeros), "Fear", 1, "", added("Frightened", 2, valeros));

        encounter.nextTurn(); // Valeros
        encounter.nextTurn(); // Valeros ends: frightened 2 -> 1, the spell is untouched
        assertEquals(1, named(valeros, "Frightened").getValue());
        assertEquals(1, named(valeros, "Fear").getDuration());

        encounter.nextTurn(); // Goblin
        Encounter.Turn turn = encounter.nextTurn(); // Kyra: the spell ends, frightened stays
        assertEquals(1, turn.expiredSpells.size());
        assertEquals(1, named(valeros, "Frightened").getValue());

        encounter.nextTurn(); // Valeros
        encounter.nextTurn(); // Valeros ends: frightened 1 -> gone
        assertNull(named(valeros, "Frightened"));
        assertNotNull(named(valeros, "Fear"));
    }

    @Test
    public void casterCanTargetThemselves() {
        setUpParty();
        Spells.cast(encounter, kyra, Arrays.asList(kyra, ogre), "Shield", 2, "", NONE);

        encounter.nextTurn(); // Kyra ends: her own spell doesn't count down there
        assertEquals(2, only(kyra).getDuration());
        for (int i = 0; i < 3; i++) encounter.nextTurn();

        assertSame(kyra, encounter.get(0));
        assertEquals(1, only(kyra).getDuration());
        assertEquals(1, only(ogre).getDuration());
    }

    @Test
    public void endTakesTheSpellOffEveryTarget() {
        setUpParty();
        ogre.getDebuffList().add(new DebuffModel("Prone", "", 0, 0));
        String bless = Spells.cast(encounter, kyra, Arrays.asList(valeros, ogre), "Bless", 3, "", NONE);
        Spells.cast(encounter, kyra, Collections.singletonList(ogre), "Haste", 3, "", NONE);

        Spells.end(encounter, bless, true);

        assertTrue(valeros.getDebuffList().isEmpty());
        assertEquals(Arrays.asList("Prone", "Haste"),
                Arrays.asList(ogre.getDebuffList().get(0).getName(), ogre.getDebuffList().get(1).getName()));
    }

    @Test
    public void endRemovesTheAddedConditionOnlyWhenAsked() {
        setUpParty();
        String slow = Spells.cast(encounter, kyra, Arrays.asList(ogre, goblin), "Slow", 10, "",
                added("Slowed", 1, ogre, goblin));
        String fear = Spells.cast(encounter, kyra, Collections.singletonList(goblin), "Fear", 1, "", added("Frightened", 1, goblin));

        Spells.end(encounter, slow, false);
        assertEquals("Slowed", only(ogre).getName());

        Spells.end(encounter, fear, true);
        assertEquals(Collections.singletonList("Slowed"), Collections.singletonList(only(goblin).getName()));
        assertNull(Spells.addedConditionNames(encounter, fear));
    }

    @Test
    public void removeExpiredTakesOffTheSpellAndItsAddedCondition() {
        setUpParty();
        goblin.getDebuffList().add(new DebuffModel("Prone", "", 0, 0));
        Spells.cast(encounter, kyra, Arrays.asList(ogre, goblin), "Slow", 1, "",
                added("Slowed", 1, ogre, goblin));
        for (int i = 0; i < 3; i++) encounter.nextTurn();
        Encounter.Turn turn = encounter.nextTurn(); // Kyra: Slow ends on both
        assertEquals("Slowed", Spells.addedConditionNames(turn.expiredSpells));

        Spells.removeExpired(encounter, turn.expiredSpells, true);

        assertTrue(ogre.getDebuffList().isEmpty());
        assertEquals("Prone", only(goblin).getName());
    }

    @Test
    public void endOfTurnQuestionsComeBeforeStartOfTurnQuestions() {
        setUpParty();
        Spells.cast(encounter, kyra, Collections.singletonList(ogre), "Bless", 1, "", NONE);
        goblin.getDebuffList().add(DebuffModel.persistentDamage("Fire", "fire", "1d6", 15, 0, ""));
        goblin.getDebuffList().add(new DebuffModel("Off-Guard", "", 1, 0));
        kyra.setFastHealing(2);
        kyra.setHp(5);
        kyra.setMaxHp(10);
        for (int i = 0; i < 3; i++) encounter.nextTurn();

        Encounter.Turn turn = encounter.nextTurn(); // Goblin ends, Kyra starts

        List<TurnStep.Kind> kinds = new ArrayList<>();
        for (TurnStep s : turn.steps()) kinds.add(s.kind);
        assertEquals(Arrays.asList(TurnStep.Kind.PERSISTENT_DAMAGE, TurnStep.Kind.EXPIRED_DEBUFF,
                TurnStep.Kind.EXPIRED_SPELL, TurnStep.Kind.FAST_HEALING), kinds);
    }

    @Test
    public void removedCasterIsReplacedBySpellCardsOnTheSameSpot() {
        setUpParty();
        encounter.nextTurn(); // Valeros's turn; Kyra at the bottom
        String bless = Spells.cast(encounter, kyra, Arrays.asList(valeros, ogre), "Bless", 2, "", NONE);
        String haste = Spells.cast(encounter, kyra, Collections.singletonList(goblin), "Haste", 3, "", NONE);

        encounter.remove(kyra);

        assertEquals(Arrays.asList("Valeros", "Ogre", "Goblin", "Bless", "Haste"), names(encounter));
        CharacterModel blessCard = encounter.get(3);
        CharacterModel hasteCard = encounter.get(4);
        assertTrue(blessCard.isSpellCard());
        assertEquals("Kyra", blessCard.getSpellCardCaster());
        assertEquals(20, blessCard.getInitiative());
        // Kyra was first in round: the first card takes that over
        assertTrue(blessCard.isFirstInRound());
        assertFalse(hasteCard.isFirstInRound());
        assertEquals(blessCard.getStableId(), only(valeros).getCasterId());
        assertEquals(hasteCard.getStableId(), only(goblin).getCasterId());
        assertEquals(bless, Spells.spellOfCard(encounter, blessCard));
        assertEquals(haste, Spells.spellOfCard(encounter, hasteCard));
        assertEquals(3, Spells.roundsLeft(encounter, hasteCard));

        List<Encounter.CasterGone> gone = encounter.takeCastersGone();
        assertEquals(1, gone.size());
        assertEquals("Kyra", gone.get(0).casterName);
        assertEquals(Arrays.asList(blessCard, hasteCard), gone.get(0).spellCards);
        assertTrue(encounter.takeCastersGone().isEmpty());

        // The spell keeps counting at the start of the card's turn, on the old initiative
        encounter.nextTurn(); // Ogre
        encounter.nextTurn(); // Goblin
        Encounter.Turn turn = encounter.nextTurn(); // Bless card
        assertSame(blessCard, turn.active);
        assertTrue(turn.newRound);
        assertFalse(turn.recoveryCheck);
        assertTrue(turn.steps().isEmpty());
        assertEquals(1, only(valeros).getDuration());
        assertEquals(3, only(goblin).getDuration());
        // Two spell cards share an initiative, which is no tie to warn about
        assertFalse(encounter.hasDuplicateInitiative());
    }

    @Test
    public void casterWithoutSpellsOnOthersJustLeaves() {
        setUpParty();
        Spells.cast(encounter, kyra, Collections.singletonList(kyra), "Shield", 2, "", NONE);

        encounter.remove(kyra);

        assertEquals(Arrays.asList("Valeros", "Ogre", "Goblin"), names(encounter));
        assertTrue(encounter.takeCastersGone().isEmpty());
        assertTrue(valeros.isFirstInRound());
    }

    @Test
    public void casterRemovedWithAllTargetsGetsNoCards() {
        setUpParty();
        Spells.cast(encounter, kyra, Collections.singletonList(ogre), "Bless", 2, "", NONE);

        encounter.removeAll(Arrays.asList(kyra, ogre));

        assertEquals(Arrays.asList("Valeros", "Goblin"), names(encounter));
        assertTrue(encounter.takeCastersGone().isEmpty());
        assertTrue(valeros.isFirstInRound());
    }

    @Test
    public void endingTheSpellOfACardRemovesTheCard() {
        setUpParty();
        String bless = Spells.cast(encounter, kyra, Arrays.asList(valeros, ogre), "Bless", 2, "",
                added("Frightened", 1, valeros, ogre));
        encounter.nextTurn(); // Valeros's turn
        encounter.remove(kyra);
        assertEquals(4, encounter.size());

        Spells.end(encounter, bless, true);

        assertEquals(Arrays.asList("Valeros", "Ogre", "Goblin"), names(encounter));
        assertTrue(valeros.getDebuffList().isEmpty());
        // The card was first in round; that went to the next in line
        assertTrue(valeros.isFirstInRound());
    }

    @Test
    public void removingASpellCardEndsItsSpell() {
        setUpParty();
        Spells.cast(encounter, kyra, Arrays.asList(valeros, ogre), "Bless", 2, "", NONE);
        encounter.remove(kyra);
        CharacterModel card = encounter.get(0);

        encounter.remove(card);

        assertTrue(valeros.getDebuffList().isEmpty());
        assertTrue(ogre.getDebuffList().isEmpty());
        assertEquals(3, encounter.size());
    }

    @Test
    public void spellCardWhoseSpellExpiredLeavesWhenTheTurnMovesOn() {
        setUpParty();
        Spells.cast(encounter, kyra, Collections.singletonList(ogre), "Bless", 1, "", NONE);
        encounter.nextTurn(); // Valeros
        encounter.remove(kyra);
        encounter.nextTurn(); // Ogre
        encounter.nextTurn(); // Goblin
        Encounter.Turn turn = encounter.nextTurn(); // the Bless card: it ends
        CharacterModel card = turn.active;
        assertTrue(card.isSpellCard());
        assertEquals(1, turn.expiredSpells.size());
        assertEquals(2, encounter.getRound());

        // The GM removes the ended spell; the card stays while it is having its turn
        Spells.removeExpired(encounter, turn.expiredSpells, true);
        assertSame(card, encounter.get(0));

        turn = encounter.nextTurn();
        assertSame(valeros, turn.active);
        assertSame(card, turn.ended);
        assertFalse(encounter.getCharacters().contains(card));
        assertEquals(3, encounter.size());
        // The round started on the card's turn, so Valeros doesn't start another
        assertFalse(turn.newRound);
        assertTrue(valeros.isFirstInRound());
        assertEquals(2, encounter.getRound());
    }

    @Test
    public void conditionWhoseCasterIsMissingCountsAtTheEndOfItsOwnTurn() {
        setUpParty();
        DebuffModel d = new DebuffModel("Bless", "", 2, 0);
        d.setCaster("someone-not-here", "Ghost", "spell");
        valeros.getDebuffList().add(d);

        encounter.nextTurn(); // Valeros
        encounter.nextTurn(); // Valeros ends

        assertEquals(1, d.getDuration());
    }

    @Test
    public void saveRoundTripKeepsCastersSpellCardsAndAddedConditions() {
        setUpParty();
        String bless = Spells.cast(encounter, kyra, Arrays.asList(valeros, ogre), "Bless", 2, "", added("Frightened", 1, valeros, ogre));
        Spells.cast(encounter, valeros, Collections.singletonList(goblin), "Haste", 3, "", NONE);
        encounter.remove(kyra);

        List<CharacterModel> loaded = EncounterJson.fromJson(EncounterJson.toJson(encounter.getCharacters()));
        Encounter reloaded = new Encounter(loaded, 1);

        CharacterModel card = loaded.get(0);
        assertTrue(card.isSpellCard());
        assertEquals("Kyra", card.getSpellCardCaster());
        assertEquals(card.getStableId(), encounter.get(0).getStableId());
        DebuffModel loadedBless = named(loaded.get(1), "Bless");
        assertEquals(card.getStableId(), loadedBless.getCasterId());
        assertEquals("Kyra", loadedBless.getCasterName());
        assertEquals(bless, loadedBless.getSpellId());
        assertEquals(bless, named(loaded.get(1), "Frightened").getAddedBySpellId());
        assertEquals(1, named(loaded.get(1), "Frightened").getValue());
        assertEquals(loaded.get(1).getStableId(), only(loaded.get(3)).getCasterId());

        // Still counts on the right turn after loading: Valeros's turn ticks Haste on Goblin
        reloaded.nextTurn(); // Valeros
        assertEquals(2, only(loaded.get(3)).getDuration());
        assertEquals(2, loadedBless.getDuration());
    }

    @Test
    public void saveWithoutSpellFieldsLoadsAndCountsAsAlways() {
        String save = "[{\"id\":0,\"initiative\":18,\"isFirstInRound\":true,\"characterName\":\"Seoni\","
                + "\"debuffList\":[{\"name\":\"Haste\",\"duration\":3,\"description\":\"\"},"
                + "{\"name\":\"Frightened 2\",\"duration\":0,\"description\":\"\"}]},"
                + "{\"id\":0,\"initiative\":9,\"characterName\":\"Goblin\","
                + "\"debuffList\":[{\"name\":\"Off-Guard\",\"duration\":2,\"description\":\"\"}]}]";
        List<CharacterModel> loaded = EncounterJson.fromJson(save);
        Encounter e = new Encounter(loaded, 1);

        DebuffModel haste = loaded.get(0).getDebuffList().get(0);
        assertFalse(haste.hasCaster());
        assertFalse(haste.hasGmLinks());
        assertEquals("", haste.getCasterName());
        assertFalse(loaded.get(0).isSpellCard());
        assertNotEquals(loaded.get(0).getStableId(), loaded.get(1).getStableId());

        e.nextTurn(); // Seoni ends
        assertEquals(2, haste.getDuration());
        assertEquals(1, loaded.get(0).getDebuffList().get(1).getValue());
        e.nextTurn(); // Goblin ends
        assertEquals(1, loaded.get(1).getDebuffList().get(0).getDuration());
    }

    @Test
    public void syncSendsTheCasterNameAndTheGmKeepsTheSpellLinks() {
        setUpParty();
        String slow = Spells.cast(encounter, kyra, Collections.singletonList(valeros), "Slow", 3, "",
                added("Slowed", 1, valeros));
        valeros.getDebuffList().add(new DebuffModel("Prone", "", 0, 0));

        // To the player: the caster's name only, no ids
        Map<String, Object> map = PlayerSheet.of(valeros).toMap();
        assertFalse(map.toString().contains(kyra.getStableId()));
        assertFalse(map.toString().contains(slow));
        PlayerSheet player = PlayerSheet.read(map);
        assertEquals("Kyra", player.conditions.get(0).getCasterName());
        assertFalse(player.conditions.get(0).hasCaster());
        assertNull(player.conditions.get(1).getAddedBySpellId());
        assertEquals("", player.conditions.get(2).getCasterName());

        // The caster's name alone is no change for the GM to approve
        PlayerSheet gm = PlayerSheet.of(valeros);
        assertTrue(SyncDiff.changes(gm, PlayerSheet.read(player.toMap())).isEmpty());

        // The player changes the rounds; the GM approves: still Kyra's spell, Slowed still linked
        player.conditions.get(0).setDuration(1);
        PlayerSheet sent = PlayerSheet.read(player.toMap());
        PlayerSheet.read(SyncDiff.apply(gm, sent, SyncDiff.changes(gm, sent)).toMap()).applyTo(valeros);
        DebuffModel spell = named(valeros, "Slow");
        assertEquals(1, spell.getDuration());
        assertEquals(kyra.getStableId(), spell.getCasterId());
        assertEquals(slow, spell.getSpellId());
        assertEquals(slow, named(valeros, "Slowed").getAddedBySpellId());
        assertFalse(named(valeros, "Prone").hasGmLinks());
    }

    @Test
    public void eachTargetGetsItsOwnValueForEveryAddedCondition() {
        setUpParty();
        // Fear: Ogre failed (1), Goblin critically failed (3 and fleeing), Valeros saved (only the spell)
        Spells.AddedCondition frightened = new Spells.AddedCondition("Frightened", "", 0)
                .set(ogre, 1).set(goblin, 3).set(valeros, 0);
        Spells.AddedCondition fleeing = new Spells.AddedCondition("Fleeing", "", 0).set(goblin, 1);
        String fear = Spells.cast(encounter, kyra, Arrays.asList(valeros, ogre, goblin), "Fear", 1, "",
                Arrays.asList(frightened, fleeing));

        assertEquals("Fear", only(valeros).getName());
        assertEquals(2, ogre.getDebuffList().size());
        assertEquals(1, named(ogre, "Frightened").getValue());
        assertEquals(3, goblin.getDebuffList().size());
        assertEquals(3, named(goblin, "Frightened").getValue());
        assertEquals(0, named(goblin, "Fleeing").getValue());
        assertEquals(fear, named(goblin, "Fleeing").getAddedBySpellId());
        assertEquals("Frightened, Fleeing", Spells.addedConditionNames(encounter, fear));

        Spells.end(encounter, fear, true);
        assertTrue(goblin.getDebuffList().isEmpty());
    }

    @Test
    public void lockWhileTheSpellLastsKeepsFrightenedFromDroppingBelowItsMinimum() {
        setUpParty();
        String fear = Spells.cast(encounter, kyra, Collections.singletonList(valeros), "Fear", 5, "",
                Collections.singletonList(new Spells.AddedCondition("Frightened", "", 1).set(valeros, 2)));
        DebuffModel frightened = named(valeros, "Frightened");
        assertTrue(frightened.isLocked());

        encounter.nextTurn(); // Valeros
        encounter.nextTurn(); // Valeros ends: 2 -> 1
        assertEquals(1, frightened.getValue());
        for (int i = 0; i < 4; i++) encounter.nextTurn();
        encounter.nextTurn(); // Valeros ends again: held at 1
        assertEquals(1, frightened.getValue());

        Spells.end(encounter, fear, false); // the lock goes with the spell, the condition stays
        assertFalse(frightened.isLocked());
        for (int i = 0; i < 4; i++) encounter.nextTurn();
        encounter.nextTurn(); // Valeros ends: 1 -> gone
        assertNull(named(valeros, "Frightened"));
    }

    @Test
    public void lockAddedLaterCanCountOnAnotherCharactersTurn() {
        setUpParty();
        goblin.getDebuffList().add(new DebuffModel("Frightened", "", 0, 1));
        encounter.nextTurn(); // Valeros's turn: Shatter Defenses on the Goblin until his next turn
        DebuffModel frightened = only(goblin);
        frightened.lock(1, 1, valeros.getStableId(), null);

        encounter.nextTurn(); // Ogre
        encounter.nextTurn(); // Goblin
        encounter.nextTurn(); // Goblin ends: held at 1
        assertEquals(1, frightened.getValue());
        encounter.nextTurn(); // Valeros starts: the lock ends
        assertFalse(frightened.isLocked());
        encounter.nextTurn(); // Ogre
        encounter.nextTurn(); // Goblin
        encounter.nextTurn(); // Goblin ends: gone
        assertTrue(goblin.getDebuffList().isEmpty());
    }

    @Test
    public void lockWithRoundsOnItsOwnTurnHoldsOnceThenOpens() {
        setUpParty();
        valeros.getDebuffList().add(new DebuffModel("Frightened", "", 0, 1));
        DebuffModel frightened = only(valeros);
        frightened.lock(1, 1, null, null);

        encounter.nextTurn(); // Valeros
        encounter.nextTurn(); // Valeros ends: held, and the lock's round is used up
        assertEquals(1, frightened.getValue());
        assertFalse(frightened.isLocked());
        for (int i = 0; i < 4; i++) encounter.nextTurn();
        encounter.nextTurn(); // Valeros ends: gone
        assertTrue(valeros.getDebuffList().isEmpty());
    }

    @Test
    public void lockIsSavedAndStaysWithTheGm() {
        setUpParty();
        valeros.getDebuffList().add(new DebuffModel("Frightened", "", 0, 2));
        only(valeros).lock(1, 0, null, null);

        List<CharacterModel> loaded = EncounterJson.fromJson(EncounterJson.toJson(encounter.getCharacters()));
        assertTrue(only(loaded.get(1)).isLocked());
        assertEquals(1, only(loaded.get(1)).getLockMin());

        // Not sent to the player; kept when a player's change is approved
        Map<String, Object> map = PlayerSheet.of(valeros).toMap();
        assertFalse(map.toString().toLowerCase(java.util.Locale.ROOT).contains("lock"));
        PlayerSheet player = PlayerSheet.read(map);
        player.conditions.get(0).setValue(1);
        PlayerSheet.read(player.toMap()).applyTo(valeros);
        assertEquals(1, only(valeros).getValue());
        assertTrue(only(valeros).isLocked());
    }
}
