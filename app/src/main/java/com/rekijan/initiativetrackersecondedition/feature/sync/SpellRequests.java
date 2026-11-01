package com.rekijan.initiativetrackersecondedition.feature.sync;

import com.rekijan.initiativetrackersecondedition.core.util.NumberInput;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;
import com.rekijan.initiativetrackersecondedition.feature.spell.Spells;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Spell casting through Table sync. A player can't pick targets (they never see the monsters),
 * so casting is a request: the player sends a {@link Cast} with a free-text note about the
 * targets, and the GM picks the real targets and casts it, or rejects it. A player can also ask
 * the GM to end one of their spells ({@link End}). Each request has its own time ({@code at}),
 * so the GM's app answers it once, apart from the player's sheet changes; the GM's copy says
 * which request was answered and how ({@link Answer}), and lists the spells the PC has going
 * ({@link Spell}): name, rounds left, how many targets and the conditions the cast added, never
 * who the targets are.
 * <p>
 * Whatever comes in is cleaned up and limited like {@link PlayerSheet}, with the limits the
 * Firestore rules enforce. Plain Java, so it is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 01-11-2026
 */
public final class SpellRequests {

    // Keep these in sync with firestore.rules
    public static final int MAX_NAME = Spells.MAX_NAME;
    public static final int MAX_TARGETS_NOTE = 200;
    public static final int MAX_SPELL_ID = 64;
    public static final int MAX_SPELLS = 20;
    public static final int MAX_ADDED = 200;

    // Firestore field names, also used in firestore.rules
    static final String KEY_CAST = "cast";
    static final String KEY_END = "endSpell";
    static final String KEY_SPELLS = "spells";
    static final String KEY_CAST_ANSWER = "castAnswer";
    static final String KEY_END_ANSWER = "endAnswer";
    static final String KEY_AT = "at";
    static final String KEY_NAME = "name";
    static final String KEY_ROUNDS = "rounds";
    static final String KEY_CONDITION = "condition";
    static final String KEY_VALUE = "value";
    static final String KEY_TARGETS = "targets";
    static final String KEY_SPELL_ID = "spellId";
    static final String KEY_ID = "id";
    static final String KEY_ADDED = "added";
    static final String KEY_OK = "ok";

    private SpellRequests() {
    }

    /** A player asks the GM to cast a spell. */
    public static final class Cast {
        /** When the player asked, in ms; tells this request from earlier ones. */
        public long at;
        public String name = "";
        /** Duration in rounds; 0 for no timer. */
        public int rounds;
        /** A condition from the condition list to add with it, or "" for none. */
        public String condition = "";
        /** The condition's value; 0 for a condition without one. */
        public int value;
        /** The player's note for the GM about the targets, e.g. "the two goblins in the burst". */
        public String targets = "";

        public Cast cleaned() {
            Cast c = new Cast();
            c.at = Math.max(0, at);
            c.name = cut(name, MAX_NAME);
            c.rounds = NumberInput.clamp(rounds, 0, DebuffModel.MAX_DURATION);
            c.condition = cut(condition, MAX_NAME);
            c.value = c.condition.isEmpty() ? 0 : NumberInput.clamp(value, 0, DebuffModel.MAX_VALUE);
            c.targets = cut(targets, MAX_TARGETS_NOTE);
            return c;
        }

        Map<String, Object> toMap() {
            Cast c = cleaned();
            Map<String, Object> map = new HashMap<>();
            map.put(KEY_AT, c.at);
            map.put(KEY_NAME, c.name);
            map.put(KEY_ROUNDS, (long) c.rounds);
            map.put(KEY_CONDITION, c.condition);
            map.put(KEY_VALUE, (long) c.value);
            map.put(KEY_TARGETS, c.targets);
            return map;
        }

        /** @return The cleaned request, or null when there is none or it has no name. */
        static Cast read(Object value) {
            if (!(value instanceof Map)) return null;
            Map<?, ?> m = (Map<?, ?>) value;
            Cast c = new Cast();
            c.at = number(m.get(KEY_AT));
            c.name = text(m.get(KEY_NAME));
            c.rounds = whole(m.get(KEY_ROUNDS));
            c.condition = text(m.get(KEY_CONDITION));
            c.value = whole(m.get(KEY_VALUE));
            c.targets = text(m.get(KEY_TARGETS));
            c = c.cleaned();
            return c.name.isEmpty() ? null : c;
        }
    }

    /** A player asks the GM to end one of their spells. */
    public static final class End {
        public long at;
        /** The GM's id of the spell, from the PC's {@link Spell} list. */
        public String spellId = "";
        /** The spell's name, so the GM's app can say which one when it is gone already. */
        public String name = "";

        public End cleaned() {
            End e = new End();
            e.at = Math.max(0, at);
            e.spellId = cut(spellId, MAX_SPELL_ID);
            e.name = cut(name, MAX_NAME);
            return e;
        }

        Map<String, Object> toMap() {
            End e = cleaned();
            Map<String, Object> map = new HashMap<>();
            map.put(KEY_AT, e.at);
            map.put(KEY_SPELL_ID, e.spellId);
            map.put(KEY_NAME, e.name);
            return map;
        }

        /** @return The cleaned request, or null when there is none or it names no spell. */
        static End read(Object value) {
            if (!(value instanceof Map)) return null;
            Map<?, ?> m = (Map<?, ?>) value;
            End e = new End();
            e.at = number(m.get(KEY_AT));
            e.spellId = text(m.get(KEY_SPELL_ID));
            e.name = text(m.get(KEY_NAME));
            e = e.cleaned();
            return e.spellId.isEmpty() ? null : e;
        }
    }

