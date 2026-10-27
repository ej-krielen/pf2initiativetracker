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
}
