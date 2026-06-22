package com.rekijan.initiativetrackersecondedition.features.editorder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Custom RecyclerView.Adapter for reordering characters in the initiative list.
 * Supports manual drag-and-drop interactions.
 */
public class EditOrderAdapter extends RecyclerView.Adapter<EditOrderAdapter.CharacterViewHolder> {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private final ArrayList<CharacterModel> characters = new ArrayList<>();    private final Context context;
    private final OnStartDragListener dragListener;

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Interfaces    ////////////////////////////////////////////////////////////////////////////////////////////////    /**     * Interface for communicating drag events back to the fragment.
     */
    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Constructor    ////////////////////////////////////////////////////////////////////////////////////////////////    /**     * @param context      Android context for resource access.
     * @param dragListener Listener for starting manual drag operations.
     */
    public EditOrderAdapter(Context context, OnStartDragListener dragListener) {
        this.context = context;
        this.dragListener = dragListener;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods    ////////////////////////////////////////////////////////////////////////////////////////////////    /**     * Swaps character positions in the local list and notifies the UI of the change.
     */
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(characters, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(characters, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Public helpers    ////////////////////////////////////////////////////////////////////////////////////////////////    /**     * Replaces the entire local list with new characters.
     */
    public void addAll(List<CharacterModel> characters) {
        this.characters.clear();
        this.characters.addAll(characters);
        notifyItemRangeChanged(0, this.characters.size());
    }

    /**
     * @return The local list of characters.
     */
    public ArrayList<CharacterModel> getList() {
        return characters;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// ViewHolder region    ////////////////////////////////////////////////////////////////////////////////////////////////    /**     * ViewHolder for characters in the reordering screen.
     */
    public static class CharacterViewHolder extends RecyclerView.ViewHolder {
        CardView editOrderCardView;
        TextView editOrderInitiativeTextView;
        TextView editOrderNameTextView;
        ImageView dragHandle;
        ImageView firstInRoundImage;

        CharacterViewHolder(View itemView) {
            super(itemView);
            editOrderCardView = itemView.findViewById(R.id.edit_order_cardView);
            editOrderInitiativeTextView = itemView.findViewById(R.id.edit_order_initiative_textView);
            editOrderNameTextView = itemView.findViewById(R.id.edit_order_name_textView);
            dragHandle = itemView.findViewById(R.id.drag_handle_imageView);
            firstInRoundImage = itemView.findViewById(R.id.first_in_round_imageView);
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// End of ViewHolder region    ////////////////////////////////////////////////////////////////////////////////////////////////    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Override methods    ////////////////////////////////////////////////////////////////////////////////////////////////    @NonNull    @Override
    public CharacterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_edit_order, parent, false);
        return new CharacterViewHolder(v);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final CharacterViewHolder holder, int position) {
        final CharacterModel character = characters.get(position);

        int color = character.isPC() ? ContextCompat.getColor(context, R.color.pcColor) : ContextCompat.getColor(context, R.color.npcColor);
        holder.editOrderCardView.setCardBackgroundColor(color);

        holder.editOrderInitiativeTextView.setText(String.valueOf(character.getInitiative()));
        holder.editOrderNameTextView.setText(character.getCharacterName());
        holder.firstInRoundImage.setVisibility(character.isFirstRound() ? View.VISIBLE : View.INVISIBLE);

        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                dragListener.onStartDrag(holder);
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return characters.size();
    }
}