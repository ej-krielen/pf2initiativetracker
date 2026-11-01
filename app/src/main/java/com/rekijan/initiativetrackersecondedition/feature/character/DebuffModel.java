package com.rekijan.initiativetrackersecondedition.feature.character;

import com.google.gson.annotations.SerializedName;
import com.rekijan.initiativetrackersecondedition.core.util.NumberInput;

/**
 * A (de)buff or condition on a character: a name, a duration in rounds, a description and, for
 * valued conditions such as frightened 2, a value. Persistent damage is a debuff with a damage
 * type, damage and flat check DC, that is resolved at the end of the character's turn.
 * <p>
 * A duration of 0 means it has no timer (or it ran out and the user chose to keep it). A value
 * of 0 means the condition has no value.
 * <p>
 * A condition from a cast spell also knows its caster and its spell: it counts down at the start
 * of the caster's turn instead of at the end of its own character's ({@link #hasCaster}). The
 * caster is kept by {@link CharacterModel#getStableId()}, with the name for showing it (a player
 * only gets the name). All conditions of one cast share a spell id, so the spell can be ended on
 * every target at once. A condition the cast added next to the spell, such as slowed 1 from Slow,
 * follows its own rules and only remembers the spell ({@link #getAddedBySpellId()}), so it can be
 * removed when the spell ends. Without these fields a condition works as any other.
 * <p>
 * A valued condition can be locked: its value doesn't go down on its own below the lock's
 * minimum (e.g. frightened can't drop below 1). A lock lasts until it is unlocked, for a number
 * of rounds (counting on a chosen character's turn), or as long as a spell.
 * <p>
 * The field names are part of the saved JSON, so they must stay the same.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-5-2019
 */
public class DebuffModel {

    public static final int MAX_DURATION = 9999;
    public static final int MAX_VALUE = 99;
    public static final int MAX_DC = 99;
    public static final int DEFAULT_PERSISTENT_DC = 15;

    private String name;
    private int duration;
    private String description;
    private boolean isPersistentDamage;
    private String damageType;
    private String damageValue;
    private int difficultyClass;
    // Saved as "overrideValue" in the JSON
    @SerializedName("overrideValue")
    private int value;
    /** Stable id of the character who cast this; null when it isn't a spell. */
    private String casterId;
    /** The caster's name when cast, for showing it; also what a player sees. */
    private String casterName;
    /** Shared by every condition of one cast; null when it isn't a spell. */
    private String spellId;
    /** The spell whose cast added this condition next to it; null for every other condition. */
    private String addedBySpellId;
    /** Locked: the value doesn't go down on its own below {@link #lockMin}. */
    private boolean locked;
    /** The lowest value a lock lets it go down to by itself, at least 1. */
    private int lockMin;
    /** Rounds the lock has left; 0 means until it is unlocked (or its spell ends). */
    private int lockRounds;
    /**
     * Stable id of the character on whose turn the lock rounds count down (at the start, like a
     * spell); null to count at the end of the condition's own character's turn.
     */
    private String lockCounterId;
    /** The spell the lock lasts as long as; null when the lock doesn't depend on a spell. */
    private String lockSpellId;

    /** Empty (de)buff; also used by Gson when loading saved data. */
    public DebuffModel() {
        name = "";
        description = "";
        damageType = "";
        damageValue = "";
        difficultyClass = DEFAULT_PERSISTENT_DC;
    }

    public DebuffModel(String name, String description, int duration, int value) {
        this();
        this.name = name;
        this.description = description;
        setDuration(duration);
        setValue(value);
    }

    /** A persistent damage debuff, e.g. "fire damage (1d6)" with a DC 15 flat check. */
    public static DebuffModel persistentDamage(String name, String damageType, String damage, int dc,
                                               int duration, String description) {
        DebuffModel d = new DebuffModel(name, description, duration, 0);
        d.isPersistentDamage = true;
        d.damageType = damageType;
        d.damageValue = damage;
        d.setDifficultyClass(dc);
        return d;
    }

