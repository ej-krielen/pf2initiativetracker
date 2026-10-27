package com.rekijan.initiativetrackersecondedition.feature.encounter;

import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The initiative order and round counter of the current fight. The character at the top of the
 * list is the one whose turn it is. One character is marked "first in round"; when the turn comes
 * back to them a new round starts. Plain Java (no Android), so the turn rules are unit tested.
 * <p>
 * Changes to the order, the list or the round are passed to the {@link Listener}s, so open
 * screens can redraw.
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
        /** True when the active character is dying and has to attempt a recovery check. */
        public final boolean recoveryCheck;
        public final boolean newRound;

        Turn(CharacterModel ended, List<DebuffModel> persistentDamage, List<DebuffModel> expiredDebuffs,
             CharacterModel active, boolean recoveryCheck, boolean newRound) {
            this.ended = ended;
            this.persistentDamage = persistentDamage;
            this.expiredDebuffs = expiredDebuffs;
            this.active = active;
            this.recoveryCheck = recoveryCheck;
            this.newRound = newRound;
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

    /** High to low initiative; on a tie monsters go before PCs (Player Core). */
    static final Comparator<CharacterModel> INITIATIVE_ORDER = (a, b) -> {
        int compare = Integer.compare(b.getInitiative(), a.getInitiative());
        return compare != 0 ? compare : Boolean.compare(a.isPC(), b.isPC());
    };

    private final List<CharacterModel> characters = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();
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
     * that over, so the round counter keeps going up at the right moment.
     */
    public void remove(int position) {
        if (!isValidPosition(position)) return;
        CharacterModel removed = characters.remove(position);
        if (removed.isFirstInRound() && !characters.isEmpty()) {
            characters.get(position % characters.size()).setFirstInRound(true);
        }
        notifyChanged();
    }

    public void remove(CharacterModel character) {
        remove(characters.indexOf(character));
    }

    /**
     * Sorts high to low on initiative, monsters before PCs on a tie, and makes the top character
     * first in round.
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

    /** @return True when two PCs, or two NPCs, have the same initiative. */
    public boolean hasDuplicateInitiative() {
        for (int i = 0; i < characters.size(); i++) {
            for (int j = i + 1; j < characters.size(); j++) {
                CharacterModel a = characters.get(i);
                CharacterModel b = characters.get(j);
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
     * come back). The round goes up when the new active character is first in round.
     *
     * @return What happened, or null when there are fewer than two characters.
     */
    public Turn nextTurn() {
        if (characters.size() < 2) return null;
        CharacterModel ended = characters.get(0);
        List<DebuffModel> persistentDamage = ended.isAlreadyDead()
                ? new ArrayList<>() : ended.getPersistentDamage();
        List<DebuffModel> expired = ended.endTurn();

        Collections.rotate(characters, -1);
        CharacterModel active = characters.get(0);
        active.startTurn();
        boolean recoveryCheck = active.isDying() && !active.isAlreadyDead();
        boolean newRound = active.isFirstInRound();
        if (newRound) round++;
        notifyChanged();
        return new Turn(ended, persistentDamage, expired, active, recoveryCheck, newRound);
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

    /** Tells the listeners; call this after changing a character from outside. */
    public void notifyChanged() {
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
