package com.rekijan.initiativetrackersecondedition.features.character;

import static com.rekijan.initiativetrackersecondedition.core.AppConstants.POSITION;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.common.listeners.EditTextWatcher;
import com.rekijan.initiativetrackersecondedition.core.AppConstants;
import com.rekijan.initiativetrackersecondedition.core.AppExtension;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.data.model.DebuffModel;
import com.rekijan.initiativetrackersecondedition.features.debuffs.DebuffAdapter;
import com.rekijan.initiativetrackersecondedition.core.MainActivity;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * A fragment showing the full details of a character.
 * Allows editing all character fields, managing their debuffs, and toggling PC/Dying status.
 */
public class CharacterDetailFragment extends Fragment {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members
    ////////////////////////////////////////////////////////////////////////////////////////////////    private int position;
    private Toast maxDyingInvalidToast;

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Constructor
    ////////////////////////////////////////////////////////////////////////////////////////////////    public CharacterDetailFragment() {
    }

    /**
     * @param position The index of the character to display.
     * @return A new instance of CharacterDetailFragment.
     */
    public static CharacterDetailFragment newInstance(int position) {
        CharacterDetailFragment fragment = new CharacterDetailFragment();
        Bundle args = new Bundle();
        args.putInt(POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            position = getArguments().getInt(POSITION);
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Override methods
    ////////////////////////////////////////////////////////////////////////////////////////////////    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (!getResources().getBoolean(R.bool.isTablet)) {
            menu.clear();
            inflater.inflate(R.menu.menu_minimum, menu);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_character_detail, container, false);
        AppExtension app = (AppExtension) requireActivity().getApplicationContext();

        //Get the corresponding CharacterModel
        final CharacterModel characterModel = app.getInitiativeManager().getCharacters().get(position);

        //Get fields and set their values
        TextView textView = rootView.findViewById(R.id.character_detail_textView);
        textView.setText(String.format(getString(R.string.character_details_name_title), characterModel.getCharacterName()));

        SwitchCompat pcSwitch = rootView.findViewById(R.id.is_pc_switch);
        pcSwitch.setChecked(characterModel.isPC());

        SwitchCompat isDyingSwitch = rootView.findViewById(R.id.is_dying_switch);
        isDyingSwitch.setChecked(characterModel.isDying());

        // Setup all interactive fields
        setupIntField(rootView, R.id.initiative_bonus_editText, characterModel.getInitiativeBonus(), characterModel::setInitiativeBonus);
        setupIntField(rootView, R.id.max_hp_editText, characterModel.getMaxHp(), characterModel::setMaxHp);
        setupIntField(rootView, R.id.fast_healing_editText, characterModel.getFastHealing(), characterModel::setFastHealing);
        setupIntField(rootView, R.id.regeneration_editText, characterModel.getRegeneration(), characterModel::setRegeneration);
        setupIntField(rootView, R.id.dying_editText, characterModel.getDyingValue(), characterModel::setDyingValue);
        setupIntField(rootView, R.id.wounded_editText, characterModel.getWoundedValue(), characterModel::setWoundedValue);
        setupIntField(rootView, R.id.doomed_editText, characterModel.getDoomedValue(), characterModel::setDoomedValue);
        setupIntField(rootView, R.id.recovery_dc_editText, characterModel.getRecoveryDC(), characterModel::setRecoveryDC);
        setupIntField(rootView, R.id.hero_points_editText, characterModel.getHeroPoints(), characterModel::setHeroPoints);

        setupField(rootView, R.id.skills_editText, characterModel.getSkills(), characterModel::setSkills);
        setupField(rootView, R.id.attack_routine_editText, characterModel.getAttackRoutine(), characterModel::setAttackRoutine);
        setupField(rootView, R.id.ac_editText, characterModel.getAc(), characterModel::setAc);
        setupField(rootView, R.id.saves_editText, characterModel.getSaves(), characterModel::setSaves);
        setupField(rootView, R.id.notes_editText, characterModel.getCharacterNotes(), characterModel::setCharacterNotes);

        // Special case for max dying value with validation toast
        setupField(rootView, R.id.max_dying_editText, String.valueOf(characterModel.getMaxDyingValue()), text -> {
            int parsedValue = EditTextWatcher.parseInt(text);
            if (parsedValue > AppConstants.MAX_DYING_VALUE) {
                if (maxDyingInvalidToast != null) {
                    maxDyingInvalidToast.cancel();
                }
                maxDyingInvalidToast = Toast.makeText(requireContext(),
                        getString(R.string.max_dying_value_invalid, AppConstants.MAX_DYING_VALUE),
                        Toast.LENGTH_SHORT);
                maxDyingInvalidToast.show();
            }
            characterModel.setMaxDyingValue(Math.min(parsedValue, AppConstants.MAX_DYING_VALUE));
        });

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
        clearAllDebuffButton.setOnClickListener(view -> {
            adapter.removeAll();
            characterModel.getDebuffList().clear();
        });

        Button addDebuffButton = rootView.findViewById(R.id.add_debuff_button);
        addDebuffButton.setOnClickListener(view -> ((MainActivity) requireActivity()).replaceDebuffWizardFragment(position));

        pcSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            characterModel.setIsPC(isChecked);
            app.getInitiativeAdapter().notifyCharacterChanged(characterModel);
        });

        isDyingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            characterModel.setIsDying(isChecked);
            app.getInitiativeAdapter().notifyCharacterChanged(characterModel);
        });

        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Private helpers
    ////////////////////////////////////////////////////////////////////////////////////////////////    /**
     * Binds an EditText to a string model field with automatic watcher cleanup.
     */
    private void setupField(View rootView, int viewId, String value, Consumer<String> callback) {
        EditText editText = rootView.findViewById(viewId);
        editText.setText(value);

        // Remove old watcher if it exists to avoid double watchers
        Object oldWatcher = editText.getTag();
        if (oldWatcher instanceof EditTextWatcher) {
            editText.removeTextChangedListener((EditTextWatcher) oldWatcher);
        }

        // Add new watcher
        EditTextWatcher newWatcher = new EditTextWatcher(callback);
        editText.setTag(newWatcher);
        editText.addTextChangedListener(newWatcher);
    }

    /**
     * Binds an EditText to an integer model field.
     */
    private void setupIntField(View rootView, int viewId, int value, Consumer<Integer> callback) {
        setupField(rootView, viewId, String.valueOf(value), text -> callback.accept(EditTextWatcher.parseInt(text)));
    }
}