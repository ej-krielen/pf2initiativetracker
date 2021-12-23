package com.rekijan.initiativetrackersecondedition.character.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Model class for the Reaction
 *
 * @author Erik-Jan Krielen ej.krielen@gmail.com
 * @since 28-5-2019
 */
public class ReactionModel implements Parcelable {

    private String name;
    private String description;
    private boolean isAvailable;

    public ReactionModel() {
        name = "";
        description = "";
        isAvailable = true;
    }

    public static final Creator<ReactionModel> CREATOR
            = new Creator<ReactionModel>() {
        public ReactionModel createFromParcel(Parcel in) {
            return new ReactionModel(in);
        }

        public ReactionModel[] newArray(int size) {
            return new ReactionModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(description);
        dest.writeByte(this.isAvailable ? (byte) 1 : (byte) 0);
    }

    private ReactionModel(Parcel in) {
        name = in.readString();
        description = in.readString();
        this.isAvailable = in.readByte() != 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
}