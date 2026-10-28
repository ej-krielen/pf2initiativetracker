package com.rekijan.initiativetrackersecondedition.feature.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.character.ReactionModel;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests {@link SyncDiff}: field and condition changes are found, and applying only the accepted
 * ones leaves the rest as the GM had it.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-10-2026
 */
public class SyncDiffTest {

    private static PlayerSheet gm() {
        PlayerSheet s = new PlayerSheet();
        s.name = "Valeros";
        s.hp = 20;
        s.maxHp = 34;
        s.notes = "Power attack";
        s.conditions.add(new DebuffModel("Shaken", "-2 attacks", 2, 0));
        s.conditions.add(new DebuffModel("Bless", "+1 attack", 5, 0));
        return s;
    }

    private static PlayerSheet player() {
        PlayerSheet s = gm();
        s.hp = 30;
        s.maxHp = 36;
        s.conditions = new ArrayList<>();
        s.conditions.add(new DebuffModel("Bless", "+1 attack", 4, 0));
        s.conditions.add(new DebuffModel("Haste", "+1 attack, +30 ft", 3, 0));
        return s;
    }

    private static SyncDiff.Change find(List<SyncDiff.Change> changes, SyncDiff.Kind kind, String name) {
        for (SyncDiff.Change c : changes) {
            if (c.kind == kind && (name == null || name.equals(c.from) || (c.condition != null && name.equals(c.condition.getName())))) {
                return c;
            }
        }
        throw new AssertionError("No " + kind + " " + name);
    }

    @Test
    public void sameSheetsHaveNoChanges() {
        assertTrue(SyncDiff.changes(gm(), gm()).isEmpty());
    }

    @Test
    public void findsFieldAndConditionChanges() {
        List<SyncDiff.Change> changes = SyncDiff.changes(gm(), player());
        // HP, max HP, Bless rounds, Haste added, Shaken removed
        assertEquals(5, changes.size());
        SyncDiff.Change hp = changes.get(0);
        assertEquals(SyncDiff.Field.HP, hp.field);
        assertEquals("20", hp.from);
        assertEquals("30", hp.to);
        find(changes, SyncDiff.Kind.CONDITION_ADDED, "Haste");
        find(changes, SyncDiff.Kind.CONDITION_REMOVED, "Shaken");
        assertEquals(4, find(changes, SyncDiff.Kind.CONDITION_CHANGED, "Bless").condition.getDuration());
    }

    @Test
    public void keysAreUnique() {
        List<SyncDiff.Change> changes = SyncDiff.changes(gm(), player());
        List<String> keys = new ArrayList<>();
        for (SyncDiff.Change c : changes) {
            assertTrue(c.key, !keys.contains(c.key));
            keys.add(c.key);
        }
    }

    @Test
    public void applyAllGivesThePlayersSheet() {
        PlayerSheet result = SyncDiff.apply(gm(), player(), SyncDiff.changes(gm(), player()));
        assertTrue(SyncDiff.changes(player(), result).isEmpty() || sameExceptOrder(player(), result));
    }

    @Test
    public void applyOnlyTheAcceptedChanges() {
        List<SyncDiff.Change> changes = SyncDiff.changes(gm(), player());
        List<SyncDiff.Change> accepted = new ArrayList<>();
        accepted.add(changes.get(1)); // max HP
        accepted.add(find(changes, SyncDiff.Kind.CONDITION_ADDED, "Haste"));

        PlayerSheet result = SyncDiff.apply(gm(), player(), accepted);

        assertEquals(20, result.hp);   // refused: the GM's value
        assertEquals(36, result.maxHp);
        assertEquals(3, result.conditions.size());
        assertEquals("Shaken", result.conditions.get(0).getName()); // removal refused
        assertEquals(5, result.conditions.get(1).getDuration());    // Bless change refused
        assertEquals("Haste", result.conditions.get(2).getName());
    }

    @Test
    public void applyNothingKeepsTheGmsSheet() {
        PlayerSheet result = SyncDiff.apply(gm(), player(), Collections.emptyList());
        assertTrue(SyncDiff.changes(gm(), result).isEmpty());
    }

    @Test
    public void conditionNamesMatchIgnoringCase() {
        PlayerSheet p = gm();
        p.conditions.set(0, new DebuffModel("shaken", "-2 attacks", 2, 0));
        List<SyncDiff.Change> changes = SyncDiff.changes(gm(), p);
        assertEquals(1, changes.size());
        assertEquals(SyncDiff.Kind.CONDITION_CHANGED, changes.get(0).kind);
    }

