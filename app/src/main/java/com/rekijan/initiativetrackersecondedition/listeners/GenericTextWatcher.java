package com.rekijan.initiativetrackersecondedition.listeners;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.character.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.character.model.DebuffModel;
import com.rekijan.initiativetrackersecondedition.character.model.ReactionModel;

/**
 * <p>Listener for EditText</p>
 * See {@link CharacterModel} and {@link com.rekijan.initiativetrackersecondedition.character.adapter.CharacterAdapter} as well as {@link DebuffModel} and {@link com.rekijan.initiativetrackersecondedition.character.adapter.DebuffAdapter}
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 26-10-2015
 */
public class GenericTextWatcher implements TextWatcher {
    private final View view;
    private CharacterModel character;
    private DebuffModel debuff;
    private ReactionModel reaction;

    public GenericTextWatcher(CharacterModel character, View view) {
        this.character = character;
        this.view = view;
    }

    public GenericTextWatcher(DebuffModel debuff, View view) {
        this.debuff = debuff;
        this.view = view;
    }

    public GenericTextWatcher(ReactionModel reaction, View view) {
        this.reaction = reaction;
        this.view = view;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        String text = s.toString();
        int viewId = view.getId();
        if (viewId == R.id.character_initiative_editText) {
            character.setInitiative(inputIntText(text));
        } else if (viewId == R.id.initiative_bonus_editText) {
            character.setInitiativeBonus(inputIntText(text));
        } else if (viewId == R.id.fast_healing_editText) {
            character.setFastHealing(inputIntText(text));
        } else if (viewId == R.id.regeneration_editText) {
            character.setRegeneration(inputIntText(text));
        } else if (viewId == R.id.dying_editText) {
            character.setDyingValue(inputIntText(text));
        } else if (viewId == R.id.max_dying_editText) {
            character.setMaxDyingValue(Math.min(TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text), 6));
        } else if (viewId == R.id.wounded_editText) {
            character.setWoundedValue(inputIntText(text));
        } else if (viewId == R.id.doomed_editText) {
            character.setDoomedValue(inputIntText(text));
        } else if (viewId == R.id.recovery_dc_editText) {
            character.setRecoveryDC(inputIntText(text));
        } else if (viewId == R.id.hero_points_editText) {
            character.setHeroPoints(inputIntText(text));
        } else if (viewId == R.id.skills_editText) {
            character.setSkills(text);
        } else if (viewId == R.id.attack_routine_editText) {
            character.setAttackRoutine(text);
        } else if (viewId == R.id.ac_editText) {
            character.setAc(text);
        } else if (viewId == R.id.saves_editText) {
            character.setSaves(text);
        } else if (viewId == R.id.max_hp_editText) {
            character.setMaxHp(inputIntText(text));
        } else if (viewId == R.id.character_name_editText) {
            character.setCharacterName(text);
        } else if (viewId == R.id.notes_editText) {
            character.setCharacterNotes(text);
        } else if (viewId == R.id.debuff_name_editText) {
            debuff.setName(text);
        } else if (viewId == R.id.debuff_duration_editText) {
            debuff.setDuration(inputIntText(text));
        } else if (viewId == R.id.debuff_description_editText) {
            debuff.setDescription(text);
        } else if (viewId == R.id.reaction_name_editText) {
            reaction.setName(text);
        } else if (viewId == R.id.reaction_description_editText) {
            reaction.setDescription(text);
        }
    }

    private int inputIntText(String text) {
        return TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text);
    }
}