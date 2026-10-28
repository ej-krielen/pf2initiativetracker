package com.rekijan.initiativetrackersecondedition.feature.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.character.ReactionModel;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests {@link PlayerSheet}: a sheet survives the trip to Firestore and back, incoming numbers and
 * texts are kept inside the limits, wrong types count as missing, and applying a sheet keeps the
 * character's (de)buffs and turn order.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-10-2026
 */
public class PlayerSheetTest {

    static PlayerSheet valeros() {
        PlayerSheet s = new PlayerSheet();
        s.name = "Valeros";
        s.initiative = 18;
        s.initiativeBonus = 2;
        s.hp = 30;
        s.maxHp = 34;
        s.ac = "AC 22";
        s.saves = "Fort +11 Ref +8 Will +7";
        s.skills = "Athletics +12";
        s.attackRoutine = "Longsword +13 (1d8+6)";
        s.notes = "Sudden Charge";
        s.heroPoints = 2;
        s.dying = true;
        s.dyingValue = 2;
        s.maxDying = 5;
        s.wounded = 1;
        s.doomed = 1;
        s.recoveryDc = 11;
        s.reactionAvailable = false;
        s.sentAt = 1000;
        return s;
    }

    @Test
    public void roundTripKeepsEverything() {
        PlayerSheet back = PlayerSheet.fromMap(valeros().toMap());
        assertEquals("Valeros", back.name);
        assertEquals(18, back.initiative);
        assertEquals(2, back.initiativeBonus);
        assertEquals(30, back.hp);
        assertEquals(34, back.maxHp);
        assertEquals("AC 22", back.ac);
        assertEquals("Fort +11 Ref +8 Will +7", back.saves);
        assertEquals("Athletics +12", back.skills);
        assertEquals("Longsword +13 (1d8+6)", back.attackRoutine);
        assertEquals("Sudden Charge", back.notes);
        assertEquals(2, back.heroPoints);
        assertTrue(back.dying);
        assertEquals(2, back.dyingValue);
        assertEquals(5, back.maxDying);
        assertEquals(1, back.wounded);
        assertEquals(1, back.doomed);
        assertEquals(11, back.recoveryDc);
        assertFalse(back.reactionAvailable);
        assertEquals(1000, back.sentAt);
    }

    @Test
    public void ofTakesTheStatsOfAPlayersCharacter() {
        CharacterModel c = new CharacterModel();
        c.setCharacterName(" Seoni ");
        c.setInitiative(14);
        c.setInitiativeBonus(3);
        c.setHp(21);
        c.setMaxHp(26);
        c.setAc("AC 18");
        c.setCharacterNotes("Familiar");
        c.getDebuffList().add(new DebuffModel("Shield", "+4 AC", 10, 0));
        PlayerSheet s = PlayerSheet.of(c);
        assertEquals("Seoni", s.name);
        assertEquals(14, s.initiative);
        assertEquals(3, s.initiativeBonus);
        assertEquals(21, s.hp);
        assertEquals(26, s.maxHp);
        assertEquals("AC 18", s.ac);
        assertEquals("Familiar", s.notes);
        assertEquals(0, s.sentAt);
    }

    @Test
    public void numbersAreSentAsLong() {
        Map<String, Object> map = valeros().toMap();
        assertTrue(map.get(PlayerSheet.KEY_HP) instanceof Long);
        assertTrue(map.get(PlayerSheet.KEY_SENT_AT) instanceof Long);
    }

    @Test
    public void incomingNumbersAreClamped() {
        Map<String, Object> map = valeros().toMap();
        map.put(PlayerSheet.KEY_INITIATIVE, 5000L);
        map.put(PlayerSheet.KEY_INITIATIVE_BONUS, Long.MIN_VALUE);
        map.put(PlayerSheet.KEY_HP, 1e12);
        map.put(PlayerSheet.KEY_MAX_HP, -5L);
        PlayerSheet s = PlayerSheet.fromMap(map);
        assertEquals(CharacterModel.MAX_INITIATIVE, s.initiative);
        assertEquals(CharacterModel.MIN_INITIATIVE, s.initiativeBonus);
        assertEquals(CharacterModel.MAX_HP, s.hp);
        assertEquals(0, s.maxHp);
    }

    @Test
    public void incomingTextsAreCut() {
        Map<String, Object> map = valeros().toMap();
        map.put(PlayerSheet.KEY_NAME, repeat('n', 80));
        map.put(PlayerSheet.KEY_NOTES, repeat('x', 5000));
        map.put(PlayerSheet.KEY_AC, "  AC 22  ");
        PlayerSheet s = PlayerSheet.fromMap(map);
        assertEquals(PlayerSheet.MAX_NAME, s.name.length());
        assertEquals(PlayerSheet.MAX_NOTES, s.notes.length());
        assertEquals("AC 22", s.ac);
    }

    @Test
    public void cuttingDoesNotSplitAnEmoji() {
        PlayerSheet s = new PlayerSheet();
        // A dragon emoji (two chars) right across the limit
        s.name = repeat('a', PlayerSheet.MAX_NAME - 1) + "🐉";
        String cut = s.cleaned().name;
        assertEquals(PlayerSheet.MAX_NAME - 1, cut.length());
        assertFalse(Character.isHighSurrogate(cut.charAt(cut.length() - 1)));
    }

    @Test
    public void wrongTypesCountAsMissing() {
        Map<String, Object> map = valeros().toMap();
        map.put(PlayerSheet.KEY_HP, "lots");
        map.put(PlayerSheet.KEY_SAVES, 12L);
        PlayerSheet s = PlayerSheet.fromMap(map);
        assertEquals(0, s.hp);
        assertEquals("", s.saves);
    }

