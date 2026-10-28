package com.rekijan.initiativetrackersecondedition.feature.sync;

import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.character.ReactionModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The differences between two {@link PlayerSheet}s, as a list of {@link Change}s: one per field
 * (HP 20 → 30, Hero points 1 → 0), one per custom reaction and one per condition that was added,
 * removed or changed (matched by name). The GM ticks the changes to accept in the approval popup
 * and {@link #apply} takes only those; the player's screen uses the same list to mark what is not
 * sent yet. Plain Java, so it is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-10-2026
 */
public final class SyncDiff {

    /** The fields of a sheet, in the order they are listed. */
    public enum Field {
        NAME, INITIATIVE, INITIATIVE_BONUS, HP, MAX_HP, FAST_HEALING, REGENERATION, HERO_POINTS,
        DYING, DYING_VALUE, MAX_DYING, WOUNDED, DOOMED, RECOVERY_DC,
        AC, SAVES, SKILLS, ATTACK_ROUTINE, NOTES, REACTION, REACTIONS, CONDITIONS;

        /** @return True for the on/off fields (dying, the reaction), whose values are "true" and "false". */
        public boolean isSwitch() {
            return this == DYING || this == REACTION;
        }
    }

    /** What kind of change. */
    public enum Kind {
        /** A field other than the lists has another value. */
        FIELD,
        CONDITION_ADDED,
        CONDITION_REMOVED,
        /** Same name, other value, rounds, description or damage. */
        CONDITION_CHANGED,
        REACTION_ADDED,
        REACTION_REMOVED,
        /** Same name, other description or used. */
        REACTION_CHANGED
    }

    /** One change from one sheet to the other. */
    public static final class Change {
        public final Kind kind;
        public final Field field;
        /** For a field: the old and new value as text. For a condition or reaction: its old name, and "". */
        public final String from;
        public final String to;
        /** For a condition change: the condition as it is after the change (null otherwise or when removed). */
        public final DebuffModel condition;
        /** For a reaction change: the reaction as it is after the change (null otherwise or when removed). */
        public final ReactionModel reaction;
        /** Unique within one list; identifies the change when the GM ticks it. */
        public final String key;

        Change(Kind kind, Field field, String from, String to, DebuffModel condition, ReactionModel reaction,
               String key) {
            this.kind = kind;
            this.field = field;
            this.from = from;
            this.to = to;
            this.condition = condition;
            this.reaction = reaction;
            this.key = key;
        }
    }

    private SyncDiff() {
    }

    /** @return What changes from {@code from} to {@code to}; empty when they are the same. */
    public static List<Change> changes(PlayerSheet from, PlayerSheet to) {
        PlayerSheet a = from.cleaned();
        PlayerSheet b = to.cleaned();
        List<Change> list = new ArrayList<>();
        field(list, Field.NAME, a.name, b.name);
        field(list, Field.INITIATIVE, a.initiative, b.initiative);
        field(list, Field.INITIATIVE_BONUS, a.initiativeBonus, b.initiativeBonus);
        field(list, Field.HP, a.hp, b.hp);
        field(list, Field.MAX_HP, a.maxHp, b.maxHp);
        field(list, Field.FAST_HEALING, a.fastHealing, b.fastHealing);
        field(list, Field.REGENERATION, a.regeneration, b.regeneration);
        field(list, Field.HERO_POINTS, a.heroPoints, b.heroPoints);
        field(list, Field.DYING, a.dying, b.dying);
        field(list, Field.DYING_VALUE, a.dyingValue, b.dyingValue);
        field(list, Field.MAX_DYING, a.maxDying, b.maxDying);
        field(list, Field.WOUNDED, a.wounded, b.wounded);
        field(list, Field.DOOMED, a.doomed, b.doomed);
        field(list, Field.RECOVERY_DC, a.recoveryDc, b.recoveryDc);
        field(list, Field.AC, a.ac, b.ac);
        field(list, Field.SAVES, a.saves, b.saves);
        field(list, Field.SKILLS, a.skills, b.skills);
        field(list, Field.ATTACK_ROUTINE, a.attackRoutine, b.attackRoutine);
        field(list, Field.NOTES, a.notes, b.notes);
        field(list, Field.REACTION, a.reactionAvailable, b.reactionAvailable);
        reactions(list, a.reactions, b.reactions);
        conditions(list, a.conditions, b.conditions);
        return list;
    }

    /** @return The fields that differ; REACTIONS or CONDITIONS when any of those differs. */
    public static List<Field> changedFields(PlayerSheet from, PlayerSheet to) {
        List<Field> fields = new ArrayList<>();
        for (Change c : changes(from, to)) {
            if (!fields.contains(c.field)) fields.add(c.field);
        }
        return fields;
    }

    /**
     * @param target   The sheet the changes are made on (the GM's character).
     * @param source   The sheet the changes come from (the player's send).
     * @param accepted The changes from {@code changes(target, source)} to take.
     * @return A new sheet: the target with only the accepted changes made.
     */
    public static PlayerSheet apply(PlayerSheet target, PlayerSheet source, Collection<Change> accepted) {
        PlayerSheet r = target.cleaned();
        PlayerSheet s = source.cleaned();
        for (Change c : accepted) {
            switch (c.kind) {
                case FIELD:
                    copyField(r, s, c.field);
                    break;
                case CONDITION_ADDED:
                    r.conditions.add(PlayerSheet.cleanedCondition(c.condition));
                    break;
                case CONDITION_REMOVED: {
                    int i = conditionIndex(r.conditions, c.from);
                    if (i >= 0) r.conditions.remove(i);
                    break;
                }
                case CONDITION_CHANGED: {
                    int i = conditionIndex(r.conditions, c.from);
                    if (i >= 0) r.conditions.set(i, PlayerSheet.cleanedCondition(c.condition));
                    break;
                }
                case REACTION_ADDED:
                    r.reactions.add(copy(c.reaction));
                    break;
                case REACTION_REMOVED: {
                    int i = reactionIndex(r.reactions, c.from);
                    if (i >= 0) r.reactions.remove(i);
                    break;
                }
                case REACTION_CHANGED: {
                    int i = reactionIndex(r.reactions, c.from);
                    if (i >= 0) r.reactions.set(i, copy(c.reaction));
                    break;
                }
                default:
                    break;
            }
        }
        return r.cleaned();
    }

    /** @return True when the two lists hold exactly the same conditions, in the same order. */
    static boolean sameConditions(List<DebuffModel> a, List<DebuffModel> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!same(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    /** @return True when the two lists hold exactly the same reactions, in the same order. */
    static boolean sameReactions(List<ReactionModel> a, List<ReactionModel> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!same(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    // ---------------------------------------------------------------- Private

    private static void field(List<Change> list, Field field, Object a, Object b) {
        if (Objects.equals(a, b)) return;
        list.add(new Change(Kind.FIELD, field, String.valueOf(a), String.valueOf(b), null, null, field.name()));
    }

    /**
     * Pairs conditions by name (ignoring case), in order, so two conditions with the same name are
     * paired first with first. Unpaired ones in {@code b} are added, in {@code a} removed.
     */
    private static void conditions(List<Change> list, List<DebuffModel> a, List<DebuffModel> b) {
        boolean[] used = new boolean[a.size()];
        int n = 0;
        for (DebuffModel nb : b) {
            int match = -1;
            for (int i = 0; i < a.size(); i++) {
                if (!used[i] && key(a.get(i).getName()).equals(key(nb.getName()))) {
                    match = i;
                    break;
                }
            }
            if (match < 0) {
                list.add(new Change(Kind.CONDITION_ADDED, Field.CONDITIONS, nb.getName(), "",
                        PlayerSheet.cleanedCondition(nb), null, "C+" + (n++) + ":" + key(nb.getName())));
                continue;
            }
            used[match] = true;
            DebuffModel na = a.get(match);
            if (!same(na, nb)) {
                list.add(new Change(Kind.CONDITION_CHANGED, Field.CONDITIONS, na.getName(), "",
                        PlayerSheet.cleanedCondition(nb), null, "C~" + (n++) + ":" + key(nb.getName())));
            }
        }
        for (int i = 0; i < a.size(); i++) {
            if (!used[i]) {
                list.add(new Change(Kind.CONDITION_REMOVED, Field.CONDITIONS, a.get(i).getName(),
                        "", null, null, "C-" + (n++) + ":" + key(a.get(i).getName())));
            }
        }
    }

    /** Pairs custom reactions by name, like {@link #conditions}. */
    private static void reactions(List<Change> list, List<ReactionModel> a, List<ReactionModel> b) {
        boolean[] used = new boolean[a.size()];
        int n = 0;
        for (ReactionModel nb : b) {
            int match = -1;
            for (int i = 0; i < a.size(); i++) {
                if (!used[i] && key(a.get(i).getName()).equals(key(nb.getName()))) {
                    match = i;
                    break;
                }
            }
            if (match < 0) {
                list.add(new Change(Kind.REACTION_ADDED, Field.REACTIONS, nb.getName(), "",
                        null, copy(nb), "R+" + (n++) + ":" + key(nb.getName())));
                continue;
            }
            used[match] = true;
            ReactionModel na = a.get(match);
            if (!same(na, nb)) {
                list.add(new Change(Kind.REACTION_CHANGED, Field.REACTIONS, na.getName(), "",
                        null, copy(nb), "R~" + (n++) + ":" + key(nb.getName())));
            }
        }
        for (int i = 0; i < a.size(); i++) {
            if (!used[i]) {
                list.add(new Change(Kind.REACTION_REMOVED, Field.REACTIONS, a.get(i).getName(),
                        "", null, null, "R-" + (n++) + ":" + key(a.get(i).getName())));
            }
        }
    }

    private static void copyField(PlayerSheet r, PlayerSheet s, Field field) {
        switch (field) {
            case NAME: r.name = s.name; break;
            case INITIATIVE: r.initiative = s.initiative; break;
            case INITIATIVE_BONUS: r.initiativeBonus = s.initiativeBonus; break;
            case HP: r.hp = s.hp; break;
            case MAX_HP: r.maxHp = s.maxHp; break;
            case FAST_HEALING: r.fastHealing = s.fastHealing; break;
            case REGENERATION: r.regeneration = s.regeneration; break;
            case HERO_POINTS: r.heroPoints = s.heroPoints; break;
            case DYING: r.dying = s.dying; break;
            case DYING_VALUE: r.dyingValue = s.dyingValue; break;
            case MAX_DYING: r.maxDying = s.maxDying; break;
            case WOUNDED: r.wounded = s.wounded; break;
            case DOOMED: r.doomed = s.doomed; break;
            case RECOVERY_DC: r.recoveryDc = s.recoveryDc; break;
            case AC: r.ac = s.ac; break;
            case SAVES: r.saves = s.saves; break;
            case SKILLS: r.skills = s.skills; break;
            case ATTACK_ROUTINE: r.attackRoutine = s.attackRoutine; break;
            case NOTES: r.notes = s.notes; break;
            case REACTION: r.reactionAvailable = s.reactionAvailable; break;
            case REACTIONS: r.reactions = new ArrayList<>(s.reactions); break;
            case CONDITIONS: r.conditions = new ArrayList<>(s.conditions); break;
            default: break;
        }
    }

    private static int conditionIndex(List<DebuffModel> list, String name) {
        String k = key(name);
        for (int i = 0; i < list.size(); i++) {
            if (key(list.get(i).getName()).equals(k)) return i;
        }
        return -1;
    }

    private static int reactionIndex(List<ReactionModel> list, String name) {
        String k = key(name);
        for (int i = 0; i < list.size(); i++) {
            if (key(list.get(i).getName()).equals(k)) return i;
        }
        return -1;
    }

    private static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean same(DebuffModel a, DebuffModel b) {
        return Objects.equals(a.getName(), b.getName()) && a.getDuration() == b.getDuration()
                && Objects.equals(a.getDescription(), b.getDescription()) && a.getValue() == b.getValue()
                && a.isPersistentDamage() == b.isPersistentDamage()
                && Objects.equals(a.getDamageType(), b.getDamageType())
                && Objects.equals(a.getDamageValue(), b.getDamageValue())
                && a.getDifficultyClass() == b.getDifficultyClass();
    }

    private static boolean same(ReactionModel a, ReactionModel b) {
        return Objects.equals(a.getName(), b.getName()) && Objects.equals(a.getDescription(), b.getDescription())
                && a.isAvailable() == b.isAvailable();
    }

    private static ReactionModel copy(ReactionModel r) {
        return PlayerSheet.cleanedReaction(r.getName(), r.getDescription(), r.isAvailable());
    }
}
