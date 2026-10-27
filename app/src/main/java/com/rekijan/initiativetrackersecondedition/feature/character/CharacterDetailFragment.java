package com.rekijan.initiativetrackersecondedition.feature.character;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.app.CombatTrackerApp;
import com.rekijan.initiativetrackersecondedition.app.MainActivity;
import com.rekijan.initiativetrackersecondedition.core.util.NumberInput;
import com.rekijan.initiativetrackersecondedition.core.util.TextBinding;
import com.rekijan.initiativetrackersecondedition.feature.condition.DebuffAdapter;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The details of one character: PC or not, dying or not, initiative bonus, max hp, fast healing,
 * regeneration, the dying track (dying, max dying, wounded, doomed, recovery DC), hero points,
 * the free-text stat lines, notes and the (de)buffs. On a phone it replaces the list; on a
 * tablet it is shown next to it.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class CharacterDetailFragment extends Fragment {

    private static final String ARG_POSITION = "POSITION";

    private CharacterModel character;
    private Toast maxDyingToast;

    public static CharacterDetailFragment newInstance(int position) {
        CharacterDetailFragment fragment = new CharacterDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_character_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Encounter encounter = ((CombatTrackerApp) requireActivity().getApplication()).getEncounter();
        int position = requireArguments().getInt(ARG_POSITION);
        if (!encounter.isValidPosition(position)) {
            // The character was removed (e.g. before a rotation); show nothing
            view.setVisibility(View.INVISIBLE);
            return;
        }
        character = encounter.get(position);

        TextView titleTextView = view.findViewById(R.id.character_detail_textView);
        titleTextView.setText(getString(R.string.character_details_name_title, character.getCharacterName()));

        CompoundButton pcSwitch = view.findViewById(R.id.is_pc_switch);
        pcSwitch.setChecked(character.isPC());
        pcSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            character.setPC(isChecked);
            encounter.notifyChanged();
        });
        CompoundButton dyingSwitch = view.findViewById(R.id.is_dying_switch);
        dyingSwitch.setChecked(character.isDying());
        dyingSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            character.setDying(isChecked);
            encounter.notifyChanged();
        });

        bindInt(view, R.id.initiative_bonus_editText, character.getInitiativeBonus(),
                CharacterModel.MIN_INITIATIVE, CharacterModel.MAX_INITIATIVE, character::setInitiativeBonus);
        bindInt(view, R.id.max_hp_editText, character.getMaxHp(), 0, CharacterModel.MAX_HP, character::setMaxHp);
        bindInt(view, R.id.fast_healing_editText, character.getFastHealing(),
                0, CharacterModel.MAX_HEALING, character::setFastHealing);
        bindInt(view, R.id.regeneration_editText, character.getRegeneration(),
                0, CharacterModel.MAX_HEALING, character::setRegeneration);
        bindInt(view, R.id.dying_editText, character.getDyingValue(),
                0, CharacterModel.MAX_CONDITION_VALUE, character::setDyingValue);
        bindInt(view, R.id.wounded_editText, character.getWoundedValue(),
                0, CharacterModel.MAX_CONDITION_VALUE, character::setWoundedValue);
        bindInt(view, R.id.doomed_editText, character.getDoomedValue(),
                0, CharacterModel.MAX_CONDITION_VALUE, character::setDoomedValue);
        bindInt(view, R.id.recovery_dc_editText, character.getRecoveryDC(),
                0, CharacterModel.MAX_RECOVERY_DC, character::setRecoveryDC);
        bindInt(view, R.id.hero_points_editText, character.getHeroPoints(),
                0, CharacterModel.MAX_HERO_POINTS, character::setHeroPoints);
        TextBinding.bindText(view.findViewById(R.id.max_dying_editText), String.valueOf(character.getMaxDyingValue()),
                this::onMaxDyingChanged);
        bindText(view, R.id.skills_editText, character.getSkills(), character::setSkills);
        bindText(view, R.id.attack_routine_editText, character.getAttackRoutine(), character::setAttackRoutine);
        bindText(view, R.id.ac_editText, character.getAc(), character::setAc);
        bindText(view, R.id.saves_editText, character.getSaves(), character::setSaves);
        bindText(view, R.id.notes_editText, character.getCharacterNotes(), character::setCharacterNotes);

        RecyclerView debuffRecyclerView = view.findViewById(R.id.debuff_recyclerView);
        debuffRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        DebuffAdapter adapter = new DebuffAdapter(character, encounter::notifyChanged);
        debuffRecyclerView.setAdapter(adapter);

        view.findViewById(R.id.clear_all_debuffs_button).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View v) {
                character.getDebuffList().clear();
                adapter.notifyDataSetChanged();
                encounter.notifyChanged();
            }
        });
        view.findViewById(R.id.add_debuff_button).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showConditionWizard(encounter.indexOf(character)));
    }

    /** Max dying above {@link CharacterModel#MAX_DYING_VALUE} is capped, with a short explanation. */
    private void onMaxDyingChanged(String text) {
        int value = NumberInput.parseInt(text, CharacterModel.DEFAULT_MAX_DYING, 1, Integer.MAX_VALUE);
        if (value > CharacterModel.MAX_DYING_VALUE) {
            if (maxDyingToast != null) maxDyingToast.cancel();
            maxDyingToast = Toast.makeText(requireContext(),
                    getString(R.string.max_dying_value_invalid, CharacterModel.MAX_DYING_VALUE), Toast.LENGTH_SHORT);
            maxDyingToast.show();
        }
        character.setMaxDyingValue(value);
    }

    private static void bindInt(View root, int id, int value, int min, int max, IntConsumer onChanged) {
        EditText editText = root.findViewById(id);
        TextBinding.bindInt(editText, value, min, max, onChanged);
    }

    private static void bindText(View root, int id, String value, Consumer<String> onChanged) {
        EditText editText = root.findViewById(id);
        TextBinding.bindText(editText, value, onChanged);
    }
}
