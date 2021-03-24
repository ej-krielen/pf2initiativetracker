package com.rekijan.initiativetrackersecondedition;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rekijan.initiativetrackersecondedition.character.adapter.CharacterAdapter;
import com.rekijan.initiativetrackersecondedition.character.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.ui.activities.MainActivity;

import java.lang.reflect.Type;
import java.util.ArrayList;

import static com.rekijan.initiativetrackersecondedition.AppConstants.GSON_TAG;
import static com.rekijan.initiativetrackersecondedition.AppConstants.ROUND_COUNTER;
import static com.rekijan.initiativetrackersecondedition.AppConstants.SHARED_PREF_TAG;

/**
 * Extends Application to store and manipulate top-level data
 *
 * @author Erik-Jan Krielen ej.krielen@gmail.com
 * @since 16-5-2019
 */
public class AppExtension extends Application {

    private boolean showBackNavigation;
    private CharacterAdapter mCharacterAdapter;

    @Override
    public void onCreate() {
        super.onCreate();
        showBackNavigation = false;
    }

    /**
     * Looks for previously saved data and restores that, else it will add 5 empty characters
     *
     * @param activity Reference to {@link MainActivity} to pass to {@link CharacterAdapter}
     */
    public void initializeData(MainActivity activity) {
        if (mCharacterAdapter == null) {
            mCharacterAdapter = new CharacterAdapter(activity);

            SharedPreferences sharedPreferences = this.getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
            Gson gson = new Gson();
            String json = sharedPreferences.getString(GSON_TAG, null);
            Type type = new TypeToken<ArrayList<CharacterModel>>() {
            }.getType();
            ArrayList<CharacterModel> characters;
            characters = gson.fromJson(json, type);
            mCharacterAdapter.removeAll();
            if (characters != null) {
                //Load existing characters
                for (CharacterModel c : characters) {
                    CharacterModel tempCharacter = new CharacterModel(this, c.getInitiative(), c.getInitiativeBonus(), c.getSkills(), c.getAttackRoutine(), c.getAc(), c.getSaves(),
                            c.getHp(), c.getMaxHp(), c.getCharacterName(), c.getCharacterNotes(), c.isFirstRound(), c.isPC(), c.getFastHealing(), c.getRegeneration(),
                            c.isDying(), c.getDyingValue(), c.getMaxDyingValue(), c.getWoundedValue(), c.getDoomedValue(), c.getRecoveryDC(), c.getHeroPoints(), c.isReactionAvailable(), c.isReactionCollapsed());
                    if (c.getDebuffList() != null)
                    {
                        tempCharacter.setDebuffList(c.getDebuffList());
                    }
                    mCharacterAdapter.add(tempCharacter);
                }
            } else {
                //Make a default list
                mCharacterAdapter.add(new CharacterModel(this));
                mCharacterAdapter.add(new CharacterModel(this));
                mCharacterAdapter.add(new CharacterModel(this));
                mCharacterAdapter.add(new CharacterModel(this));
                mCharacterAdapter.add(new CharacterModel(this));
                for (CharacterModel c : mCharacterAdapter.getList()) {
                    c.setIsPC(true);
                }
                mCharacterAdapter.getList().get(mCharacterAdapter.getItemCount()-1).setIsPC(false);
                mCharacterAdapter.getList().get(0).setIsFirstRound(true);
            }
        }
    }

    /**
     * Saves all character data
     */
    public void saveData(int roundCounter) {
        SharedPreferences sharedPreferences = this.getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        ArrayList<CharacterModel> characters = mCharacterAdapter.getList();
        String json = gson.toJson(characters);
        editor.putString(GSON_TAG, json);
        editor.putInt(ROUND_COUNTER, roundCounter);
        editor.apply();
    }

    public CharacterAdapter getCharacterAdapter() {
        return mCharacterAdapter;
    }

    public void setCharacterAdapter(CharacterAdapter mCharacterAdapter) {
        this.mCharacterAdapter = mCharacterAdapter;
    }

    public boolean showBackNavigation() {
        return showBackNavigation;
    }

    public void setShowBackNavigation(boolean showBackNavigation) {
        this.showBackNavigation = showBackNavigation;
    }

}