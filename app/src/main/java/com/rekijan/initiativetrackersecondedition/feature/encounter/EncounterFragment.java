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

/**
 * The main screen: the initiative order and the turn bar (round, Edit order, Next turn). Long
 * press a card to remove that character. The menu actions (sort, add, ...) are handled by
 * {@link MainActivity}.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 17-9-2015
 */
public class EncounterFragment extends Fragment implements Encounter.Listener {

    private Encounter encounter;
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
        }, store::isHideReactions);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnItemTouchListener(new RecyclerLongPressListener(requireContext(), recyclerView,
                this::askRemoveCharacter));

        encounter.addListener(this);
        onEncounterChanged();
    }

    @Override
    public void onDestroyView() {
        encounter.removeListener(this);
        super.onDestroyView();
    }

    @SuppressLint("NotifyDataSetChanged") // Turns rotate the whole list
    @Override
    public void onEncounterChanged() {
        roundTextView.setText(String.valueOf(encounter.getRound()));
        adapter.notifyDataSetChanged();
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
        String name = encounter.get(position).getCharacterName();
        if (TextUtils.isEmpty(name)) name = getString(R.string.empty_character_name);
        Dialogs.builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete) + " " + name + "?")
                .setPositiveButton(R.string.dialog_ok, (d, w) -> encounter.remove(position))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}
