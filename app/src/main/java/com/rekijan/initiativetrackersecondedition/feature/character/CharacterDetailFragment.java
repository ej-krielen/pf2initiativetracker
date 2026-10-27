package com.rekijan.initiativetrackersecondedition.feature.character;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.app.CombatTrackerApp;
import com.rekijan.initiativetrackersecondedition.app.MainActivity;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;

/**
 * The details of one character in the GM's encounter: the name, then the sheet with the Stats and
 * Conditions tabs ({@link CharacterSheet}): PC or not, dying or not, initiative bonus, max hp, fast
 * healing, regeneration, the dying track (dying, max dying, wounded, doomed, recovery DC), hero
 * points, the free-text stat lines, notes and the conditions. On a phone it replaces the list; on
 * a tablet it is shown next to it.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class CharacterDetailFragment extends Fragment {

    private static final String ARG_POSITION = "POSITION";
    private static final String STATE_TAB = "sheet_tab";

    private CharacterSheet sheet;
    private int tab = CharacterSheet.TAB_STATS;

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
        CharacterModel character = encounter.get(position);

        TextView titleTextView = view.findViewById(R.id.character_detail_textView);
        titleTextView.setText(getString(R.string.character_details_name_title, character.getCharacterName()));

        sheet = new CharacterSheet(view.findViewById(R.id.character_sheet), false, encounter::notifyChanged,
                () -> ((MainActivity) requireActivity()).showConditionWizard(encounter.indexOf(character)));
        sheet.bind(character);
        if (savedInstanceState != null) tab = savedInstanceState.getInt(STATE_TAB, tab);
        sheet.selectTab(tab);
    }

    /** Back from the condition wizard, the same tab is open again. */
    @Override
    public void onDestroyView() {
        if (sheet != null) tab = sheet.getSelectedTab();
        sheet = null;
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, sheet != null ? sheet.getSelectedTab() : tab);
    }
}
