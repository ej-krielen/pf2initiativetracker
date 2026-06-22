package com.rekijan.initiativetrackersecondedition.core;

import static com.rekijan.initiativetrackersecondedition.core.AppConstants.GSON_TAG;
import static com.rekijan.initiativetrackersecondedition.core.AppConstants.ROUND_COUNTER;
import static com.rekijan.initiativetrackersecondedition.core.AppConstants.SHARED_PREF_HIDE_DESC;
import static com.rekijan.initiativetrackersecondedition.core.AppConstants.SHARED_PREF_HIDE_REACTIONS;
import static com.rekijan.initiativetrackersecondedition.core.AppConstants.SHARED_PREF_TAG;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.features.initiative.InitiativeAdapter;
import com.rekijan.initiativetrackersecondedition.features.initiative.InitiativeManager;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Extends Application to store and manipulate top-level data.
 * Acts as the central hub for shared state like settings and the character data manager.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 16-5-2019
 */
public class AppExtension extends Application {
    /// /////////////////////////////////////////////////////////////////////////////////////////////
    /// Members
    /// /////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showBackNavigation;
    private InitiativeAdapter mInitiativeAdapter;
    private InitiativeManager mInitiativeManager;

    /// /////////////////////////////////////////////////////////////////////////////////////////////
    /// Override methods
    /// /////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onCreate () {
        super.onCreate();
        showBackNavigation = false;
        mInitiativeManager = new InitiativeManager();
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////    /**
     * Looks for previously saved data and restores that, else it will add 4 empty characters.
     *
     * @param activity Reference to {@link MainActivity} to pass to {@link InitiativeAdapter}.
     */
    public void initializeData (MainActivity activity) {
        if (mInitiativeAdapter == null) {
            mInitiativeAdapter = new InitiativeAdapter(activity, mInitiativeManager);
            mInitiativeManager.clearAllCharacters();
            ArrayList<CharacterModel> characters = loadSavedCharacters();

            if (characters != null) {
                for (CharacterModel c : characters) {
                    CharacterModel tempCharacter = getCharacterModel(c);
                    mInitiativeManager.addCharacter(tempCharacter);
                }
            }
            else {
                makeDefaultEmptyList();
            }
        }
        else {
            mInitiativeAdapter.updateActivityReference(activity);
        }
    }

    /**
     * Serializes the current character list and round counter to SharedPreferences.
     *
     * @param roundCounter The current state of the round counter.
     */
    public void saveData (int roundCounter) {
        if (mInitiativeManager == null) return;
        SharedPreferences.Editor editor = getEditor();
        ArrayList<CharacterModel> characters = mInitiativeManager.getCharacters();
        String json = new Gson().toJson(characters);
        editor.putString(GSON_TAG, json);
        editor.putInt(ROUND_COUNTER, roundCounter);
        editor.apply();
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Getters/Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////    /**
     * @return The global initiative adapter instance.
     */
    public InitiativeAdapter getInitiativeAdapter () {
        return mInitiativeAdapter;
    }

    /**
     * @return The global initiative manager instance (source of truth).
     */
    public InitiativeManager getInitiativeManager() {
        return mInitiativeManager;
    }

    public boolean showBackNavigation () {
        return showBackNavigation;
    }

    public void setShowBackNavigation (boolean showBackNavigation) {
        this.showBackNavigation = showBackNavigation;
    }

    /**
     * @return true if the user has opted to hide debuff descriptions in the wizard.
     */
    public boolean getHideDescription () {
        return getSharedPreferences().getBoolean(SHARED_PREF_HIDE_DESC, false);
    }

    public void setHideDescriptions (boolean hide) {
        SharedPreferences.Editor editor = getEditor();
        editor.putBoolean(SHARED_PREF_HIDE_DESC, hide);
        editor.apply();
    }

    /**
     * @return true if the user has opted to hide reactions on the character cards.
     */
    public boolean getHideReactions () {
        return getSharedPreferences().getBoolean(SHARED_PREF_HIDE_REACTIONS, false);
    }

    public void setHideReactions (boolean hide) {
        SharedPreferences.Editor editor = getEditor();
        editor.putBoolean(SHARED_PREF_HIDE_REACTIONS, hide);
        editor.apply();
        if (mInitiativeAdapter != null) {
            mInitiativeAdapter.notifyItemRangeChanged(0, mInitiativeAdapter.getItemCount());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Private helpers
    ////////////////////////////////////////////////////////////////////////////////////////////////    /**
     * Loads the character list JSON from disk and deserializes it.
     */
    private ArrayList<CharacterModel> loadSavedCharacters () {
        String json = getSharedPreferences().getString(GSON_TAG, null);
        Type type = new TypeToken<ArrayList<CharacterModel>>() {
        }.getType();
        return new Gson().fromJson(json, type);
    }

    /**
     * Maps a deserialized character to a fresh instance with proper context.
     */
    private CharacterModel getCharacterModel (CharacterModel c) {
        CharacterModel tempCharacter = new CharacterModel(this, c.getInitiative(), c.getInitiativeBonus(), c.getSkills(), c.getAttackRoutine(), c.getAc(), c.getSaves(), c.getHp(), c.getMaxHp(), c.getCharacterName(), c.getCharacterNotes(), c.isFirstRound(), c.isPC(), c.getFastHealing(), c.getRegeneration(), c.isDying(), c.getDyingValue(), c.getMaxDyingValue(), c.getWoundedValue(), c.getDoomedValue(), c.getRecoveryDC(), c.getHeroPoints(), c.isReactionAvailable(), c.isReactionCollapsed());
        if (c.getDebuffList() != null) {
            tempCharacter.setDebuffList(c.getDebuffList());
        }
        if (c.getReactionList() != null) {
            tempCharacter.setReactionList(c.getReactionList());
        }
        return tempCharacter;
    }

    /**
     * Populates the manager with 4 default characters if no saved data is found.
     */
    private void makeDefaultEmptyList () {
        mInitiativeManager.addCharacter(new CharacterModel(this));
        mInitiativeManager.addCharacter(new CharacterModel(this));
        mInitiativeManager.addCharacter(new CharacterModel(this));
        mInitiativeManager.addCharacter(new CharacterModel(this));
        for (CharacterModel c : mInitiativeManager.getCharacters()) {
            c.setIsPC(true);
        }
        mInitiativeManager.getCharacters().get(mInitiativeManager.getCharacters().size() - 1).setIsPC(false);
        mInitiativeManager.getCharacters().get(0).setIsFirstRound(true);
    }

    private SharedPreferences getSharedPreferences () {
        return getApplicationContext().getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
    }

    private SharedPreferences.Editor getEditor(){
        return getSharedPreferences().edit();
    }
}