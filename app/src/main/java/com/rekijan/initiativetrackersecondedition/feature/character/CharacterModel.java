package com.rekijan.initiativetrackersecondedition.feature.character;

import com.rekijan.initiativetrackersecondedition.core.util.NumberInput;
import com.rekijan.initiativetrackersecondedition.feature.condition.Conditions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * One combatant in the initiative order: a PC or an NPC/monster with initiative, hit points,
 * healing per turn, free-text stat lines, (de)buffs, reactions, hero points and the Pathfinder 2e
 * dying track (dying, wounded, doomed). Plain Java (no Android), so the rules are unit tested.
 * <p>
 * The field names are part of the JSON that 1.x saved in SharedPreferences. They must stay the
 * same so users keep their characters after the update.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 11-10-2015
 */
public class CharacterModel {

    public static final int MIN_INITIATIVE = -99;
    public static final int MAX_INITIATIVE = 999;
    public static final int MIN_HP = -99999;
    public static final int MAX_HP = 99999;
    public static final int MAX_HEALING = 999;
    /** Dying 4 means death, Player Core pg. 443. */
    public static final int DEFAULT_MAX_DYING = 4;
    /** Highest max dying value the user can set, for effects that raise it. */
    public static final int MAX_DYING_VALUE = 6;
    public static final int MAX_CONDITION_VALUE = 99;
    /** A recovery check is a flat check against DC 10 + the dying value. */
    public static final int DEFAULT_RECOVERY_DC = 10;
    public static final int MAX_RECOVERY_DC = 99;
    public static final int MAX_HERO_POINTS = 99;

    /** The outcome of a recovery check (Player Core, Recovery Checks). */
    public enum RecoveryResult {
        CRITICAL_SUCCESS(-2), SUCCESS(-1), FAILURE(1), CRITICAL_FAILURE(2);

        final int dyingChange;

        RecoveryResult(int dyingChange) {
            this.dyingChange = dyingChange;
        }
    }

    private int initiative;
    private int initiativeBonus;
    private String skills;
    private String attackRoutine;
    private String ac;
    private String saves;
    private int hp;
    private int maxHp;
    private boolean isFirstInRound;
    private boolean isPC;
    private int fastHealing;
    private int regeneration;
    private boolean isDying;
    private int dyingValue;
    private int maxDyingValue;
    private int woundedValue;
    private int doomedValue;
    private int recoveryDC;
    private int heroPoints;
    private boolean reactionAvailable;
    private boolean reactionCollapsed;
    private String characterName;
    private String characterNotes;
    private ArrayList<DebuffModel> debuffList = new ArrayList<>();
    private ArrayList<ReactionModel> reactionsList = new ArrayList<>();

    /** Empty character; also used by Gson when loading saved data. */
    public CharacterModel() {
        skills = "";
        attackRoutine = "";
        ac = "";
        saves = "";
        characterName = "";
        characterNotes = "";
        maxDyingValue = DEFAULT_MAX_DYING;
        recoveryDC = DEFAULT_RECOVERY_DC;
        heroPoints = 1;
        reactionAvailable = true;
    }

    // region Turn rules

    /** Start of this character's turn: their reactions are available again. */
    public void startTurn() {
        reactionAvailable = true;
        for (ReactionModel r : getReactionList()) {
            r.setAvailable(true);
        }
    }

    /**
     * End of this character's turn: frightened goes down by 1 (and goes away at 0), and every
     * (de)buff loses a round, never below 0.
     *
     * @return The (de)buffs whose duration just reached 0, so the user can be asked to remove them.
     */
    public List<DebuffModel> endTurn() {
        lowerFrightened();
        List<DebuffModel> expired = new ArrayList<>();
        for (DebuffModel d : getDebuffList()) {
            int duration = d.getDuration() - 1;
            if (duration == 0) expired.add(d);
            d.setDuration(duration);
        }
        return expired;
    }

