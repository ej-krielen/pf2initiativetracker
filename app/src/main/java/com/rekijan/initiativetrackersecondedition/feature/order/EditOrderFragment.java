package com.rekijan.initiativetrackersecondedition.feature.order;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.app.CombatTrackerApp;
import com.rekijan.initiativetrackersecondedition.core.util.RecyclerLongPressListener;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;

/**
 * Screen to change the initiative order by hand without sorting: drag a character by its handle.
 * Long press a character to make them first in round.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 24-3-2021
 */
public class EditOrderFragment extends Fragment implements Encounter.Listener {

    private Encounter encounter;
    private EditOrderAdapter adapter;
    private boolean dragging;

    public static EditOrderFragment newInstance() {
        return new EditOrderFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_order, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        encounter = ((CombatTrackerApp) requireActivity().getApplication()).getEncounter();

        RecyclerView recyclerView = view.findViewById(R.id.edit_order_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder from,
                                  @NonNull RecyclerView.ViewHolder to) {
                dragging = true;
                int fromPosition = from.getBindingAdapterPosition();
                int toPosition = to.getBindingAdapterPosition();
                encounter.move(fromPosition, toPosition);
                adapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(rv, viewHolder);
                dragging = false;
                encounter.notifyChanged();
            }

            @Override
            public boolean isLongPressDragEnabled() {
                // Long press marks first in round; dragging uses the handle
                return false;
            }
        });
        adapter = new EditOrderAdapter(encounter, touchHelper::startDrag);
        recyclerView.setAdapter(adapter);
        touchHelper.attachToRecyclerView(recyclerView);
        recyclerView.addOnItemTouchListener(new RecyclerLongPressListener(requireContext(), recyclerView,
                encounter::setFirstInRound));

        encounter.addListener(this);
    }

    @Override
    public void onDestroyView() {
        encounter.removeListener(this);
        super.onDestroyView();
    }

    @SuppressLint("NotifyDataSetChanged") // The first-in-round marker can move to any row
    @Override
    public void onEncounterChanged() {
        // While dragging the rows are moved one by one; a full redraw would end the drag
        if (!dragging) adapter.notifyDataSetChanged();
    }
}
