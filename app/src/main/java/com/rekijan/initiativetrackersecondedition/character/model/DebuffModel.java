package com.rekijan.initiativetrackersecondedition.character.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Model class for the Debuffs
 *
 * @author Erik-Jan Krielen ej.krielen@gmail.com
 * @since 28-5-2019
 */
public class DebuffModel implements Parcelable {

    private String name;
    private int duration;
    private String description;
    private boolean isPersistentDamage;
    private String damageType;
    private String damageValue;
    private int difficultyClass;
    private int overrideValue;

    public DebuffModel() {
        name = "";
        damageType = "";
        duration = 0;
        isPersistentDamage = false;
        difficultyClass = 15;
        damageValue = "5";
        description = "";
        overrideValue = 0;
    }

    public DebuffModel(String name, String description) {
        this.name = name;
        this.description = description;
        duration = 0;
        isPersistentDamage = false;
        difficultyClass = 15;
        damageValue = "5";
        overrideValue = 0;
    }

    public DebuffModel(String name, String description, int duration) {
        this.name = name;
        this.description = description;
        this.duration = duration;
    }

    public DebuffModel(String name, String type, int duration, String damage, int difficultyClass, String description) {
        this.name = name;
        this.damageType = type;
        this.description = description;
        this.damageValue = damage;
        this.difficultyClass = difficultyClass;
        this.duration = duration;
        overrideValue = 0;
        isPersistentDamage = true;
    }

    public static final Parcelable.Creator<DebuffModel> CREATOR
            = new Parcelable.Creator<DebuffModel>() {
        public DebuffModel createFromParcel(Parcel in) {
            return new DebuffModel(in);
        }

        public DebuffModel[] newArray(int size) {
            return new DebuffModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(duration);
        dest.writeString(description);
        dest.writeByte(isPersistentDamage ? (byte) 1 : (byte) 0);
        dest.writeString(damageType);
        dest.writeString(damageValue);
        dest.writeInt(difficultyClass);
        dest.writeInt(overrideValue);
    }

    private DebuffModel(Parcel in) {
        name = in.readString();
        duration = in.readInt();
        description = in.readString();
        isPersistentDamage = in.readByte() != 0;
        damageType = in.readString();
        damageValue = in.readString();
        difficultyClass = in.readInt();
        overrideValue = in.readInt();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPersistentDamage() {
        return isPersistentDamage;
    }

    public void setPersistentDamage(boolean persistentDamage) {
        isPersistentDamage = persistentDamage;
    }

    public String getDamageType() {
        return damageType;
    }

    public void setDamageType(String damageType) {
        this.damageType = damageType;
    }

    public String getDamageValue() {
        return damageValue;
    }

    public void setDamageValue(String damageValue) {
        this.damageValue = damageValue;
    }

    public int getDifficultyClass() {
        return difficultyClass;
    }

    public void setDifficultyClass(int difficultyClass) {
        this.difficultyClass = difficultyClass;
    }

    public int getOverrideValue() {
        return overrideValue;
    }

    public void setOverrideValue(int overrideValue) {
        this.overrideValue = overrideValue;
    }
}