    /** @return The persistent damage this character takes at the end of their turn. */
    public List<DebuffModel> getPersistentDamage() {
        List<DebuffModel> result = new ArrayList<>();
        for (DebuffModel d : getDebuffList()) {
            if (d.isPersistentDamage()) result.add(d);
        }
        return result;
    }

    private void lowerFrightened() {
        Iterator<DebuffModel> it = getDebuffList().iterator();
        while (it.hasNext()) {
            DebuffModel d = it.next();
            // Frightened without a value was added by hand; leave it to the user
            if (!Conditions.is(d, Conditions.FRIGHTENED) || d.getValue() <= 0) continue;
            d.setValue(d.getValue() - 1);
            if (d.getValue() == 0) it.remove();
        }
    }

    // endregion

    // region Hit points and dying

    /** Adds healing (fast healing, regeneration) to the current hp. */
    public void heal(int amount) {
        setHp(hp + amount);
    }

    public boolean isAboveMaxHp() {
        return hp > maxHp;
    }

    public void capHpAtMaxHp() {
        if (hp > maxHp) hp = maxHp;
    }

    /** @return The dying value at which this character dies: max dying minus doomed. */
    public int getDeathThreshold() {
        return maxDyingValue - doomedValue;
    }

    /** @return True when the dying value reached the death threshold. */
    public boolean isAlreadyDead() {
        return dyingValue >= getDeathThreshold();
    }

    /** @return The DC of this character's next recovery check: the starting DC + the dying value. */
    public int getRecoveryCheckDc() {
        return recoveryDC + dyingValue;
    }

    /**
     * Knocked out at 0 hp: dying 1, or 2 from a critical hit or critical failure, plus the
     * wounded value.
     */
    public void knockOut(int dying) {
        isDying = true;
        setDyingValue(dying + woundedValue);
    }

    /** Damage while dying: the dying value goes up by 1, or 2 on a critical. */
    public void increaseDying(int amount) {
        setDyingValue(dyingValue + amount);
    }

    /**
     * Applies a recovery check. When the dying value drops to 0 the character loses dying (and
     * gains wounded), stabilized at 0 hp.
     *
     * @return True when the character is no longer dying.
     */
    public boolean applyRecoveryCheck(RecoveryResult result) {
        setDyingValue(dyingValue + result.dyingChange);
        if (dyingValue > 0) return false;
        loseDying();
        hp = 0;
        return true;
    }

    /** Losing the dying condition: dying ends and wounded goes up by 1. */
    public void loseDying() {
        isDying = false;
        dyingValue = 0;
        setWoundedValue(woundedValue + 1);
    }

    /**
     * Heroic Recovery: spend all hero points to stop dying, stabilized at 0 hp, without gaining
     * wounded.
     */
    public void useHeroPointsToStabilize() {
        heroPoints = 0;
        isDying = false;
        dyingValue = 0;
        hp = 0;
    }

    // endregion

    // region Getters and setters

    public int getInitiative() {
        return initiative;
    }

    public void setInitiative(int initiative) {
        this.initiative = NumberInput.clamp(initiative, MIN_INITIATIVE, MAX_INITIATIVE);
    }

    public int getInitiativeBonus() {
        return initiativeBonus;
    }

    public void setInitiativeBonus(int initiativeBonus) {
        this.initiativeBonus = NumberInput.clamp(initiativeBonus, MIN_INITIATIVE, MAX_INITIATIVE);
    }

    public String getSkills() {
        return skills == null ? "" : skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getAttackRoutine() {
        return attackRoutine == null ? "" : attackRoutine;
    }

    public void setAttackRoutine(String attackRoutine) {
        this.attackRoutine = attackRoutine;
    }

    public String getAc() {
        return ac == null ? "" : ac;
    }

    public void setAc(String ac) {
        this.ac = ac;
    }

    public String getSaves() {
        return saves == null ? "" : saves;
    }

    public void setSaves(String saves) {
        this.saves = saves;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = NumberInput.clamp(hp, MIN_HP, MAX_HP);
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = NumberInput.clamp(maxHp, 0, MAX_HP);
    }

    public String getCharacterName() {
        return characterName == null ? "" : characterName;
    }

    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }

