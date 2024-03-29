package com.rekijan.initiativetrackersecondedition.ui.fragments;

import static com.rekijan.initiativetrackersecondedition.AppConstants.POSITION;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.AppExtension;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.character.adapter.DebuffAdapter;
import com.rekijan.initiativetrackersecondedition.character.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.character.model.DebuffModel;
import com.rekijan.initiativetrackersecondedition.listeners.GenericTextWatcher;
import com.rekijan.initiativetrackersecondedition.listeners.HpTextWatcher;
import com.rekijan.initiativetrackersecondedition.ui.activities.MainActivity;

import java.util.ArrayList;

/**
 * A fragment containing details of a single CharacterModel
 */
public class CharacterDetailFragment extends Fragment {

    private int position;

    public CharacterDetailFragment() {}

    public static CharacterDetailFragment newInstance(int position) {
        CharacterDetailFragment fragment =  new CharacterDetailFragment();
        Bundle args = new Bundle();
        args.putInt(POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            position = getArguments().getInt(POSITION);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        menu.clear();
        inflater.inflate(R.menu.menu_minimum,menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_character_detail, container, false);
        AppExtension app = (AppExtension) getActivity().getApplicationContext();

        //Get the corresponding CharacterModel
        final CharacterModel characterModel = app.getCharacterAdapter().getList().get(position);

        //Get fields and set their values
        TextView textView = rootView.findViewById(R.id.character_detail_textView);
        textView.setText(String.format(getString(R.string.character_details_name_title), characterModel.getCharacterName()));

        SwitchCompat pcSwitch = rootView.findViewById(R.id.is_pc_switch);
        pcSwitch.setChecked(characterModel.isPC());

        SwitchCompat isDyingSwitch = rootView.findViewById(R.id.is_dying_switch);
        isDyingSwitch.setChecked(characterModel.isDying());

        EditText initiativeBonusEditText = rootView.findViewById(R.id.initiative_bonus_editText);
        initiativeBonusEditText.setText(String.valueOf(characterModel.getInitiativeBonus()));

        EditText maxHpEditText = rootView.findViewById(R.id.max_hp_editText);
        maxHpEditText.setText(String.valueOf(characterModel.getMaxHp()));

        EditText fastHealingEditText = rootView.findViewById(R.id.fast_healing_editText);
        fastHealingEditText.setText(String.valueOf(characterModel.getFastHealing()));

        EditText regenerationEditText = rootView.findViewById(R.id.regeneration_editText);
        regenerationEditText.setText(String.valueOf(characterModel.getRegeneration()));

        EditText dyingEditText = rootView.findViewById(R.id.dying_editText);
        dyingEditText.setText(String.valueOf(characterModel.getDyingValue()));

        EditText maxDyingEditText = rootView.findViewById(R.id.max_dying_editText);
        maxDyingEditText.setText(String.valueOf(characterModel.getMaxDyingValue()));

        EditText woundedEditText = rootView.findViewById(R.id.wounded_editText);
        woundedEditText.setText(String.valueOf(characterModel.getWoundedValue()));

        EditText doomedEditText = rootView.findViewById(R.id.doomed_editText);
        doomedEditText.setText(String.valueOf(characterModel.getDoomedValue()));

        EditText recoveryDcEditText = rootView.findViewById(R.id.recovery_dc_editText);
        recoveryDcEditText.setText(String.valueOf(characterModel.getRecoveryDC()));

        EditText heroPointsEditText = rootView.findViewById(R.id.hero_points_editText);
        heroPointsEditText.setText(String.valueOf(characterModel.getHeroPoints()));

        EditText skillsEditText = rootView.findViewById(R.id.skills_editText);
        skillsEditText.setText(characterModel.getSkills());

        EditText attackRoutineEditText = rootView.findViewById(R.id.attack_routine_editText);
        attackRoutineEditText.setText(characterModel.getAttackRoutine());

        EditText acEditText = rootView.findViewById(R.id.ac_editText);
        acEditText.setText(characterModel.getAc());

        EditText savesEditText = rootView.findViewById(R.id.saves_editText);
        savesEditText.setText(characterModel.getSaves());

        EditText notesEditText = rootView.findViewById(R.id.notes_editText);
        notesEditText.setText(characterModel.getCharacterNotes());

        //Remove watcher if they exist to avoid double watchers
        GenericTextWatcher oldInitiativeBonusWatcher = (GenericTextWatcher) initiativeBonusEditText.getTag();
        if (oldInitiativeBonusWatcher != null) {
            initiativeBonusEditText.removeTextChangedListener(oldInitiativeBonusWatcher);
        }

        GenericTextWatcher oldFastHealingWatcher = (GenericTextWatcher) fastHealingEditText.getTag();
        if (oldFastHealingWatcher != null) {
            fastHealingEditText.removeTextChangedListener(oldFastHealingWatcher);
        }

        GenericTextWatcher oldRegenerationWatcher = (GenericTextWatcher) regenerationEditText.getTag();
        if (oldRegenerationWatcher != null) {
            regenerationEditText.removeTextChangedListener(oldRegenerationWatcher);
        }

        GenericTextWatcher oldDyingWatcher = (GenericTextWatcher) dyingEditText.getTag();
        if (oldDyingWatcher != null) {
            dyingEditText.removeTextChangedListener(oldDyingWatcher);
        }

        GenericTextWatcher oldMaxDyingWatcher = (GenericTextWatcher) maxDyingEditText.getTag();
        if (oldMaxDyingWatcher != null) {
            maxDyingEditText.removeTextChangedListener(oldMaxDyingWatcher);
        }

        GenericTextWatcher oldWoundedWatcher = (GenericTextWatcher) woundedEditText.getTag();
        if (oldWoundedWatcher != null) {
            woundedEditText.removeTextChangedListener(oldWoundedWatcher);
        }

        GenericTextWatcher oldDoomedWatcher = (GenericTextWatcher) doomedEditText.getTag();
        if (oldDoomedWatcher != null) {
            doomedEditText.removeTextChangedListener(oldDoomedWatcher);
        }

        GenericTextWatcher oldRecoveryDcWatcher = (GenericTextWatcher) recoveryDcEditText.getTag();
        if (oldRecoveryDcWatcher != null) {
            recoveryDcEditText.removeTextChangedListener(oldRecoveryDcWatcher);
        }

        GenericTextWatcher oldHeroPointsWatcher = (GenericTextWatcher) heroPointsEditText.getTag();
        if (oldHeroPointsWatcher != null) {
            heroPointsEditText.removeTextChangedListener(oldHeroPointsWatcher);
        }

        GenericTextWatcher oldSkillsWatcher = (GenericTextWatcher) skillsEditText.getTag();
        if (oldSkillsWatcher != null) {
            skillsEditText.removeTextChangedListener(oldSkillsWatcher);
        }

        GenericTextWatcher oldAttackRoutineWatcher = (GenericTextWatcher) attackRoutineEditText.getTag();
        if (oldAttackRoutineWatcher != null) {
            attackRoutineEditText.removeTextChangedListener(oldAttackRoutineWatcher);
        }

        GenericTextWatcher oldAcWatcher = (GenericTextWatcher) acEditText.getTag();
        if (oldAcWatcher != null) {
            acEditText.removeTextChangedListener(oldAcWatcher);
        }

        GenericTextWatcher oldSavesWatcher = (GenericTextWatcher) savesEditText.getTag();
        if (oldSavesWatcher != null) {
            savesEditText.removeTextChangedListener(oldSavesWatcher);
        }

        GenericTextWatcher oldNoteWatcher = (GenericTextWatcher) notesEditText.getTag();
        if (oldNoteWatcher != null) {
            notesEditText.removeTextChangedListener(oldNoteWatcher);
        }

        HpTextWatcher oldHPWatcher = (HpTextWatcher) maxHpEditText.getTag();
        if (oldHPWatcher != null) {
            maxHpEditText.setOnClickListener(null);
        }

        //Add new text watchers
        GenericTextWatcher newInitiativeBonusWatcher = new GenericTextWatcher(characterModel, initiativeBonusEditText);
        initiativeBonusEditText.setTag(newInitiativeBonusWatcher);
        initiativeBonusEditText.addTextChangedListener(newInitiativeBonusWatcher);

        GenericTextWatcher newFastHealingWatcher = new GenericTextWatcher(characterModel, fastHealingEditText);
        fastHealingEditText.setTag(newFastHealingWatcher);
        fastHealingEditText.addTextChangedListener(newFastHealingWatcher);

        GenericTextWatcher newRegenerationWatcher = new GenericTextWatcher(characterModel, regenerationEditText);
        regenerationEditText.setTag(newRegenerationWatcher);
        regenerationEditText.addTextChangedListener(newRegenerationWatcher);

        GenericTextWatcher newDyingWatcher = new GenericTextWatcher(characterModel, dyingEditText);
        dyingEditText.setTag(newDyingWatcher);
        dyingEditText.addTextChangedListener(newDyingWatcher);

        GenericTextWatcher newMaxDyingWatcher = new GenericTextWatcher(characterModel, maxDyingEditText);
        maxDyingEditText.setTag(newMaxDyingWatcher);
        maxDyingEditText.addTextChangedListener(newMaxDyingWatcher);

        GenericTextWatcher newWoundedWatcher = new GenericTextWatcher(characterModel, woundedEditText);
        woundedEditText.setTag(newWoundedWatcher);
        woundedEditText.addTextChangedListener(newWoundedWatcher);

        GenericTextWatcher newDoomedWatcher = new GenericTextWatcher(characterModel, doomedEditText);
        doomedEditText.setTag(newDoomedWatcher);
        doomedEditText.addTextChangedListener(newDoomedWatcher);

        GenericTextWatcher newRecoveryDcWatcher = new GenericTextWatcher(characterModel, recoveryDcEditText);
        recoveryDcEditText.setTag(newRecoveryDcWatcher);
        recoveryDcEditText.addTextChangedListener(newRecoveryDcWatcher);

        GenericTextWatcher newHeroPointsWatcher = new GenericTextWatcher(characterModel, heroPointsEditText);
        heroPointsEditText.setTag(newHeroPointsWatcher);
        heroPointsEditText.addTextChangedListener(newHeroPointsWatcher);

        GenericTextWatcher newSkillsWatcher = new GenericTextWatcher(characterModel, skillsEditText);
        skillsEditText.setTag(newSkillsWatcher);
        skillsEditText.addTextChangedListener(newSkillsWatcher);

        GenericTextWatcher newAttackRoutineWatcher = new GenericTextWatcher(characterModel, attackRoutineEditText);
        attackRoutineEditText.setTag(newAttackRoutineWatcher);
        attackRoutineEditText.addTextChangedListener(newAttackRoutineWatcher);

        GenericTextWatcher newAcWatcher = new GenericTextWatcher(characterModel, acEditText);
        acEditText.setTag(newAcWatcher);
        acEditText.addTextChangedListener(newAcWatcher);

        GenericTextWatcher newSavesWatcher = new GenericTextWatcher(characterModel, savesEditText);
        savesEditText.setTag(newSavesWatcher);
        savesEditText.addTextChangedListener(newSavesWatcher);

        GenericTextWatcher newNoteWatcher = new GenericTextWatcher(characterModel, notesEditText);
        notesEditText.setTag(newNoteWatcher);
        notesEditText.addTextChangedListener(newNoteWatcher);

        GenericTextWatcher newMaxHPWatcher = new GenericTextWatcher(characterModel, maxHpEditText);
        maxHpEditText.setTag(newMaxHPWatcher);
        maxHpEditText.addTextChangedListener(newMaxHPWatcher);

        //Setup RecyclerView by binding the adapter to it.
        RecyclerView debuffRecyclerView = rootView.findViewById(R.id.debuff_recyclerView);
        debuffRecyclerView.setHasFixedSize(false);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        debuffRecyclerView.setLayoutManager(llm);

        final DebuffAdapter adapter = new DebuffAdapter(characterModel);
        ArrayList<DebuffModel> debuffList = characterModel.getDebuffList();
        adapter.addAll(debuffList);
        debuffRecyclerView.setAdapter(adapter);

        //Buttons
        Button clearAllDebuffButton = rootView.findViewById(R.id.clear_all_debuffs_button);
        clearAllDebuffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.removeAll();
                characterModel.getDebuffList().clear();
                adapter.notifyDataSetChanged();
            }
        });

        Button addDebuffButton = rootView.findViewById(R.id.add_debuff_button);
        addDebuffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity)getContext()).replaceDebuffWizardFragment(position);
            }
        });

        pcSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                characterModel.setIsPC(isChecked);
            }
        });

        isDyingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                characterModel.setIsDying(isChecked);
            }
        });

        return rootView;
    }
}