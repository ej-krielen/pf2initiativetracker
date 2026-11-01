package com.rekijan.initiativetrackersecondedition.feature.encounter;

import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The initiative order and round counter of the current fight. The character at the top of the
 * list is the one whose turn it is. One character is marked "first in round"; when the turn comes
 * back to them a new round starts. Plain Java (no Android), so the turn rules are unit tested.
 * <p>
 * Changes to the order, the list or the round are passed to the {@link Listener}s, so open
 * screens can redraw.
 * <p>
 * Spells: a condition with a caster counts down at the start of its caster's turn, on every
 * target (Player Core, Turns: an effect lasting a number of rounds loses one at the start of its
 * creator's turn). When a caster leaves the fight while their spells still hold targets, each
 * spell gets a spell card in the caster's place, so it keeps counting on the same initiative; the
 * screen then asks which of them go on ({@link #takeCastersGone}). A spell card goes away once
 * its spell ended.
 * <p>
 * Locks on valued conditions: rounds of a lock counted on another character's turn go down at
 * the start of that turn, and a lock that lasts as long as a spell opens when the spell ends.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 21-6-2026
 */
public class Encounter {

    /** Told after the order, the list or the round changed. */
    public interface Listener {
        void onEncounterChanged();
    }

    /** What happened when the turn moved on, so the app can ask the matching questions. */
    public static final class Turn {
        /** The character whose turn just ended. */
        public final CharacterModel ended;
        /** Persistent damage the ended character takes now (none when they are already dead). */
        public final List<DebuffModel> persistentDamage;
        /** (De)buffs of the ended character whose duration just ran out. */
        public final List<DebuffModel> expiredDebuffs;
        /** The character whose turn starts now. */
        public final CharacterModel active;
        /** Spells the active character cast that ran out at the start of this turn, on each target. */
        public final List<Expired> expiredSpells;
        /** True when the active character is dying and has to attempt a recovery check. */
        public final boolean recoveryCheck;
        public final boolean newRound;

        Turn(CharacterModel ended, List<DebuffModel> persistentDamage, List<DebuffModel> expiredDebuffs,
             CharacterModel active, List<Expired> expiredSpells, boolean recoveryCheck, boolean newRound) {
            this.ended = ended;
            this.persistentDamage = persistentDamage;
            this.expiredDebuffs = expiredDebuffs;
            this.active = active;
            this.expiredSpells = expiredSpells;
            this.recoveryCheck = recoveryCheck;
            this.newRound = newRound;
        }

        /**
         * @return What the user has to resolve, in order: first everything of the turn that
         * ended (persistent damage, then expired (de)buffs), then the start of the new turn
         * (expired spells, one step per spell for all its targets, fast healing, regeneration,
         * recovery check).
         */
        public List<TurnStep> steps() {
            List<TurnStep> steps = new ArrayList<>();
            for (DebuffModel d : persistentDamage) {
                steps.add(new TurnStep(TurnStep.Kind.PERSISTENT_DAMAGE, ended, d));
            }
            for (DebuffModel d : expiredDebuffs) {
                steps.add(new TurnStep(TurnStep.Kind.EXPIRED_DEBUFF, ended, d));
            }
            Map<String, List<Expired>> spells = new LinkedHashMap<>();
            for (Expired e : expiredSpells) {
                String key = e.condition.getSpellId() != null ? e.condition.getSpellId() : "name:" + e.condition.getName();
                List<Expired> group = spells.get(key);
                if (group == null) spells.put(key, group = new ArrayList<>());
                group.add(e);
            }
            for (List<Expired> group : spells.values()) {
                steps.add(new TurnStep(TurnStep.Kind.EXPIRED_SPELL, active, null, group));
            }
            if (active.getFastHealing() > 0) steps.add(new TurnStep(TurnStep.Kind.FAST_HEALING, active, null));
            if (active.getRegeneration() > 0) steps.add(new TurnStep(TurnStep.Kind.REGENERATION, active, null));
            if (recoveryCheck) steps.add(new TurnStep(TurnStep.Kind.RECOVERY_CHECK, active, null));
            return steps;
        }
    }

    /** A condition that reached 0, and who has it. */
    public static final class Expired {
        public final CharacterModel character;
        public final DebuffModel condition;

        Expired(CharacterModel character, DebuffModel condition) {
            this.character = character;
            this.condition = condition;
        }
    }

    /** A caster left the fight; their spells go on as these spell cards until the GM says otherwise. */
    public static final class CasterGone {
        public final String casterName;
        public final List<CharacterModel> spellCards;

        CasterGone(String casterName, List<CharacterModel> spellCards) {
            this.casterName = casterName;
            this.spellCards = spellCards;
        }
    }

    /** What happened when the PCs were healed to max hp. */
    public static final class MaxHpResult {
        /** Names of the PCs with more hp than max (temporary hp), who were left alone. */
        public final List<String> skipped;
        /** True when at least one PC was healed, which also ends wounded. */
        public final boolean woundedReset;

        MaxHpResult(List<String> skipped, boolean woundedReset) {
            this.skipped = skipped;
            this.woundedReset = woundedReset;
        }
    }

    /**
     * High to low initiative. On a tie monsters go before PCs (Player Core), except PCs that win
     * initiative ties: they go before the monsters.
     */
    static final Comparator<CharacterModel> INITIATIVE_ORDER = (a, b) -> {
        int compare = Integer.compare(b.getInitiative(), a.getInitiative());
        return compare != 0 ? compare : Integer.compare(tieRank(a), tieRank(b));
    };

    /** @return The place on an initiative tie: PCs that win ties, then monsters, then other PCs. */
    private static int tieRank(CharacterModel character) {
        if (!character.isPC()) return 1;
        return character.winsInitiativeTies() ? 0 : 2;
    }

    private final List<CharacterModel> characters = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();
    private final List<CasterGone> castersGone = new ArrayList<>();
    private int round;

    public Encounter(List<CharacterModel> characters, int round) {
        this.characters.addAll(characters);
        this.round = Math.max(0, round);
    }

    public List<CharacterModel> getCharacters() {
        return Collections.unmodifiableList(characters);
    }

    public int size() {
        return characters.size();
    }

    public CharacterModel get(int position) {
        return characters.get(position);
    }

    public int indexOf(CharacterModel character) {
        return characters.indexOf(character);
    }

    public boolean isValidPosition(int position) {
        return position >= 0 && position < characters.size();
    }

    public void add(CharacterModel character) {
        characters.add(character);
        notifyChanged();
    }

    /**
     * Removes a character. When it was first in round, the next character in the order takes
     * that over, so the round counter keeps going up at the right moment. See {@link #removeAll}
     * for casters and spell cards.
     */
    public void remove(int position) {
        if (!isValidPosition(position)) return;
        removeAll(Collections.singletonList(characters.get(position)));
    }

    public void remove(CharacterModel character) {
        remove(characters.indexOf(character));
    }

    /**
     * Removes several characters (Delete characters in the drawer), with one change notice. When
     * the one first in round leaves, the next character that stays takes it over.
     * <p>
     * A caster whose spells still hold targets that stay is replaced by one spell card per spell,
     * on the same spot (first in round included), and reported by {@link #takeCastersGone}. A
     * spell card that is removed ends its spell on every target.
     */
    public void removeAll(Collection<CharacterModel> toRemove) {
        List<CharacterModel> leaving = new ArrayList<>();
        for (CharacterModel c : toRemove) {
            if (characters.contains(c) && !leaving.contains(c)) leaving.add(c);
        }
        if (leaving.isEmpty()) return;
        for (CharacterModel c : leaving) {
            if (c.isSpellCard()) removeSpellsOf(c.getStableId());
        }
        List<CharacterModel> gone = new ArrayList<>();
        for (CharacterModel c : leaving) {
            List<CharacterModel> cards = c.isSpellCard() ? Collections.emptyList() : spellCardsFor(c, leaving);
            if (cards.isEmpty()) {
                gone.add(c);
                continue;
            }
            int position = characters.indexOf(c);
            characters.remove(position);
            characters.addAll(position, cards);
            cards.get(0).setFirstInRound(c.isFirstInRound());
            castersGone.add(new CasterGone(c.getCharacterName(), cards));
        }
        removeKeepingFirstInRound(gone);
        notifyChanged();
    }

    /**
     * @return The casters that left the fight since the last call, with the spell cards that
     * took their place; the list is emptied, so each one is asked about once.
     */
    public List<CasterGone> takeCastersGone() {
        List<CasterGone> taken = new ArrayList<>(castersGone);
        castersGone.clear();
        return taken;
    }

    /** @return True when a condition in the fight was cast by this character (or spell card). */
    public boolean hasSpellsOf(CharacterModel caster) {
        String id = caster.getStableId();
        for (CharacterModel c : characters) {
            for (DebuffModel d : c.getDebuffList()) {
                if (id.equals(d.getCasterId())) return true;
            }
        }
        return false;
    }

    /** Unlocks the conditions whose lock lasted as long as a spell that is no longer on anyone. */
    private void releaseEndedSpellLocks() {
        Set<String> spells = new HashSet<>();
        for (CharacterModel c : characters) {
            for (DebuffModel d : c.getDebuffList()) {
                if (d.getSpellId() != null) spells.add(d.getSpellId());
            }
        }
        for (CharacterModel c : characters) {
            for (DebuffModel d : c.getDebuffList()) {
                if (d.getLockSpellId() != null && !spells.contains(d.getLockSpellId())) d.unlock();
            }
        }
    }

    /** Removes these characters; when the one first in round leaves, the next that stays takes it over. */
    private void removeKeepingFirstInRound(List<CharacterModel> toRemove) {
        if (toRemove.isEmpty()) return;
        int size = characters.size();
        int first = -1;
        for (int i = 0; i < size; i++) {
            if (characters.get(i).isFirstInRound()) first = i;
        }
        boolean firstLeaves = first >= 0 && toRemove.contains(characters.get(first));
        CharacterModel successor = null;
        if (firstLeaves) {
            for (int step = 1; step < size && successor == null; step++) {
                CharacterModel next = characters.get((first + step) % size);
                if (!toRemove.contains(next)) successor = next;
            }
        }
        characters.removeAll(toRemove);
        if (successor != null) successor.setFirstInRound(true);
    }

    /**
     * One spell card per spell the caster cast on characters that stay, with the caster's
     * initiative and the spell's name. Those conditions now count on the card's turn.
     */
    private List<CharacterModel> spellCardsFor(CharacterModel caster, List<CharacterModel> leaving) {
        String id = caster.getStableId();
        Map<String, CharacterModel> cards = new LinkedHashMap<>();
        for (CharacterModel c : characters) {
            if (leaving.contains(c)) continue;
            for (DebuffModel d : c.getDebuffList()) {
                if (!id.equals(d.getCasterId())) continue;
                String key = d.getSpellId() != null ? d.getSpellId() : "name:" + d.getName();
                CharacterModel card = cards.get(key);
                if (card == null) {
                    card = new CharacterModel();
                    card.setCharacterName(d.getName());
                    card.setInitiative(caster.getInitiative());
                    card.setSpellCardCaster(caster.getCharacterName());
                    cards.put(key, card);
                }
                d.setCasterId(card.getStableId());
            }
        }
        return new ArrayList<>(cards.values());
    }

    /** Takes every condition cast by this id off every character. */
    private void removeSpellsOf(String casterId) {
        for (CharacterModel c : characters) {
            c.getDebuffList().removeIf(d -> casterId.equals(d.getCasterId()));
        }
    }

    /**
     * Spell cards whose spells all ended go away. Not the one at the top: it is having its turn,
     * so it goes when the turn moves on ({@link #nextTurn}).
     */
    private void removeEndedSpellCards() {
        List<CharacterModel> ended = new ArrayList<>();
        for (int i = 1; i < characters.size(); i++) {
            CharacterModel c = characters.get(i);
            if (c.isSpellCard() && !hasSpellsOf(c)) ended.add(c);
        }
        removeKeepingFirstInRound(ended);
    }

    /**
     * Puts another party's characters and round counter in this fight, used when switching
     * parties. Open screens keep the same Encounter and are told to redraw.
     */
    public void replace(List<CharacterModel> newCharacters, int newRound) {
        characters.clear();
        characters.addAll(newCharacters);
        castersGone.clear();
        round = Math.max(0, newRound);
        notifyChanged();
    }

    /**
     * Sorts high to low on initiative (ties: see {@link #INITIATIVE_ORDER}) and makes the top
     * character first in round.
     *
     * @return True when two characters of the same side share an initiative, so the user can
     * check the order.
     */
    public boolean sortByInitiative() {
        characters.sort(INITIATIVE_ORDER);
        if (!characters.isEmpty()) setFirstInRoundSilently(0);
        notifyChanged();
        return hasDuplicateInitiative();
    }

    /**
     * @return True when two PCs, or two NPCs, have the same initiative. Spell cards don't count:
     * they share their caster's initiative.
     */
    public boolean hasDuplicateInitiative() {
        for (int i = 0; i < characters.size(); i++) {
            for (int j = i + 1; j < characters.size(); j++) {
                CharacterModel a = characters.get(i);
                CharacterModel b = characters.get(j);
                if (a.isSpellCard() || b.isSpellCard()) continue;
                if (a.getInitiative() == b.getInitiative() && a.isPC() == b.isPC()) return true;
            }
        }
        return false;
    }

    public void setFirstInRound(int position) {
        if (!isValidPosition(position)) return;
        setFirstInRoundSilently(position);
        notifyChanged();
    }

    /**
     * Moves the turn on. The top character ends their turn (persistent damage, frightened goes
     * down, durations tick) and goes to the bottom; the next one starts their turn (reactions
     * come back, and the spells they cast lose a round on every target). The round goes up when
     * the new active character is first in round.
     * <p>
     * A spell whose caster isn't in the fight counts down at the end of its own character's turn,
     * so it still ends. A spell card at the top whose spells all ended leaves instead of going to
     * the bottom.
     *
     * @return What happened, or null when there are fewer than two characters.
     */
    public Turn nextTurn() {
        if (characters.size() < 2) return null;
        CharacterModel ended = characters.get(0);
        Set<String> present = new HashSet<>();
        for (CharacterModel c : characters) present.add(c.getStableId());
        List<DebuffModel> persistentDamage;
        List<DebuffModel> expired;
        boolean roundStarted = false;
        if (ended.isSpellCard() && !hasSpellsOf(ended)) {
            characters.remove(0);
            persistentDamage = new ArrayList<>();
            expired = new ArrayList<>();
            // Its round already started on its turn; the next one is first in round from now on
            roundStarted = ended.isFirstInRound();
            if (roundStarted) characters.get(0).setFirstInRound(true);
        } else {
            persistentDamage = ended.isAlreadyDead() ? new ArrayList<>() : ended.getPersistentDamage();
            expired = ended.endTurn(present);
            Collections.rotate(characters, -1);
        }

        CharacterModel active = characters.get(0);
        active.startTurn();
        List<Expired> expiredSpells = new ArrayList<>();
        String activeId = active.getStableId();
        for (CharacterModel c : characters) {
            for (DebuffModel d : c.getDebuffList()) {
                if (d.getDuration() > 0 && activeId.equals(d.getCasterId()) && d.tick()) {
                    expiredSpells.add(new Expired(c, d));
                }
                if (activeId.equals(d.getLockCounterId())) d.tickLock();
            }
        }
        boolean recoveryCheck = active.isDying() && !active.isAlreadyDead();
        boolean newRound = active.isFirstInRound() && !roundStarted;
        if (newRound) round++;
        notifyChanged();
        return new Turn(ended, persistentDamage, expired, active, expiredSpells, recoveryCheck, newRound);
    }

    /**
     * Knocks a character out: they get dying 1 (2 on a critical) plus their wounded value, and
     * move to the bottom of the order, which is directly before the character whose turn it is.
     * When they were first in round, the next character takes that over.
     */
    public void knockOut(CharacterModel character, int dying) {
        int position = characters.indexOf(character);
        if (position == -1) return;
        if (character.isFirstInRound() && position != characters.size() - 1) {
            character.setFirstInRound(false);
            characters.get(position + 1).setFirstInRound(true);
        }
        character.knockOut(dying);
        characters.remove(position);
        characters.add(character);
        notifyChanged();
    }

    /** Moves a character to another place in the order (drag and drop). */
    public void move(int from, int to) {
        if (!isValidPosition(from) || !isValidPosition(to) || from == to) return;
        characters.add(to, characters.remove(from));
        notifyChanged();
    }

    /**
     * Heals every PC to max hp, which also ends wounded. A PC with more hp than max (temporary
     * hp) is left alone.
     */
    public MaxHpResult setPcsToMaxHp() {
        List<String> skipped = new ArrayList<>();
        boolean woundedReset = false;
        for (CharacterModel c : characters) {
            if (!c.isPC()) continue;
            if (c.getMaxHp() >= c.getHp()) {
                c.setHp(c.getMaxHp());
                c.setWoundedValue(0);
                woundedReset = true;
            } else {
                skipped.add(c.getCharacterName());
            }
        }
        notifyChanged();
        return new MaxHpResult(skipped, woundedReset);
    }

    public int getRound() {
        return round;
    }

    public void nextRound() {
        round++;
        notifyChanged();
    }

    public void previousRound() {
        round = Math.max(0, round - 1);
        notifyChanged();
    }

    public void resetRound() {
        round = 1;
        notifyChanged();
    }

    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Tells the listeners; call this after changing a character from outside. Spell cards whose
     * spell just ended are removed first, and locks that lasted as long as an ended spell open.
     */
    public void notifyChanged() {
        removeEndedSpellCards();
        releaseEndedSpellLocks();
        for (Listener l : new ArrayList<>(listeners)) {
            l.onEncounterChanged();
        }
    }

    private void setFirstInRoundSilently(int position) {
        for (CharacterModel c : characters) {
            c.setFirstInRound(false);
        }
        characters.get(position).setFirstInRound(true);
    }
}
