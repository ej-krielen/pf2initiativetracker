package com.rekijan.initiativetrackersecondedition.feature.player;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.app.CombatTrackerApp;
import com.rekijan.initiativetrackersecondedition.app.MainActivity;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterSheet;

/**
 * A player's own character, like Initiative Tracker's player screen: the sheet with the Stats and
 * Conditions tabs ({@link CharacterSheet}), with name, initiative and hp. Every edit is saved on
 * the phone straight away. Add opens the condition wizard for this character. Table sync adds the
 * session card (Join) and Send to GM later.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public class PlayerFragment extends Fragment {

    private static final String ARG_ID = "PLAYER_ID";
    private static final String STATE_TAB = "sheet_tab";

    private CharacterSheet sheet;
    private int tab = CharacterSheet.TAB_STATS;

    public static PlayerFragment newInstance(String characterId) {
        PlayerFragment fragment = new PlayerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, characterId);
        fragment.setArguments(args);
        return fragment;
    }

    /** @return The id of the player character shown here. */
    public String getCharacterId() {
        return requireArguments().getString(ARG_ID);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        PlayerBook players = ((CombatTrackerApp) requireActivity().getApplication()).getPlayers();
        String id = getCharacterId();
        // Loaded fresh every time, so a condition added in the wizard shows up
        CharacterModel character = players.load(id);

        sheet = new CharacterSheet(view.findViewById(R.id.player_sheet), true,
                () -> {
                    players.save(id, character);
                    ((MainActivity) requireActivity()).onPlayerChanged();
                },
                () -> ((MainActivity) requireActivity()).showPlayerConditionWizard(id));
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
