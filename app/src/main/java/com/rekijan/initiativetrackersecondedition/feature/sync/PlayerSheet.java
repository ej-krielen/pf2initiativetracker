package com.rekijan.initiativetrackersecondedition.feature.sync;

import com.rekijan.initiativetrackersecondedition.core.util.NumberInput;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.character.ReactionModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything Table sync shares about one player character, in both directions: name, initiative
 * and initiative bonus, hp and max hp, fast healing and regeneration, hero points, the dying track
 * (dying on/off, dying, max dying, wounded, doomed, recovery DC), the free-text lines (AC, saves,
 * skills, attack routine, notes), the reaction and the custom reactions, and the conditions with
 * their value, duration and persistent damage. The player sends it to ask the GM for changes; the
 * GM's copy ({@link GmCopy}) uses it to send the GM's version back. {@link #sentAt} goes up on
 * every send, so the GM's app can tell a new send from an old one.
 * <p>
 * Whatever comes in is cleaned up: numbers are kept inside the ranges of {@link CharacterModel}
 * and {@link DebuffModel}, texts are cut to the limits below and the lists have a maximum length,
 * the same limits the Firestore rules enforce. Plain Java, so the mapping and the limits are unit
 * tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-10-2026
 */
public class PlayerSheet {

    // Keep these in sync with firestore.rules and the maxLength of the name field
    public static final int MAX_NAME = 50;
    public static final int MAX_SHORT_TEXT = 200;
    public static final int MAX_LONG_TEXT = 1000;
    public static final int MAX_NOTES = 2000;
    public static final int MAX_CONDITIONS = 50;
    public static final int MAX_CONDITION_NAME = 100;
    public static final int MAX_CONDITION_DESCRIPTION = 500;
    public static final int MAX_DAMAGE_TEXT = 50;
    public static final int MAX_REACTIONS = 20;
    public static final int MAX_REACTION_NAME = 100;
    public static final int MAX_REACTION_DESCRIPTION = 500;

    // Firestore field names, also used in firestore.rules
    static final String KEY_NAME = "name";
    static final String KEY_INITIATIVE = "initiative";
    static final String KEY_INITIATIVE_BONUS = "initiativeBonus";
    static final String KEY_HP = "hp";
    static final String KEY_MAX_HP = "maxHp";
    static final String KEY_FAST_HEALING = "fastHealing";
    static final String KEY_REGENERATION = "regeneration";
    static final String KEY_HERO_POINTS = "heroPoints";
    static final String KEY_DYING = "dying";
    static final String KEY_DYING_VALUE = "dyingValue";
    static final String KEY_MAX_DYING = "maxDying";
    static final String KEY_WOUNDED = "wounded";
    static final String KEY_DOOMED = "doomed";
    static final String KEY_RECOVERY_DC = "recoveryDc";
    static final String KEY_AC = "ac";
    static final String KEY_SAVES = "saves";
    static final String KEY_SKILLS = "skills";
    static final String KEY_ATTACK_ROUTINE = "attackRoutine";
    static final String KEY_NOTES = "notes";
    static final String KEY_REACTION_AVAILABLE = "reactionAvailable";
    static final String KEY_REACTIONS = "reactions";
    static final String KEY_REACTION_NAME = "name";
    static final String KEY_REACTION_DESCRIPTION = "description";
    static final String KEY_REACTION_AVAILABLE_ITEM = "available";
    static final String KEY_CONDITIONS = "conditions";
    static final String KEY_CONDITION_NAME = "name";
    static final String KEY_CONDITION_DURATION = "duration";
    static final String KEY_CONDITION_DESCRIPTION = "description";
    static final String KEY_CONDITION_VALUE = "value";
    static final String KEY_CONDITION_PERSISTENT = "persistent";
    static final String KEY_CONDITION_DAMAGE_TYPE = "damageType";
    static final String KEY_CONDITION_DAMAGE = "damage";
    static final String KEY_CONDITION_DC = "dc";
    static final String KEY_SENT_AT = "sentAt";

    public String name = "";
    public int initiative;
    public int initiativeBonus;
    public int hp;
    public int maxHp;
    public int fastHealing;
    public int regeneration;
    public int heroPoints;
    public boolean dying;
    public int dyingValue;
    public int maxDying = CharacterModel.DEFAULT_MAX_DYING;
    public int wounded;
    public int doomed;
    public int recoveryDc = CharacterModel.DEFAULT_RECOVERY_DC;
    public String ac = "";
    public String saves = "";
    public String skills = "";
    public String attackRoutine = "";
    public String notes = "";
    public boolean reactionAvailable = true;
    public List<ReactionModel> reactions = new ArrayList<>();
    public List<DebuffModel> conditions = new ArrayList<>();
    /** When the player sent this, in ms; only compared with the same player's earlier sends. */
    public long sentAt;

    /** @return Everything Table sync shares about this character (sentAt still 0). */
    public static PlayerSheet of(CharacterModel character) {
        PlayerSheet s = new PlayerSheet();
        s.name = character.getCharacterName();
        s.initiative = character.getInitiative();
        s.initiativeBonus = character.getInitiativeBonus();
        s.hp = character.getHp();
        s.maxHp = character.getMaxHp();
        s.fastHealing = character.getFastHealing();
        s.regeneration = character.getRegeneration();
        s.heroPoints = character.getHeroPoints();
        s.dying = character.isDying();
        s.dyingValue = character.getDyingValue();
        s.maxDying = character.getMaxDyingValue();
        s.wounded = character.getWoundedValue();
        s.doomed = character.getDoomedValue();
        s.recoveryDc = character.getRecoveryDC();
        s.ac = character.getAc();
        s.saves = character.getSaves();
        s.skills = character.getSkills();
        s.attackRoutine = character.getAttackRoutine();
        s.notes = character.getCharacterNotes();
        s.reactionAvailable = character.isReactionAvailable();
        s.reactions = new ArrayList<>(character.getReactionList());
        s.conditions = new ArrayList<>(character.getDebuffList());
        return s.cleaned();
    }

    /** @return True when there is a name; a player can't send a nameless character. */
    public boolean hasName() {
        return name != null && !name.trim().isEmpty();
    }

    /** @return A copy with every number in range, every text trimmed and cut, and the lists copied. */
    public PlayerSheet cleaned() {
        PlayerSheet s = new PlayerSheet();
        s.name = cut(name, MAX_NAME);
        s.initiative = NumberInput.clamp(initiative, CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE);
        s.initiativeBonus = NumberInput.clamp(initiativeBonus, CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE);
        s.hp = NumberInput.clamp(hp, CharacterModel.MIN_HP, CharacterModel.MAX_HP);
        s.maxHp = NumberInput.clamp(maxHp, 0, CharacterModel.MAX_HP);
        s.fastHealing = NumberInput.clamp(fastHealing, 0, CharacterModel.MAX_HEALING);
        s.regeneration = NumberInput.clamp(regeneration, 0, CharacterModel.MAX_HEALING);
        s.heroPoints = NumberInput.clamp(heroPoints, 0, CharacterModel.MAX_HERO_POINTS);
        s.dying = dying;
        s.dyingValue = NumberInput.clamp(dyingValue, 0, CharacterModel.MAX_CONDITION_VALUE);
        s.maxDying = NumberInput.clamp(maxDying, 1, CharacterModel.MAX_DYING_VALUE);
        s.wounded = NumberInput.clamp(wounded, 0, CharacterModel.MAX_CONDITION_VALUE);
        s.doomed = NumberInput.clamp(doomed, 0, CharacterModel.MAX_CONDITION_VALUE);
        s.recoveryDc = NumberInput.clamp(recoveryDc, 0, CharacterModel.MAX_RECOVERY_DC);
        s.ac = cut(ac, MAX_SHORT_TEXT);
        s.saves = cut(saves, MAX_SHORT_TEXT);
        s.skills = cut(skills, MAX_LONG_TEXT);
        s.attackRoutine = cut(attackRoutine, MAX_LONG_TEXT);
        s.notes = cut(notes, MAX_NOTES);
        s.reactionAvailable = reactionAvailable;
        if (reactions != null) {
            for (ReactionModel r : reactions) {
                if (s.reactions.size() >= MAX_REACTIONS) break;
                if (r != null) s.reactions.add(cleanedReaction(r.getName(), r.getDescription(), r.isAvailable()));
            }
        }
        if (conditions != null) {
            for (DebuffModel d : conditions) {
                if (s.conditions.size() >= MAX_CONDITIONS) break;
                if (d != null) s.conditions.add(cleanedCondition(d));
            }
        }
        s.sentAt = Math.max(0, sentAt);
        return s;
    }

    /** @return The cleaned sheet as a Firestore document (numbers as Long), without the expiry. */
    public Map<String, Object> toMap() {
        PlayerSheet s = cleaned();
        Map<String, Object> map = new HashMap<>();
        map.put(KEY_NAME, s.name);
        map.put(KEY_INITIATIVE, (long) s.initiative);
        map.put(KEY_INITIATIVE_BONUS, (long) s.initiativeBonus);
        map.put(KEY_HP, (long) s.hp);
        map.put(KEY_MAX_HP, (long) s.maxHp);
        map.put(KEY_FAST_HEALING, (long) s.fastHealing);
        map.put(KEY_REGENERATION, (long) s.regeneration);
        map.put(KEY_HERO_POINTS, (long) s.heroPoints);
        map.put(KEY_DYING, s.dying);
        map.put(KEY_DYING_VALUE, (long) s.dyingValue);
        map.put(KEY_MAX_DYING, (long) s.maxDying);
        map.put(KEY_WOUNDED, (long) s.wounded);
        map.put(KEY_DOOMED, (long) s.doomed);
        map.put(KEY_RECOVERY_DC, (long) s.recoveryDc);
        map.put(KEY_AC, s.ac);
        map.put(KEY_SAVES, s.saves);
        map.put(KEY_SKILLS, s.skills);
        map.put(KEY_ATTACK_ROUTINE, s.attackRoutine);
        map.put(KEY_NOTES, s.notes);
        map.put(KEY_REACTION_AVAILABLE, s.reactionAvailable);
        List<Map<String, Object>> reactionList = new ArrayList<>();
        for (ReactionModel r : s.reactions) {
            Map<String, Object> item = new HashMap<>();
            item.put(KEY_REACTION_NAME, r.getName());
            item.put(KEY_REACTION_DESCRIPTION, r.getDescription());
            item.put(KEY_REACTION_AVAILABLE_ITEM, r.isAvailable());
            reactionList.add(item);
        }
        map.put(KEY_REACTIONS, reactionList);
        List<Map<String, Object>> conditionList = new ArrayList<>();
        for (DebuffModel d : s.conditions) {
            Map<String, Object> item = new HashMap<>();
            item.put(KEY_CONDITION_NAME, d.getName());
            item.put(KEY_CONDITION_DURATION, (long) d.getDuration());
            item.put(KEY_CONDITION_DESCRIPTION, d.getDescription());
            item.put(KEY_CONDITION_VALUE, (long) d.getValue());
            item.put(KEY_CONDITION_PERSISTENT, d.isPersistentDamage());
            item.put(KEY_CONDITION_DAMAGE_TYPE, d.getDamageType());
            item.put(KEY_CONDITION_DAMAGE, d.getDamageValue());
            item.put(KEY_CONDITION_DC, (long) d.getDifficultyClass());
            conditionList.add(item);
        }
        map.put(KEY_CONDITIONS, conditionList);
        map.put(KEY_SENT_AT, s.sentAt);
        return map;
    }

    /**
     * Reads a player's send. Like {@link #read}, and a send without a name counts as no send.
     *
     * @return The cleaned sheet, or null when there is no map or no name.
     */
    public static PlayerSheet fromMap(Map<String, Object> map) {
        PlayerSheet s = read(map);
        return s != null && s.hasName() ? s : null;
    }

    /**
     * Reads a received document. Fields of the wrong type count as missing (max dying and the
     * recovery DC then get the PF2e defaults), numbers are clamped, texts cut and lists limited,
     * so a tampered document can't break a character.
     *
     * @return The cleaned sheet, or null when there is no map.
     */
    public static PlayerSheet read(Map<String, Object> map) {
        if (map == null) return null;
        PlayerSheet s = new PlayerSheet();
        s.name = text(map.get(KEY_NAME));
        s.initiative = whole(map.get(KEY_INITIATIVE), 0);
        s.initiativeBonus = whole(map.get(KEY_INITIATIVE_BONUS), 0);
        s.hp = whole(map.get(KEY_HP), 0);
        s.maxHp = whole(map.get(KEY_MAX_HP), 0);
        s.fastHealing = whole(map.get(KEY_FAST_HEALING), 0);
        s.regeneration = whole(map.get(KEY_REGENERATION), 0);
        s.heroPoints = whole(map.get(KEY_HERO_POINTS), 0);
        s.dying = Boolean.TRUE.equals(map.get(KEY_DYING));
        s.dyingValue = whole(map.get(KEY_DYING_VALUE), 0);
        s.maxDying = whole(map.get(KEY_MAX_DYING), CharacterModel.DEFAULT_MAX_DYING);
        s.wounded = whole(map.get(KEY_WOUNDED), 0);
        s.doomed = whole(map.get(KEY_DOOMED), 0);
        s.recoveryDc = whole(map.get(KEY_RECOVERY_DC), CharacterModel.DEFAULT_RECOVERY_DC);
        s.ac = text(map.get(KEY_AC));
        s.saves = text(map.get(KEY_SAVES));
        s.skills = text(map.get(KEY_SKILLS));
        s.attackRoutine = text(map.get(KEY_ATTACK_ROUTINE));
        s.notes = text(map.get(KEY_NOTES));
        s.reactionAvailable = !Boolean.FALSE.equals(map.get(KEY_REACTION_AVAILABLE));
        Object reactionList = map.get(KEY_REACTIONS);
        if (reactionList instanceof List) {
            for (Object item : (List<?>) reactionList) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) item;
                s.reactions.add(cleanedReaction(text(m.get(KEY_REACTION_NAME)),
                        text(m.get(KEY_REACTION_DESCRIPTION)), !Boolean.FALSE.equals(m.get(KEY_REACTION_AVAILABLE_ITEM))));
            }
        }
        Object conditionList = map.get(KEY_CONDITIONS);
        if (conditionList instanceof List) {
            for (Object item : (List<?>) conditionList) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) item;
                String name = text(m.get(KEY_CONDITION_NAME));
                String description = text(m.get(KEY_CONDITION_DESCRIPTION));
                int duration = whole(m.get(KEY_CONDITION_DURATION), 0);
                DebuffModel d = Boolean.TRUE.equals(m.get(KEY_CONDITION_PERSISTENT))
                        ? DebuffModel.persistentDamage(name, text(m.get(KEY_CONDITION_DAMAGE_TYPE)),
                        text(m.get(KEY_CONDITION_DAMAGE)),
                        whole(m.get(KEY_CONDITION_DC), DebuffModel.DEFAULT_PERSISTENT_DC), duration, description)
                        : new DebuffModel(name, description, duration, 0);
                d.setValue(whole(m.get(KEY_CONDITION_VALUE), 0));
                s.conditions.add(d);
            }
        }
        s.sentAt = number(map.get(KEY_SENT_AT));
        return s.cleaned();
    }

    /**
     * Puts this sheet on a character: every shared field, and the reactions and conditions are
     * replaced. Whether it is a PC, its turn order and its sync id stay as they are.
     */
    public void applyTo(CharacterModel character) {
        PlayerSheet s = cleaned();
        character.setCharacterName(s.name);
        character.setInitiative(s.initiative);
        character.setInitiativeBonus(s.initiativeBonus);
        character.setMaxHp(s.maxHp);
        character.setHp(s.hp);
        character.setFastHealing(s.fastHealing);
        character.setRegeneration(s.regeneration);
        character.setHeroPoints(s.heroPoints);
        character.setDying(s.dying);
        character.setDyingValue(s.dyingValue);
        character.setMaxDyingValue(s.maxDying);
        character.setWoundedValue(s.wounded);
        character.setDoomedValue(s.doomed);
        character.setRecoveryDC(s.recoveryDc);
        character.setAc(s.ac);
        character.setSaves(s.saves);
        character.setSkills(s.skills);
        character.setAttackRoutine(s.attackRoutine);
        character.setCharacterNotes(s.notes);
        character.setReactionAvailable(s.reactionAvailable);
        character.getReactionList().clear();
        character.getReactionList().addAll(s.reactions);
        character.getDebuffList().clear();
        character.getDebuffList().addAll(s.conditions);
    }

    /** @return A cleaned copy of the condition, persistent damage included. */
    static DebuffModel cleanedCondition(DebuffModel d) {
        String name = cutRaw(d.getName(), MAX_CONDITION_NAME);
        String description = cutRaw(d.getDescription(), MAX_CONDITION_DESCRIPTION);
        DebuffModel c = d.isPersistentDamage()
                ? DebuffModel.persistentDamage(name, cutRaw(d.getDamageType(), MAX_DAMAGE_TEXT),
                cutRaw(d.getDamageValue(), MAX_DAMAGE_TEXT), d.getDifficultyClass(), d.getDuration(), description)
                : new DebuffModel(name, description, d.getDuration(), 0);
        c.setValue(d.getValue());
        return c;
    }

    /** @return A cleaned reaction. */
    static ReactionModel cleanedReaction(String name, String description, boolean available) {
        ReactionModel r = new ReactionModel();
        r.setName(cutRaw(name, MAX_REACTION_NAME));
        r.setDescription(cutRaw(description, MAX_REACTION_DESCRIPTION));
        r.setAvailable(available);
        return r;
    }

    private static String cut(String value, int max) {
        return cutRaw(value == null ? "" : value.trim(), max);
    }

    /** Cuts without trimming (a condition or reaction being typed may end in a space). */
    private static String cutRaw(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        // Don't cut a surrogate pair (emoji) in half
        int end = Character.isHighSurrogate(value.charAt(max - 1)) ? max - 1 : max;
        return value.substring(0, end);
    }

    private static String text(Object value) {
        return value instanceof String ? (String) value : "";
    }

    /** @return The number as an int, kept inside the int range, or the fallback when it isn't a number. */
    private static int whole(Object value, int fallback) {
        if (!(value instanceof Number)) return fallback;
        return NumberInput.clamp(number(value), Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static long number(Object value) {
        // NaN becomes 0, infinities become the long limits
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }
}
