package com.rekijan.initiativetrackersecondedition.core;

import static com.rekijan.initiativetrackersecondedition.core.AppConstants.ROUND_COUNTER;
import static com.rekijan.initiativetrackersecondedition.core.AppConstants.SHARED_PREF_TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.common.helper.DialogHelper;
import com.rekijan.initiativetrackersecondedition.common.listeners.RecyclerTouchListener;
import com.rekijan.initiativetrackersecondedition.features.health.HitPointDialogHelper;
import com.rekijan.initiativetrackersecondedition.features.initiative.EncounterActionHandler;
import com.rekijan.initiativetrackersecondedition.features.initiative.InitiativeAdapter;
import com.rekijan.initiativetrackersecondedition.features.initiative.InitiativeManager;
import com.rekijan.initiativetrackersecondedition.features.initiative.RoundController;

/**
 * A fragment containing the main initiative list and encounter controls.
 * Acts as the primary screen where users track combat rounds and character turns.
 */
public class MainActivityFragment extends Fragment {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private RoundController roundController;    private EncounterActionHandler actionHandler;

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Constructor    ////////////////////////////////////////////////////////////////////////////////////////////////    public MainActivityFragment() {}
    /**
     * @return A new instance of MainActivityFragment.
     */
    public static MainActivityFragment newInstance() { return new MainActivityFragment(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Override methods    ////////////////////////////////////////////////////////////////////////////////////////////////    @Override    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        //Round counter setup
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
        int initialRound = sharedPreferences.getInt(ROUND_COUNTER, 1);
        roundController = new RoundController(initialRound, rootView.findViewById(R.id.round_counter_textView));

        Button plusButton = rootView.findViewById(R.id.up_edit_order_button);
        plusButton.setOnClickListener(view -> roundController.changeRound(1));

        Button minusButton = rootView.findViewById(R.id.minus_round_button);
        minusButton.setOnClickListener(view -> roundController.changeRound(-1));

        Button resetButton = rootView.findViewById(R.id.reset_round_button);
        resetButton.setOnClickListener(view -> roundController.resetRound(1));

        Button editOrderButton = rootView.findViewById(R.id.edit_order_button);
        editOrderButton.setOnClickListener(v -> ((MainActivity)requireActivity()).replaceEditOrderFragment());

        //Setup RecyclerView
        RecyclerView initiativeRecyclerView = rootView.findViewById(R.id.initiative_recyclerView);
        initiativeRecyclerView.setHasFixedSize(true);
        initiativeRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        AppExtension app = (AppExtension) requireActivity().getApplicationContext();
        final InitiativeAdapter adapter = app.getInitiativeAdapter();
        final InitiativeManager manager = app.getInitiativeManager();
        initiativeRecyclerView.setAdapter(adapter);

        //Setup action handler
        actionHandler = new EncounterActionHandler(requireActivity(), manager, adapter);

        //Setup touch listener
        initiativeRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(),
                initiativeRecyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) { }

            @Override
            public void onLongClick(View view, int position) {
                actionHandler.confirmDeleteCharacter(position);
            }
        }));
        return rootView;
    }

    @Override
    public void onPause() {
        AppExtension app = (AppExtension) requireActivity().getApplicationContext();
        app.saveData(roundController.getRoundCounter());
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        AppExtension app = (AppExtension) requireActivity().getApplicationContext();
        savedInstanceState.putParcelableArrayList("characters", app.getInitiativeManager().getCharacters());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        AppExtension app = (AppExtension) requireActivity().getApplicationContext();
        menu.findItem(R.id.action_settings_hide_reactions).setChecked(app.getHideReactions());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        AppExtension app = (AppExtension) requireActivity().getApplicationContext();
        InitiativeManager manager = app.getInitiativeManager();
        InitiativeAdapter adapter = app.getInitiativeAdapter();
        int itemId = item.getItemId();

        if (itemId == R.id.action_settings_next_turn) {
            if (manager.nextTurn(requireActivity())) {
                roundController.changeRound(1);
            }
            adapter.notifyItemRangeChanged(0, manager.getCharacters().size());
            return true;
        } else if (itemId == R.id.action_settings_sort) {
            actionHandler.askSortRoundResetConfirmation(() -> roundController.resetRound(1));
            return true;
        } else if (itemId == R.id.action_settings_add_character) {
            actionHandler.addCharacter();
            return true;
        } else if (itemId == R.id.action_settings_delete_character) {
            DialogHelper.getInstance().simpleDialog(requireActivity(), getString(R.string.dialog_delete_info_title), getString(R.string.dialog_delete_info));
            return true;
        } else if (itemId == R.id.action_settings_set_pcs_max_hp) {
            HitPointDialogHelper.getInstance().setPCsToMaxHP(app);
            return true;
        } else if (itemId == R.id.action_settings_hide_reactions) {
            boolean newState = !item.isChecked();
            item.setChecked(newState);
            app.setHideReactions(newState);
            return true;
        } else if (itemId == R.id.action_settings_start_turn_info) {
            DialogHelper.getInstance().simpleDialog(requireActivity(), getString(R.string.dialog_turn_start_info_title), getString(R.string.dialog_turn_start_info));
            return true;
        } else if (itemId == R.id.action_settings_end_turn_info) {
            DialogHelper.getInstance().simpleDialog(requireActivity(), getString(R.string.dialog_turn_end_info_title), getString(R.string.dialog_turn_end_info));
            return true;
        } else if (itemId == R.id.action_settings_about) {
            CommonDialogHelper.getInstance().aboutInfo(requireActivity());
            return true;
        } else if (itemId == R.id.action_settings_privacy) {
            CommonDialogHelper.getInstance().privacyPolicyInfo(requireActivity());
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}