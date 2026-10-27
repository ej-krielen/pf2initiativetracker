package com.rekijan.initiativetrackersecondedition.core.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.condition.Conditions;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Converts the character list to and from the saved JSON. A valued condition saved with its
 * value in the name ("Frightened 2") gets the value split off the name. Plain Java, so loading
 * saves is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public final class EncounterJson {

    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<ArrayList<CharacterModel>>() {
    }.getType();

    private EncounterJson() {
    }

    public static String toJson(List<CharacterModel> characters) {
        return GSON.toJson(new ArrayList<>(characters), LIST_TYPE);
    }

    /**
     * @return The saved characters, or null when nothing (readable) was saved.
     */
    public static List<CharacterModel> fromJson(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            List<CharacterModel> characters = GSON.fromJson(json, LIST_TYPE);
            if (characters == null) return null;
            characters.removeIf(Objects::isNull);
            for (CharacterModel c : characters) {
                clean(c);
            }
            return characters;
        } catch (JsonParseException e) {
            return null;
        }
    }

    /** One character in the same format, used for the player characters. */
    public static String characterToJson(CharacterModel character) {
        return GSON.toJson(character);
    }

    /** @return The saved character, or null when nothing (readable) was saved. */
    public static CharacterModel characterFromJson(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            CharacterModel character = GSON.fromJson(json, CharacterModel.class);
            if (character != null) clean(character);
            return character;
        } catch (JsonParseException e) {
            return null;
        }
    }

    private static void clean(CharacterModel c) {
        c.getDebuffList().removeIf(Objects::isNull);
        c.getReactionList().removeIf(Objects::isNull);
        for (DebuffModel d : c.getDebuffList()) {
            Conditions.upgradeLegacyValue(d);
        }
    }
}
