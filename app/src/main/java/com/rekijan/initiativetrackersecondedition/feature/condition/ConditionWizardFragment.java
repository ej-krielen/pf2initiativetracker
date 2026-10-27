package com.rekijan.initiativetrackersecondedition.feature.condition;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.app.CombatTrackerApp;
import com.rekijan.initiativetrackersecondedition.core.data.EncounterStore;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.DebuffModel;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;

/**
 * Adds a (de)buff to a character: a standard condition from the searchable list, an empty one to
 * fill in, or persistent damage. Goes back to the character after adding.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class ConditionWizardFragment extends Fragment {

    private static final String ARG_POSITION = "POSITION";

    public static ConditionWizardFragment newInstance(int position) {
        ConditionWizardFragment fragment = new ConditionWizardFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_condition_wizard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CombatTrackerApp app = (CombatTrackerApp) requireActivity().getApplication();
        Encounter encounter = app.getEncounter();
        EncounterStore store = app.getStore();
        int position = requireArguments().getInt(ARG_POSITION);
        if (!encounter.isValidPosition(position)) {
            // The character was removed (e.g. before a rotation); show nothing
            view.setVisibility(View.INVISIBLE);
            return;
        }
        CharacterModel character = encounter.get(position);

        RecyclerView recyclerView = view.findViewById(R.id.debuff_wizard_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        ConditionWizardAdapter adapter = new ConditionWizardAdapter(
                ConditionProvider.standardConditions(requireContext()), store.isHideDescriptions(),
                debuff -> add(encounter, character, debuff,
                        getString(R.string.debuff_added_existing, debuff.getDisplayName())));
        recyclerView.setAdapter(adapter);

        EditText searchEditText = view.findViewById(R.id.debuff_wizard_searchView);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        CompoundButton hideDescriptionsCheckBox = view.findViewById(R.id.debuff_wizard_description_checkBox);
        hideDescriptionsCheckBox.setChecked(store.isHideDescriptions());
        hideDescriptionsCheckBox.setOnCheckedChangeListener((button, checked) -> {
            store.setHideDescriptions(checked);
            adapter.setHideDescriptions(checked);
        });

        view.findViewById(R.id.debuff_wizard_add_empty_button).setOnClickListener(v ->
                add(encounter, character, new DebuffModel(), getString(R.string.debuff_added_empty)));
        // Poisons are planned for a later release
        view.findViewById(R.id.debuff_wizard_add_poison_button).setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.poison_not_implemented, Toast.LENGTH_LONG).show());
        view.findViewById(R.id.debuff_wizard_add_persistent_damage_button).setOnClickListener(v ->
                PersistentDamageDialogs.create(requireContext(), debuff -> add(encounter, character, debuff,
                        getString(R.string.debuff_added_persistent_damage))));
    }

    private void add(Encounter encounter, CharacterModel character, DebuffModel debuff, String message) {
        character.getDebuffList().add(debuff);
        encounter.notifyChanged();
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        getParentFragmentManager().popBackStack();
    }
}
