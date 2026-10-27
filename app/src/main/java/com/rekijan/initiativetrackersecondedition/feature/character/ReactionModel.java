package com.rekijan.initiativetrackersecondedition.feature.character;

/**
 * A custom reaction of a character, next to the standard one (e.g. Shield Block, Attack of
 * Opportunity). Like the standard reaction it becomes available again at the start of the
 * character's turn.
 * <p>
 * The field names are part of the saved JSON, so they must stay the same.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-5-2019
 */
public class ReactionModel {

    private String name;
    private String description;
    private boolean isAvailable;

    /** New, available reaction; also used by Gson when loading saved data. */
    public ReactionModel() {
        name = "";
        description = "";
        isAvailable = true;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** @return True when the reaction has not been used since the start of the character's turn. */
    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
}