    public String getCharacterNotes() {
        return characterNotes == null ? "" : characterNotes;
    }

    public void setCharacterNotes(String characterNotes) {
        this.characterNotes = characterNotes;
    }

    public boolean isFirstInRound() {
        return isFirstInRound;
    }

    public void setFirstInRound(boolean firstInRound) {
        this.isFirstInRound = firstInRound;
    }

    public boolean isPC() {
        return isPC;
    }

    public void setPC(boolean isPC) {
        this.isPC = isPC;
    }

    public int getFastHealing() {
        return fastHealing;
    }

    public void setFastHealing(int fastHealing) {
        this.fastHealing = NumberInput.clamp(fastHealing, 0, MAX_HEALING);
    }

    public int getRegeneration() {
        return regeneration;
    }

    public void setRegeneration(int regeneration) {
        this.regeneration = NumberInput.clamp(regeneration, 0, MAX_HEALING);
    }

    public boolean isDying() {
        return isDying;
    }

    public void setDying(boolean dying) {
        isDying = dying;
    }

    public int getDyingValue() {
        return dyingValue;
    }

    public void setDyingValue(int dyingValue) {
        this.dyingValue = NumberInput.clamp(dyingValue, 0, MAX_CONDITION_VALUE);
    }

    public int getMaxDyingValue() {
        return maxDyingValue;
    }

    public void setMaxDyingValue(int maxDyingValue) {
        this.maxDyingValue = NumberInput.clamp(maxDyingValue, 1, MAX_DYING_VALUE);
    }

    public int getWoundedValue() {
        return woundedValue;
    }

    public void setWoundedValue(int woundedValue) {
        this.woundedValue = NumberInput.clamp(woundedValue, 0, MAX_CONDITION_VALUE);
    }

    public int getDoomedValue() {
        return doomedValue;
    }

    public void setDoomedValue(int doomedValue) {
        this.doomedValue = NumberInput.clamp(doomedValue, 0, MAX_CONDITION_VALUE);
    }

    /** @return The starting recovery DC, before the dying value is added. */
    public int getRecoveryDC() {
        return recoveryDC;
    }

    public void setRecoveryDC(int recoveryDC) {
        this.recoveryDC = NumberInput.clamp(recoveryDC, 0, MAX_RECOVERY_DC);
    }

    public int getHeroPoints() {
        return heroPoints;
    }

    public void setHeroPoints(int heroPoints) {
        this.heroPoints = NumberInput.clamp(heroPoints, 0, MAX_HERO_POINTS);
    }

    /** @return True when the standard reaction has not been used since the start of the turn. */
    public boolean isReactionAvailable() {
        return reactionAvailable;
    }

    public void setReactionAvailable(boolean reactionAvailable) {
        this.reactionAvailable = reactionAvailable;
    }

    /** @return True when the reactions block on the card is folded away. */
    public boolean isReactionCollapsed() {
        return reactionCollapsed;
    }

    public void setReactionCollapsed(boolean reactionCollapsed) {
        this.reactionCollapsed = reactionCollapsed;
    }

    public List<DebuffModel> getDebuffList() {
        // Saves from before (de)buffs existed have no list
        if (debuffList == null) debuffList = new ArrayList<>();
        return debuffList;
    }

    public List<ReactionModel> getReactionList() {
        // Saves from before custom reactions existed have no list
        if (reactionsList == null) reactionsList = new ArrayList<>();
        return reactionsList;
    }

    /** @return How many custom reactions are still available this round. */
    public int getAvailableReactionCount() {
        int count = 0;
        for (ReactionModel r : getReactionList()) {
            if (r.isAvailable()) count++;
        }
        return count;
    }

    // endregion
}
