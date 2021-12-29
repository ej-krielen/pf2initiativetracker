package com.rekijan.initiativetrackersecondedition;

import static com.rekijan.initiativetrackersecondedition.AppConstants.GSON_TAG;
import static com.rekijan.initiativetrackersecondedition.AppConstants.ROUND_COUNTER;
import static com.rekijan.initiativetrackersecondedition.AppConstants.SHARED_PREF_HIDE_DESC;
import static com.rekijan.initiativetrackersecondedition.AppConstants.SHARED_PREF_TAG;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rekijan.initiativetrackersecondedition.character.adapter.CharacterAdapter;
import com.rekijan.initiativetrackersecondedition.character.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.character.model.DebuffModel;
import com.rekijan.initiativetrackersecondedition.ui.activities.MainActivity;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Extends Application to store and manipulate top-level data
 *
 * @author Erik-Jan Krielen ej.krielen@gmail.com
 * @since 16-5-2019
 */
public class AppExtension extends Application {

    private boolean showBackNavigation;
    private CharacterAdapter mCharacterAdapter;
    private ArrayList<DebuffModel> conditions;

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
                    if (c.getReactionList() != null)
                    {
                        tempCharacter.setReactionList(c.getReactionList());
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
        } else {
            mCharacterAdapter.updateActivityReference(activity);
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

    public ArrayList<DebuffModel> getConditionList() {
        if (conditions == null || conditions.size() <= 0) {
            conditions = new ArrayList<>();
            conditions.add(new DebuffModel(getResources().getString(R.string.blinded_name), getResources().getString(R.string.blinded_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.clumsy_name), getResources().getString(R.string.clumsy_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.confused_name), getResources().getString(R.string.confused_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.controlled_name), getResources().getString(R.string.controlled_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.dazzled_name), getResources().getString(R.string.dazzled_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.deafened_name), getResources().getString(R.string.deafened_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.drained_name), getResources().getString(R.string.drained_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.encumbered_name), getResources().getString(R.string.encumbered_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.enfeebled_name), getResources().getString(R.string.enfeebled_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.fascinated_name), getResources().getString(R.string.fascinated_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.fatigued_name), getResources().getString(R.string.fatigued_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.flat_footed_name), getResources().getString(R.string.flat_footed_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.fleeing_name), getResources().getString(R.string.fleeing_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.frightened_name), getResources().getString(R.string.frightened_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.grabbed_name), getResources().getString(R.string.grabbed_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.immobilized_name), getResources().getString(R.string.immobilized_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.paralyzed_name), getResources().getString(R.string.paralyzed_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.petrified_name), getResources().getString(R.string.petrified_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.prone_name), getResources().getString(R.string.prone_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.quickened_name), getResources().getString(R.string.quickened_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.restrained_name), getResources().getString(R.string.restrained_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.sickened_name), getResources().getString(R.string.sickened_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.slowed_name), getResources().getString(R.string.slowed_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.stunned_name), getResources().getString(R.string.stunned_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.stupefied_name), getResources().getString(R.string.stupefied_desc)));
            conditions.add(new DebuffModel(getResources().getString(R.string.unconscious_name), getResources().getString(R.string.unconscious_desc)));

        }
        return conditions;
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

    public void setHideDescriptions(boolean hide)
    {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(SHARED_PREF_HIDE_DESC, android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHARED_PREF_HIDE_DESC, hide);
        editor.apply();
    }

    public boolean getHideDescription()
    {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(SHARED_PREF_HIDE_DESC, android.content.Context.MODE_PRIVATE);
        return preferences.getBoolean(SHARED_PREF_HIDE_DESC, false);
    }
}