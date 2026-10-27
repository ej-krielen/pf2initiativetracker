package com.rekijan.initiativetrackersecondedition.feature.encounter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the turn order rules of {@link Encounter}: sorting (monsters first on a tie), next turn
 * with its end and start of turn steps, round counting, removing, knocking out and reordering.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public class EncounterTest {

    private static CharacterModel character(String name, int initiative, boolean pc) {
        CharacterModel c = new CharacterModel();
        c.setCharacterName(name);
        c.setInitiative(initiative);
        c.setPC(pc);
        return c;
    }

    private static Encounter encounterOf(CharacterModel... characters) {
        characters[0].setFirstInRound(true);
        return new Encounter(new ArrayList<>(Arrays.asList(characters)), 1);
    }

    private static List<String> names(Encounter encounter) {
        List<String> names = new ArrayList<>();
        for (CharacterModel c : encounter.getCharacters()) names.add(c.getCharacterName());
        return names;
    }

    @Test
    public void sortOrdersHighToLowWithMonstersFirstOnATie() {
        Encounter e = encounterOf(character("Seoni", 12, true), character("Valeros", 18, true),
                character("Goblin", 12, false), character("Rat", 3, false));

        boolean duplicate = e.sortByInitiative();

        assertFalse(duplicate);
        assertEquals(Arrays.asList("Valeros", "Goblin", "Seoni", "Rat"), names(e));
        assertTrue(e.get(0).isFirstInRound());
        assertFalse(e.get(2).isFirstInRound());
    }

    @Test
    public void sortWarnsForATieOnTheSameSide() {
        Encounter e = encounterOf(character("Seoni", 12, true), character("Kyra", 12, true));
        assertTrue(e.sortByInitiative());
    }

    @Test
    public void nextTurnRotatesAndCountsRounds() {
        Encounter e = encounterOf(character("A", 20, true), character("B", 10, false));

        Encounter.Turn turn = e.nextTurn();
        assertEquals("A", turn.ended.getCharacterName());
        assertEquals("B", turn.active.getCharacterName());
        assertFalse(turn.newRound);
        assertEquals(1, e.getRound());

        turn = e.nextTurn();
        assertTrue(turn.newRound);
        assertEquals(2, e.getRound());
    }

    @Test
    public void nextTurnNeedsTwoCharacters() {
        assertNull(encounterOf(character("A", 1, true)).nextTurn());
    }

    @Test
    public void endOfTurnStepsHappenToTheCharacterWhoseTurnEnded() {
        CharacterModel a = character("A", 20, true);
        CharacterModel b = character("B", 10, false);
        DebuffModel frightened = new DebuffModel("Frightened", "", 0, 1);
        DebuffModel offGuard = new DebuffModel("Off-Guard", "", 1, 0);
        DebuffModel fire = DebuffModel.persistentDamage("fire damage (1d6)", "fire", "1d6", 15, 0, "");
        a.getDebuffList().addAll(Arrays.asList(frightened, offGuard, fire));
        DebuffModel bFrightened = new DebuffModel("Frightened", "", 0, 2);
        b.getDebuffList().add(bFrightened);
        Encounter e = encounterOf(a, b);

        Encounter.Turn turn = e.nextTurn();

        assertEquals(Arrays.asList(fire), turn.persistentDamage);
        assertEquals(Arrays.asList(offGuard), turn.expiredDebuffs);
        assertFalse(a.getDebuffList().contains(frightened));
        // B's turn only starts, their frightened stays
        assertEquals(2, bFrightened.getValue());
    }

    @Test
    public void stepsOfTheEndedTurnComeBeforeThoseOfTheNewTurn() {
        // The user's case: Seoni has persistent damage, Kyra after her is dying
        CharacterModel seoni = character("Seoni", 20, true);
        DebuffModel fire = DebuffModel.persistentDamage("fire damage (1d6)", "fire", "1d6", 15, 0, "");
        DebuffModel acid = DebuffModel.persistentDamage("acid damage (2)", "acid", "2", 15, 0, "");
        DebuffModel offGuard = new DebuffModel("Off-Guard", "", 1, 0);
        seoni.getDebuffList().addAll(Arrays.asList(fire, offGuard, acid));
        CharacterModel kyra = character("Kyra", 10, true);
        kyra.setFastHealing(2);
        kyra.setRegeneration(3);
        kyra.knockOut(1);
        Encounter e = encounterOf(seoni, kyra);

        List<TurnStep> steps = e.nextTurn().steps();

        List<TurnStep.Kind> kinds = new ArrayList<>();
        for (TurnStep s : steps) kinds.add(s.kind);
        assertEquals(Arrays.asList(TurnStep.Kind.PERSISTENT_DAMAGE, TurnStep.Kind.PERSISTENT_DAMAGE,
                TurnStep.Kind.EXPIRED_DEBUFF, TurnStep.Kind.FAST_HEALING, TurnStep.Kind.REGENERATION,
                TurnStep.Kind.RECOVERY_CHECK), kinds);
        assertSame(fire, steps.get(0).debuff);
        assertSame(acid, steps.get(1).debuff);
        assertSame(offGuard, steps.get(2).debuff);
        assertSame(seoni, steps.get(2).character);
        assertSame(kyra, steps.get(5).character);
    }

    @Test
    public void aQuietTurnHasNoSteps() {
        Encounter e = encounterOf(character("A", 20, true), character("B", 10, false));
        assertTrue(e.nextTurn().steps().isEmpty());
    }

    @Test
    public void startOfTurnResetsReactionsAndAsksDyingForARecoveryCheck() {
        CharacterModel a = character("A", 20, false);
        CharacterModel b = character("B", 10, true);
        b.setReactionAvailable(false);
        b.knockOut(1);
        Encounter e = encounterOf(a, b);

        Encounter.Turn turn = e.nextTurn();

        assertTrue(b.isReactionAvailable());
        assertTrue(turn.recoveryCheck);
    }

    @Test
    public void deadCharactersTakeNoPersistentDamageAndNoRecoveryCheck() {
        CharacterModel a = character("A", 20, true);
        a.knockOut(4);
        a.getDebuffList().add(DebuffModel.persistentDamage("bleed", "bleed", "2", 15, 0, ""));
        CharacterModel b = character("B", 10, true);
        b.knockOut(4);
        Encounter e = encounterOf(a, b);

        Encounter.Turn turn = e.nextTurn();

        assertTrue(turn.persistentDamage.isEmpty());
        assertFalse(turn.recoveryCheck);
    }

    @Test
    public void removingFirstInRoundPassesItToTheNextCharacter() {
        Encounter e = encounterOf(character("A", 20, true), character("B", 10, true), character("C", 5, true));
        e.setFirstInRound(1);

        e.remove(1);

        assertEquals(Arrays.asList("A", "C"), names(e));
        assertTrue(e.get(1).isFirstInRound());
    }

    @Test
    public void removingTheLastFirstInRoundWrapsToTheTop() {
        Encounter e = encounterOf(character("A", 20, true), character("B", 10, true));
        e.setFirstInRound(1);

        e.remove(1);

        assertTrue(e.get(0).isFirstInRound());
    }

    @Test
    public void knockedOutMovesToJustBeforeTheActiveCharacter() {
        CharacterModel a = character("A", 20, false);
        CharacterModel b = character("B", 15, true);
        CharacterModel c = character("C", 10, true);
        Encounter e = encounterOf(a, b, c);
        e.setFirstInRound(1);

        e.knockOut(b, 1);

        assertEquals(Arrays.asList("A", "C", "B"), names(e));
        assertTrue(b.isDying());
        assertEquals(1, b.getDyingValue());
        assertTrue(c.isFirstInRound());
        assertFalse(b.isFirstInRound());
    }

    @Test
    public void moveChangesTheOrder() {
        Encounter e = encounterOf(character("A", 1, true), character("B", 1, true), character("C", 1, true));

        e.move(0, 2);

        assertEquals(Arrays.asList("B", "C", "A"), names(e));
    }

    @Test
    public void pcsGoToMaxHpAndLoseWounded() {
        CharacterModel pc = character("PC", 1, true);
        pc.setMaxHp(20);
        pc.setHp(5);
        pc.setWoundedValue(2);
        CharacterModel boosted = character("Boosted", 1, true);
        boosted.setMaxHp(10);
        boosted.setHp(15);
        CharacterModel npc = character("NPC", 1, false);
        npc.setMaxHp(30);
        npc.setHp(1);
        Encounter e = encounterOf(pc, boosted, npc);

        Encounter.MaxHpResult result = e.setPcsToMaxHp();

        assertEquals(20, pc.getHp());
        assertEquals(0, pc.getWoundedValue());
        assertEquals(15, boosted.getHp());
        assertEquals(1, npc.getHp());
        assertEquals(Arrays.asList("Boosted"), result.skipped);
        assertTrue(result.woundedReset);
    }

    @Test
    public void roundNeverGoesBelowZeroAndResetsToOne() {
        Encounter e = encounterOf(character("A", 1, true));
        e.previousRound();
        e.previousRound();
        assertEquals(0, e.getRound());
        e.resetRound();
        assertEquals(1, e.getRound());
    }

    @Test
    public void listenersHearAboutChanges() {
        Encounter e = encounterOf(character("A", 1, true), character("B", 1, true));
        int[] calls = {0};
        Encounter.Listener listener = () -> calls[0]++;
        e.addListener(listener);

        e.nextTurn();
        e.removeListener(listener);
        e.nextTurn();

        assertEquals(1, calls[0]);
        assertSame(e.get(0), e.getCharacters().get(0));
    }
}