    @Test
    public void noNameOrNoMapGivesNull() {
        Map<String, Object> map = valeros().toMap();
        map.put(PlayerSheet.KEY_NAME, "   ");
        assertNull(PlayerSheet.fromMap(map));
        assertNull(PlayerSheet.fromMap(null));
        assertNull(PlayerSheet.fromMap(new HashMap<>()));
    }

    @Test
    public void applyToReplacesSharedFieldsAndKeepsTurnOrder() {
        CharacterModel c = new CharacterModel();
        c.getDebuffList().add(new DebuffModel());
        c.setFirstInRound(true);
        c.setFastHealing(2);
        c.setSyncId("uid_abc");
        PlayerSheet s = valeros();
        s.regeneration = 5;
        s.conditions.add(new DebuffModel("Haste", "+1 attack", 3, 0));
        s.conditions.add(new DebuffModel("Bless", "", 5, 0));
        s.applyTo(c);
        assertEquals("Valeros", c.getCharacterName());
        assertEquals(30, c.getHp());
        assertEquals("Sudden Charge", c.getCharacterNotes());
        assertEquals(0, c.getFastHealing());
        assertEquals(5, c.getRegeneration());
        assertEquals(2, c.getDebuffList().size());
        assertEquals("Haste", c.getDebuffList().get(0).getName());
        // Not shared: stays as it was
        assertFalse(c.isPC());
        assertTrue(c.isFirstInRound());
        assertEquals("uid_abc", c.getSyncId());
    }

    @Test
    public void conditionsSurviveTheRoundTripAndAreLimited() {
        PlayerSheet s = valeros();
        for (int i = 0; i < 80; i++) s.conditions.add(new DebuffModel("c" + i, "d" + i, i, 0));
        PlayerSheet back = PlayerSheet.fromMap(s.toMap());
        assertEquals(PlayerSheet.MAX_CONDITIONS, back.conditions.size());
        assertEquals("c3", back.conditions.get(3).getName());
        assertEquals(3, back.conditions.get(3).getDuration());
        assertEquals("d3", back.conditions.get(3).getDescription());
    }

    @Test
    public void valuedConditionsAndPersistentDamageSurviveTheRoundTrip() {
        PlayerSheet s = valeros();
        s.conditions.add(new DebuffModel("Frightened", "-2 to checks and DCs", 0, 2));
        s.conditions.add(DebuffModel.persistentDamage("Persistent fire (1d6)", "fire", "1d6", 17, 0, ""));
        PlayerSheet back = PlayerSheet.fromMap(s.toMap());
        DebuffModel frightened = back.conditions.get(0);
        assertEquals("Frightened", frightened.getName());
        assertEquals(2, frightened.getValue());
        assertFalse(frightened.isPersistentDamage());
        DebuffModel fire = back.conditions.get(1);
        assertTrue(fire.isPersistentDamage());
        assertEquals("fire", fire.getDamageType());
        assertEquals("1d6", fire.getDamageValue());
        assertEquals(17, fire.getDifficultyClass());
    }

    @Test
    public void reactionsSurviveTheRoundTripAndAreLimited() {
        PlayerSheet s = valeros();
        for (int i = 0; i < 30; i++) {
            ReactionModel r = new ReactionModel();
            r.setName("r" + i);
            r.setDescription("d" + i);
            r.setAvailable(i % 2 == 0);
            s.reactions.add(r);
        }
        PlayerSheet back = PlayerSheet.fromMap(s.toMap());
        assertEquals(PlayerSheet.MAX_REACTIONS, back.reactions.size());
        assertEquals("r3", back.reactions.get(3).getName());
        assertEquals("d3", back.reactions.get(3).getDescription());
        assertFalse(back.reactions.get(3).isAvailable());
        assertTrue(back.reactions.get(4).isAvailable());
    }

    @Test
    public void missingDyingRulesGetThePf2Defaults() {
        Map<String, Object> map = valeros().toMap();
        map.remove(PlayerSheet.KEY_MAX_DYING);
        map.remove(PlayerSheet.KEY_RECOVERY_DC);
        map.remove(PlayerSheet.KEY_REACTION_AVAILABLE);
        map.put(PlayerSheet.KEY_HERO_POINTS, 500L);
        PlayerSheet s = PlayerSheet.fromMap(map);
        assertEquals(CharacterModel.DEFAULT_MAX_DYING, s.maxDying);
        assertEquals(CharacterModel.DEFAULT_RECOVERY_DC, s.recoveryDc);
        assertTrue(s.reactionAvailable);
        assertEquals(CharacterModel.MAX_HERO_POINTS, s.heroPoints);
    }

    @Test
    public void applyToSetsTheDyingTrackHeroPointsAndReactions() {
        CharacterModel c = new CharacterModel();
        ReactionModel old = new ReactionModel();
        old.setName("Old");
        c.getReactionList().add(old);
        PlayerSheet s = valeros();
        ReactionModel block = new ReactionModel();
        block.setName("Shield Block");
        s.reactions.add(block);
        s.applyTo(c);
        assertEquals(2, c.getHeroPoints());
        assertTrue(c.isDying());
        assertEquals(2, c.getDyingValue());
        assertEquals(5, c.getMaxDyingValue());
        assertEquals(1, c.getWoundedValue());
        assertEquals(1, c.getDoomedValue());
        assertEquals(11, c.getRecoveryDC());
        assertFalse(c.isReactionAvailable());
        assertEquals(1, c.getReactionList().size());
        assertEquals("Shield Block", c.getReactionList().get(0).getName());
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}