    /** @return The name with its value, e.g. "Frightened 2", or just the name without a value. */
    public String getDisplayName() {
        return value > 0 ? getName() + " " + value : getName();
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDuration() {
        return duration;
    }

    /** Sets the duration in rounds, kept between 0 and {@link #MAX_DURATION}. */
    public void setDuration(int duration) {
        this.duration = NumberInput.clamp(duration, 0, MAX_DURATION);
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    /** Sets the condition value, kept between 0 (no value) and {@link #MAX_VALUE}. */
    public void setValue(int value) {
        this.value = NumberInput.clamp(value, 0, MAX_VALUE);
    }

    /** @return True when this debuff is persistent damage, resolved at the end of the turn. */
    public boolean isPersistentDamage() {
        return isPersistentDamage;
    }

    public String getDamageType() {
        return damageType == null ? "" : damageType;
    }

    public void setDamageType(String damageType) {
        this.damageType = damageType;
    }

    /** @return The damage as typed, e.g. "5" or "1d6". */
    public String getDamageValue() {
        return damageValue == null ? "" : damageValue;
    }

    public void setDamageValue(String damageValue) {
        this.damageValue = damageValue;
    }

    public int getDifficultyClass() {
        return difficultyClass;
    }

    public void setDifficultyClass(int difficultyClass) {
        this.difficultyClass = NumberInput.clamp(difficultyClass, 0, MAX_DC);
    }

    /**
     * Lowers the duration by one round, never below 0.
     *
     * @return True when it reached 0 just now.
     */
    public boolean tick() {
        int left = duration - 1;
        setDuration(left);
        return left == 0;
    }

    /** @return True when this counts down on its caster's turn, not on its own character's. */
    public boolean hasCaster() {
        return casterId != null && !casterId.isEmpty();
    }

    /** Marks this as cast by a character, as part of one spell. */
    public void setCaster(String casterId, String casterName, String spellId) {
        this.casterId = casterId;
        this.casterName = casterName;
        this.spellId = spellId;
    }

    /**
     * Takes over the caster and spell links and the lock of another condition (e.g. the GM's own
     * copy of it).
     */
    public void copySpellFrom(DebuffModel other) {
        setCaster(other.casterId, other.casterName, other.spellId);
        addedBySpellId = other.addedBySpellId;
        locked = other.locked;
        lockMin = other.lockMin;
        lockRounds = other.lockRounds;
        lockCounterId = other.lockCounterId;
        lockSpellId = other.lockSpellId;
    }

    /** @return True when this is a spell, a condition a spell added, or locked: GM-only links. */
    public boolean hasGmLinks() {
        return spellId != null || addedBySpellId != null || locked;
    }

    /**
     * Lowers the value by one, as frightened does at the end of a turn, but not below the lock's
     * minimum while locked.
     *
     * @return True when the value reached 0, so the condition ends.
     */
    public boolean lowerValue() {
        if (value <= 0) return false;
        if (locked && value <= lockMin) return false;
        setValue(value - 1);
        return value == 0;
    }

    /**
     * Locks the value: it doesn't go down on its own below {@code min}.
     *
     * @param min       At least 1.
     * @param rounds    0 for until it is unlocked (or its spell ends), else the rounds it lasts.
     * @param counterId The character on whose turn the rounds count, or null for this condition's own.
     * @param spellId   The spell the lock lasts as long as, or null.
     */
    public void lock(int min, int rounds, String counterId, String spellId) {
        locked = true;
        lockMin = NumberInput.clamp(min, 1, MAX_VALUE);
        lockRounds = NumberInput.clamp(rounds, 0, MAX_DURATION);
        lockCounterId = counterId;
        lockSpellId = spellId;
    }

    public void unlock() {
        locked = false;
        lockMin = 0;
        lockRounds = 0;
        lockCounterId = null;
        lockSpellId = null;
    }

    /**
     * One round of the lock went by; at 0 it unlocks. A lock without rounds stays.
     *
     * @return True when it unlocked just now.
     */
    public boolean tickLock() {
        if (!locked || lockRounds <= 0) return false;
        lockRounds--;
        if (lockRounds > 0) return false;
        unlock();
        return true;
    }

    public boolean isLocked() {
        return locked;
    }

    public int getLockMin() {
        return Math.max(1, lockMin);
    }

    public void setLockMin(int lockMin) {
        this.lockMin = NumberInput.clamp(lockMin, 1, MAX_VALUE);
    }

    public int getLockRounds() {
        return lockRounds;
    }

    public void setLockRounds(int lockRounds) {
        this.lockRounds = NumberInput.clamp(lockRounds, 0, MAX_DURATION);
    }

    public String getLockCounterId() {
        return lockCounterId;
    }

    public void setLockCounterId(String lockCounterId) {
        this.lockCounterId = lockCounterId;
    }

    public String getLockSpellId() {
        return lockSpellId;
    }

    public String getCasterId() {
        return casterId;
    }

    /** Lets the spell count on another character's turn: the spell card that took the caster's place. */
    public void setCasterId(String casterId) {
        this.casterId = casterId;
    }

    public String getCasterName() {
        return casterName == null ? "" : casterName;
    }

    public void setCasterName(String casterName) {
        this.casterName = casterName;
    }

    public String getSpellId() {
        return spellId;
    }

    public String getAddedBySpellId() {
        return addedBySpellId;
    }

    public void setAddedBySpellId(String addedBySpellId) {
        this.addedBySpellId = addedBySpellId;
    }
}
