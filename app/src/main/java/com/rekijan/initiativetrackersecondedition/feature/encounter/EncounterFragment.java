package com.rekijan.initiativetrackersecondedition.feature.encounter;

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.app.CombatTrackerApp;
import com.rekijan.initiativetrackersecondedition.app.MainActivity;
import com.rekijan.initiativetrackersecondedition.core.data.EncounterStore;
import com.rekijan.initiativetrackersecondedition.core.util.Dialogs;
import com.rekijan.initiativetrackersecondedition.core.util.RecyclerLongPressListener;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.sync.SyncManager;
import com.rekijan.initiativetrackersecondedition.feature.sync.SyncRules;

import java.util.Collections;

/**
 * The main screen: the initiative order and the turn bar (round, Edit order, Next turn). Long
 * press a card to remove that character; a synced PC can also be unlinked there, and its player is
 * told. The menu actions (sort, add, ...) are handled by
 * {@link MainActivity}.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 17-9-2015
 */
public class EncounterFragment extends Fragment implements Encounter.Listener, SyncManager.StateListener {

    private Encounter encounter;
    private SyncManager sync;
    private CharacterAdapter adapter;
    private TextView roundTextView;

    public static EncounterFragment newInstance() {
        return new EncounterFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_encounter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CombatTrackerApp app = (CombatTrackerApp) requireActivity().getApplication();
        encounter = app.getEncounter();
        sync = app.getSync();
        EncounterStore store = app.getStore();
        MainActivity activity = (MainActivity) requireActivity();

        roundTextView = view.findViewById(R.id.round_counter_textView);
        view.findViewById(R.id.plus_round_button).setOnClickListener(v -> encounter.nextRound());
        view.findViewById(R.id.minus_round_button).setOnClickListener(v -> encounter.previousRound());
        MaterialButton nextTurnButton = view.findViewById(R.id.next_turn_button);
        nextTurnButton.setOnClickListener(v -> activity.nextTurn());
        dropIconWhenNarrow(nextTurnButton);
        view.findViewById(R.id.edit_order_button).setOnClickListener(v -> activity.showEditOrder());

        RecyclerView recyclerView = view.findViewById(R.id.initiative_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CharacterAdapter(encounter, new CharacterAdapter.Navigation() {
            @Override
            public void showDetails(int position) {
                activity.showCharacterDetail(position);
            }

            @Override
            public void showReactions(int position) {
                activity.showReactions(position);
            }
        }, store::isHideReactions, sync::getStatus);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnItemTouchListener(new RecyclerLongPressListener(requireContext(), recyclerView,
                this::askRemoveCharacter));

        encounter.addListener(this);
        sync.addStateListener(this);
        onEncounterChanged();
    }

    @Override
    public void onDestroyView() {
        encounter.removeListener(this);
        sync.removeStateListener(this);
        super.onDestroyView();
    }

    @SuppressLint("NotifyDataSetChanged") // Turns rotate the whole list
    @Override
    public void onEncounterChanged() {
        roundTextView.setText(String.valueOf(encounter.getRound()));
        adapter.notifyDataSetChanged();
    }

    /** Players' sheets came or went: the Synced and Player left tags change. */
    @SuppressLint("NotifyDataSetChanged") // Any card's tag may change
    @Override
    public void onSyncStateChanged() {
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    /**
     * On a narrow screen or at a large font size "Next turn" does not fit next to its icon, even
     * at the smallest autosize text size. Then the button drops the icon rather than the word.
     */
    private static void dropIconWhenNarrow(MaterialButton button) {
        Drawable icon = button.getIcon();
        if (icon == null) return;
        button.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int width = right - left;
            if (width == oldRight - oldLeft) return;
            Paint paint = new Paint(button.getPaint());
            paint.setTextSize(TextViewCompat.getAutoSizeMinTextSize(button));
            float textWidth = paint.measureText(button.getText().toString());
            int room = width - button.getPaddingStart() - button.getPaddingEnd()
                    - button.getIconSize() - button.getIconPadding();
            Drawable wanted = textWidth <= room ? icon : null;
            if (button.getIcon() != wanted) button.post(() -> button.setIcon(wanted));
        });
    }

    private void askRemoveCharacter(int position) {
        if (!encounter.isValidPosition(position)) return;
        CharacterModel character = encounter.get(position);
        String name = character.getCharacterName();
        if (TextUtils.isEmpty(name)) name = getString(R.string.empty_character_name);
        if (sync.getStatus(character) != SyncRules.Status.NONE) {
            Dialogs.builder(requireContext())
                    .setTitle(getString(R.string.sync_remove_title, name))
                    .setMessage(getString(R.string.sync_remove_message, name))
                    .setPositiveButton(R.string.sync_remove, (d, w) ->
                            sync.removeCharacters(Collections.singletonList(character)))
                    .setNeutralButton(R.string.sync_keep_unlink, (d, w) -> sync.unlink(character))
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
            return;
        }
        Dialogs.builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete) + " " + name + "?")
                .setPositiveButton(R.string.dialog_ok, (d, w) ->
                        sync.removeCharacters(Collections.singletonList(character)))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}
