package com.rekijan.initiativetrackersecondedition.feature.spell;

import com.rekijan.initiativetrackersecondedition.core.util.NumberInput;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;

import com.rekijan.initiativetrackersecondedition.feature.condition.Conditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Casting a spell: one condition per target, all with the same caster and spell id, so they count
 * down at the start of the caster's turn ({@link Encounter#nextTurn}) and can be ended together.
 * A cast can also add normal conditions, such as frightened and fleeing from Fear, with a value
 * (or not at all) per target for the degrees of success. Those follow their own rules (frightened
 * still goes down at the end of its own character's turn, unless locked while the spell lasts)
 * and only remember the spell, so they can be removed when the spell ends. Plain Java, so the
 * rules are unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-10-2026
 */
public final class Spells {

    // The same limits Table sync uses for a condition (PlayerSheet)
    public static final int MAX_NAME = 100;
    public static final int MAX_DESCRIPTION = 500;

    /**
     * A condition that a cast adds next to the spell, with what each target gets: for a valued
     * condition its value, for another 1; 0 (or nothing set) leaves that target out, e.g. one that
     * saved. Optionally locked while the spell lasts.
     */
    public static final class AddedCondition {
        public final String name;
        public final String description;
        /** The lowest value it can go down to by itself while the spell lasts; 0 for no lock. */
        public final int lockMin;
        private final Map<CharacterModel, Integer> values = new HashMap<>();

        public AddedCondition(String name, String description, int lockMin) {
            this.name = name == null ? "" : name.trim();
            this.description = description == null ? "" : description;
            this.lockMin = lockMin;
        }

        /** Sets what this target gets: the value, 1 for a condition without one, 0 for nothing. */
        public AddedCondition set(CharacterModel target, int value) {
            values.put(target, value);
            return this;
        }

        public int valueFor(CharacterModel target) {
            Integer value = values.get(target);
            return value == null ? 0 : value;
        }
    }

    private Spells() {
    }

    /**
     * Puts the spell as a condition on every target, plus the added conditions each target gets,
     * and tells the encounter. A locked added condition with a value is locked while the spell lasts.
     *
     * @param duration In rounds, kept between 0 (no timer) and {@link DebuffModel#MAX_DURATION}.
     * @param added    The conditions to add next to the spell (may be empty).
     * @return The new spell's id, or null when there are no targets.
     */
    public static String cast(Encounter encounter, CharacterModel caster, List<CharacterModel> targets,
                              String name, int duration, String description, List<AddedCondition> added) {
        if (targets.isEmpty()) return null;
        String spellId = UUID.randomUUID().toString();
        String cleanName = cut(name == null ? "" : name.trim(), MAX_NAME);
        String cleanDescription = cut(description == null ? "" : description.trim(), MAX_DESCRIPTION);
        int rounds = NumberInput.clamp(duration, 0, DebuffModel.MAX_DURATION);
        for (CharacterModel target : targets) {
            DebuffModel d = new DebuffModel(cleanName, cleanDescription, rounds, 0);
            d.setCaster(caster.getStableId(), caster.getCharacterName(), spellId);
            target.getDebuffList().add(d);
            for (AddedCondition a : added) {
                int value = a.valueFor(target);
                if (a.name.isEmpty() || value <= 0) continue;
                boolean valued = Conditions.isValued(a.name);
                DebuffModel c = new DebuffModel(cut(a.name, MAX_NAME), cut(a.description, MAX_DESCRIPTION), 0,
                        valued ? value : 0);
                c.setAddedBySpellId(spellId);
                if (valued && a.lockMin > 0) c.lock(a.lockMin, 0, null, spellId);
                target.getDebuffList().add(c);
            }
        }
        encounter.notifyChanged();
        return spellId;
    }

    /**
     * Reads the duration field of Cast spell.
     *
     * @return 0 for empty (no timer), the rounds for 1 to {@link DebuffModel#MAX_DURATION}, or -1
     * when it is something else, so the dialog can say what fits.
     */
    public static int parseDuration(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return 0;
        if (!t.matches("\\d{1,9}")) return -1;
        int rounds = Integer.parseInt(t);
        return rounds >= 1 && rounds <= DebuffModel.MAX_DURATION ? rounds : -1;
    }

    /** @return The characters that can cast or be a target: everyone but spell cards. */
    public static List<CharacterModel> combatants(Encounter encounter) {
        List<CharacterModel> list = new ArrayList<>();
        for (CharacterModel c : encounter.getCharacters()) {
            if (!c.isSpellCard()) list.add(c);
        }
        return list;
    }

    /** @return The characters that have this spell, in turn order. */
    public static List<CharacterModel> targetsOf(Encounter encounter, String spellId) {
        List<CharacterModel> targets = new ArrayList<>();
        if (spellId == null) return targets;
        for (CharacterModel c : encounter.getCharacters()) {
            for (DebuffModel d : c.getDebuffList()) {
                if (spellId.equals(d.getSpellId())) {
                    targets.add(c);
                    break;
                }
            }
        }
        return targets;
    }

    /**
     * @return The names of the conditions this spell's cast added that are still on a target
     * (e.g. "Frightened, Fleeing"), or null when there are none.
     */
    public static String addedConditionNames(Encounter encounter, String spellId) {
        return addedNames(encounter.getCharacters(), spellId == null ? new HashSet<>() : singleton(spellId));
    }

    /** @return The name of the spell with this id as it is on its targets, or "" when it ended. */
    public static String spellName(Encounter encounter, String spellId) {
        if (spellId == null) return "";
        for (CharacterModel c : encounter.getCharacters()) {
            for (DebuffModel d : c.getDebuffList()) {
                if (spellId.equals(d.getSpellId())) return d.getName();
            }
        }
        return "";
    }

    /**
     * Ends a spell (it ran out, was dismissed, concentration lost): it comes off every target,
     * with the condition its cast added when {@code removeAdded}. A spell card that only kept this
     * spell going leaves the fight too.
     */
    public static void end(Encounter encounter, String spellId, boolean removeAdded) {
        if (spellId == null) return;
        List<String> casters = new ArrayList<>();
        for (CharacterModel c : encounter.getCharacters()) {
            List<DebuffModel> list = c.getDebuffList();
            for (int i = list.size() - 1; i >= 0; i--) {
                DebuffModel d = list.get(i);
                if (removeAdded && spellId.equals(d.getAddedBySpellId())) {
                    list.remove(i);
                    continue;
                }
                if (!spellId.equals(d.getSpellId())) continue;
                if (d.getCasterId() != null && !casters.contains(d.getCasterId())) casters.add(d.getCasterId());
                list.remove(i);
            }
        }
        List<CharacterModel> endedCards = new ArrayList<>();
        for (CharacterModel c : encounter.getCharacters()) {
            if (c.isSpellCard() && casters.contains(c.getStableId()) && !encounter.hasSpellsOf(c)) endedCards.add(c);
        }
        if (endedCards.isEmpty()) {
            encounter.notifyChanged();
        } else {
            encounter.removeAll(endedCards);
        }
    }

    /**
     * Takes these expired spell conditions off their characters, with the conditions the same
     * casts added when {@code removeAdded}. The spell's other targets are left alone.
     */
    public static void removeExpired(Encounter encounter, List<Encounter.Expired> expired, boolean removeAdded) {
        for (Encounter.Expired e : expired) {
            e.character.getDebuffList().remove(e.condition);
            String spellId = e.condition.getSpellId();
            if (removeAdded && spellId != null) {
                e.character.getDebuffList().removeIf(d -> spellId.equals(d.getAddedBySpellId()));
            }
        }
        encounter.notifyChanged();
    }

    /** @return The names of the conditions added next to these expired spells, or null. */
    public static String addedConditionNames(List<Encounter.Expired> expired) {
        List<CharacterModel> characters = new ArrayList<>();
        Set<String> spellIds = new HashSet<>();
        for (Encounter.Expired e : expired) {
            if (e.condition.getSpellId() == null) continue;
            characters.add(e.character);
            spellIds.add(e.condition.getSpellId());
        }
        return addedNames(characters, spellIds);
    }

    /** "Frightened, Fleeing": each name once, in the order found; null when there is none. */
    private static String addedNames(List<CharacterModel> characters, Set<String> spellIds) {
        Set<String> names = new LinkedHashSet<>();
        for (CharacterModel c : characters) {
            for (DebuffModel d : c.getDebuffList()) {
                if (d.getAddedBySpellId() != null && spellIds.contains(d.getAddedBySpellId())) names.add(d.getName());
            }
        }
        return names.isEmpty() ? null : String.join(", ", names);
    }

    private static Set<String> singleton(String value) {
        Set<String> set = new HashSet<>();
        set.add(value);
        return set;
    }

    /** @return The spell id of the conditions a spell card keeps going, or null. */
    public static String spellOfCard(Encounter encounter, CharacterModel card) {
        String id = card.getStableId();
        for (CharacterModel c : encounter.getCharacters()) {
            for (DebuffModel d : c.getDebuffList()) {
                if (id.equals(d.getCasterId())) return d.getSpellId();
            }
        }
        return null;
    }

    /** @return The most rounds left on any target of what this spell card keeps going; 0 for none. */
    public static int roundsLeft(Encounter encounter, CharacterModel card) {
        String id = card.getStableId();
        int rounds = 0;
        for (CharacterModel c : encounter.getCharacters()) {
            for (DebuffModel d : c.getDebuffList()) {
                if (id.equals(d.getCasterId())) rounds = Math.max(rounds, d.getDuration());
            }
        }
        return rounds;
    }

    private static String cut(String value, int max) {
        if (value.length() <= max) return value;
        // Don't cut a surrogate pair (emoji) in half
        int end = Character.isHighSurrogate(value.charAt(max - 1)) ? max - 1 : max;
        return value.substring(0, end);
    }
}