    @Test
    public void changedFieldsListsEachFieldOnce() {
        List<SyncDiff.Field> fields = SyncDiff.changedFields(gm(), player());
        assertEquals(3, fields.size());
        assertTrue(fields.contains(SyncDiff.Field.CONDITIONS));
    }

    /** Applying all puts added conditions at the end, so only the order may differ. */
    private static boolean sameExceptOrder(PlayerSheet a, PlayerSheet b) {
        if (a.conditions.size() != b.conditions.size()) return false;
        for (DebuffModel d : a.conditions) {
            boolean found = false;
            for (DebuffModel e : b.conditions) {
                if (d.getName().equals(e.getName()) && d.getDuration() == e.getDuration()) found = true;
            }
            if (!found) return false;
        }
        PlayerSheet aa = a.cleaned();
        PlayerSheet bb = b.cleaned();
        aa.conditions = new ArrayList<>();
        bb.conditions = new ArrayList<>();
        return SyncDiff.changes(aa, bb).isEmpty();
    }

    private static ReactionModel reaction(String name, boolean available) {
        ReactionModel r = new ReactionModel();
        r.setName(name);
        r.setAvailable(available);
        return r;
    }

    @Test
    public void findsPf2FieldsAndAppliesThemOneByOne() {
        PlayerSheet gm = gm();
        PlayerSheet player = gm();
        player.heroPoints = 0;
        gm.heroPoints = 1;
        player.wounded = 1;
        player.dying = true;
        player.reactionAvailable = false;
        List<SyncDiff.Change> changes = SyncDiff.changes(gm, player);
        assertEquals(4, changes.size());
        SyncDiff.Change hero = find(changes, SyncDiff.Kind.FIELD, "1");
        assertEquals(SyncDiff.Field.HERO_POINTS, hero.field);
        assertEquals("0", hero.to);
        SyncDiff.Change dying = changes.get(1);
        assertEquals(SyncDiff.Field.DYING, dying.field);
        assertTrue(dying.field.isSwitch());
        assertEquals("true", dying.to);
        PlayerSheet result = SyncDiff.apply(gm, player, Collections.singletonList(hero));
        assertEquals(0, result.heroPoints);
        assertFalse(result.dying);
        assertEquals(0, result.wounded);
        assertTrue(result.reactionAvailable);
    }

    @Test
    public void conditionValueAndPersistentDamageAreChanges() {
        PlayerSheet gm = gm();
        gm.conditions.add(new DebuffModel("Frightened", "", 0, 2));
        gm.conditions.add(DebuffModel.persistentDamage("Persistent fire", "fire", "1d6", 15, 0, ""));
        PlayerSheet player = gm();
        player.conditions.add(new DebuffModel("Frightened", "", 0, 1));
        player.conditions.add(DebuffModel.persistentDamage("Persistent fire", "fire", "2d6", 15, 0, ""));
        List<SyncDiff.Change> changes = SyncDiff.changes(gm, player);
        assertEquals(2, changes.size());
        assertEquals(1, find(changes, SyncDiff.Kind.CONDITION_CHANGED, "Frightened").condition.getValue());
        PlayerSheet result = SyncDiff.apply(gm, player, changes);
        assertEquals("2d6", result.conditions.get(3).getDamageValue());
        assertTrue(result.conditions.get(3).isPersistentDamage());
    }

    @Test
    public void reactionsAreAddedRemovedAndChanged() {
        PlayerSheet gm = gm();
        gm.reactions.add(reaction("Shield Block", true));
        gm.reactions.add(reaction("Attack of Opportunity", true));
        PlayerSheet player = gm();
        player.reactions.add(reaction("shield block", false));
        player.reactions.add(reaction("Nimble Dodge", true));
        List<SyncDiff.Change> changes = SyncDiff.changes(gm, player);
        assertEquals(3, changes.size());
        find(changes, SyncDiff.Kind.REACTION_ADDED, "Nimble Dodge");
        find(changes, SyncDiff.Kind.REACTION_REMOVED, "Attack of Opportunity");
        SyncDiff.Change used = find(changes, SyncDiff.Kind.REACTION_CHANGED, "Shield Block");
        assertFalse(used.reaction.isAvailable());
        PlayerSheet result = SyncDiff.apply(gm, player, Collections.singletonList(used));
        assertEquals(2, result.reactions.size());
        assertFalse(result.reactions.get(0).isAvailable());
        assertEquals("Attack of Opportunity", result.reactions.get(1).getName());
    }
}