    /** A spell the PC has going, as the player sees it. */
    public static final class Spell {
        public String id = "";
        public String name = "";
        /** The most rounds left on any target; 0 for no timer. */
        public int rounds;
        /** How many characters have it. */
        public int targets;
        /** The conditions its cast added that are still on a target ("Frightened, Fleeing"), or "". */
        public String added = "";

        Spell cleaned() {
            Spell s = new Spell();
            s.id = cut(id, MAX_SPELL_ID);
            s.name = cut(name, MAX_NAME);
            s.rounds = NumberInput.clamp(rounds, 0, DebuffModel.MAX_DURATION);
            s.targets = NumberInput.clamp(targets, 0, 999);
            s.added = cut(added, MAX_ADDED);
            return s;
        }

        Map<String, Object> toMap() {
            Spell s = cleaned();
            Map<String, Object> map = new HashMap<>();
            map.put(KEY_ID, s.id);
            map.put(KEY_NAME, s.name);
            map.put(KEY_ROUNDS, (long) s.rounds);
            map.put(KEY_TARGETS, (long) s.targets);
            map.put(KEY_ADDED, s.added);
            return map;
        }
    }

    /** The GM's answer to a request: which one ({@code at}) and whether it was done. */
    public static final class Answer {
        public long at;
        public boolean ok;

        public Answer() {
        }

        public Answer(long at, boolean ok) {
            this.at = Math.max(0, at);
            this.ok = ok;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put(KEY_AT, Math.max(0, at));
            map.put(KEY_OK, ok);
            return map;
        }

        static Answer read(Object value) {
            if (!(value instanceof Map)) return new Answer();
            Map<?, ?> m = (Map<?, ?>) value;
            return new Answer(number(m.get(KEY_AT)), Boolean.TRUE.equals(m.get(KEY_OK)));
        }

        /** @return True when this answers the request asked at {@code requestAt}. */
        public boolean answers(long requestAt) {
            return at > 0 && at >= requestAt;
        }
    }

    /**
     * @return The spells this caster has going in the fight, in the order their first target
     * comes, at most {@link #MAX_SPELLS}.
     */
    public static List<Spell> spellsOf(Encounter encounter, CharacterModel caster) {
        String casterId = caster.getStableId();
        Map<String, Spell> spells = new LinkedHashMap<>();
        Map<String, Set<String>> added = new HashMap<>();
        for (CharacterModel c : encounter.getCharacters()) {
            for (DebuffModel d : c.getDebuffList()) {
                String spellId = d.getSpellId();
                if (spellId == null || !casterId.equals(d.getCasterId())) continue;
                Spell s = spells.get(spellId);
                if (s == null) {
                    if (spells.size() >= MAX_SPELLS) continue;
                    s = new Spell();
                    s.id = spellId;
                    s.name = d.getName();
                    spells.put(spellId, s);
                    added.put(spellId, new LinkedHashSet<>());
                }
                s.rounds = Math.max(s.rounds, d.getDuration());
                s.targets++;
            }
        }
        for (CharacterModel c : encounter.getCharacters()) {
            for (DebuffModel d : c.getDebuffList()) {
                Set<String> names = d.getAddedBySpellId() == null ? null : added.get(d.getAddedBySpellId());
                if (names != null) names.add(d.getValue() > 0 ? d.getName() + " " + d.getValue() : d.getName());
            }
        }
        List<Spell> list = new ArrayList<>();
        for (Spell s : spells.values()) {
            s.added = String.join(", ", added.get(s.id));
            list.add(s.cleaned());
        }
        return list;
    }

    /** @return The spells as a Firestore list. */
    static List<Map<String, Object>> spellsToList(List<Spell> spells) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Spell s : spells) {
            if (list.size() >= MAX_SPELLS) break;
            list.add(s.toMap());
        }
        return list;
    }

    /** @return The cleaned spells from a Firestore list; empty when there are none. */
    static List<Spell> readSpells(Object value) {
        List<Spell> list = new ArrayList<>();
        if (!(value instanceof List)) return list;
        for (Object item : (List<?>) value) {
            if (list.size() >= MAX_SPELLS) break;
            if (!(item instanceof Map)) continue;
            Map<?, ?> m = (Map<?, ?>) item;
            Spell s = new Spell();
            s.id = text(m.get(KEY_ID));
            s.name = text(m.get(KEY_NAME));
            s.rounds = whole(m.get(KEY_ROUNDS));
            s.targets = whole(m.get(KEY_TARGETS));
            s.added = text(m.get(KEY_ADDED));
            s = s.cleaned();
            if (!s.id.isEmpty()) list.add(s);
        }
        return list;
    }

    /** @return True when a spell with this id is in the list. */
    public static boolean contains(List<Spell> spells, String spellId) {
        for (Spell s : spells) {
            if (s.id.equals(spellId)) return true;
        }
        return false;
    }

    private static String cut(String value, int max) {
        String v = value == null ? "" : value.trim();
        if (v.length() <= max) return v;
        // Don't cut a surrogate pair (emoji) in half
        int end = Character.isHighSurrogate(v.charAt(max - 1)) ? max - 1 : max;
        return v.substring(0, end);
    }

    private static String text(Object value) {
        return value instanceof String ? (String) value : "";
    }

    private static int whole(Object value) {
        return NumberInput.clamp(number(value), Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }
}
