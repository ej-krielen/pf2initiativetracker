package com.rekijan.initiativetrackersecondedition.features.editorder;

import android.content.Context;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.core.AppExtension;
import com.rekijan.initiativetrackersecondedition.R;

/**
 * A fragment that allows users to manually reorder the initiative list.
 * Uses ItemTouchHelper to provide a drag-and-drop interface for fine-tuning turn order.
 */
public class EditOrderFragment extends Fragment implements EditOrderAdapter.OnStartDragListener {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private ItemTouchHelper itemTouchHelper;
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Constructor    ////////////////////////////////////////////////////////////////////////////////////////////////    public EditOrderFragment() {}
    /**
     * @return A new instance of EditOrderFragment.
     */
    public static EditOrderFragment newInstance() {
        return new EditOrderFragment();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Override methods    ////////////////////////////////////////////////////////////////////////////////////////////////    @Override    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater){
        if (!getResources().getBoolean(R.bool.isTablet)) {
            menu.clear();
            inflater.inflate(R.menu.menu_minimum, menu);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_edit_order, container, false);

        RecyclerView editOrderRecyclerView = rootView.findViewById(R.id.edit_order_recyclerView);
        editOrderRecyclerView.setHasFixedSize(true);
        editOrderRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        final AppExtension app = (AppExtension) requireActivity().getApplicationContext();
        final EditOrderAdapter adapter = new EditOrderAdapter(requireActivity(), this);
        adapter.addAll(app.getInitiativeManager().getCharacters());
        editOrderRecyclerView.setAdapter(adapter);

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                adapter.onItemMove(viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                app.getInitiativeManager().getCharacters().clear();
                app.getInitiativeManager().getCharacters().addAll(adapter.getList());
                app.getInitiativeAdapter().notifyItemRangeChanged(0, app.getInitiativeManager().getCharacters().size());
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }
        });
        itemTouchHelper.attachToRecyclerView(editOrderRecyclerView);

        editOrderRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), editOrderRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
            }

            @Override
            public void onLongClick(View view, int position) {
                app.getInitiativeManager().setFirstInRound(position);
                adapter.notifyItemChanged(position);
            }
        }));

        return rootView;
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }

    /**
     * Interface for recyclerview item interactions.
     */
    public interface ClickListener {
        void onClick(View view, int position);
        void onLongClick(View view, int position);
    }

    /**
     * Touch listener implementation for the initiative list.
     */
    public static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {
        private final GestureDetector gestureDetector;
        private final ClickListener clickListener;

        public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final ClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(@NonNull MotionEvent e) {
                    return true;
                }
                @Override
                public void onLongPress(@NonNull MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildAdapterPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) { }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) { }
    }
}