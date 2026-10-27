package com.rekijan.initiativetrackersecondedition.feature.order;

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
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;

import java.util.function.Consumer;

/**
 * A compact version of the initiative order to fix it by hand: drag a character by its handle,
 * and a marker on the one who is first in round.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 24-3-2021
 */
public class EditOrderAdapter extends RecyclerView.Adapter<EditOrderAdapter.OrderViewHolder> {

    private final Encounter encounter;
    private final Consumer<RecyclerView.ViewHolder> onStartDrag;

    /**
     * @param onStartDrag Called when the user touches a drag handle.
     */
    public EditOrderAdapter(Encounter encounter, Consumer<RecyclerView.ViewHolder> onStartDrag) {
        this.encounter = encounter;
        this.onStartDrag = onStartDrag;
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        final CardView cardView;
        final TextView initiativeTextView;
        final TextView nameTextView;
        final ImageView dragHandle;
        final ImageView firstInRoundImage;

        OrderViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.edit_order_cardView);
            initiativeTextView = itemView.findViewById(R.id.edit_order_initiative_textView);
            nameTextView = itemView.findViewById(R.id.edit_order_name_textView);
            dragHandle = itemView.findViewById(R.id.drag_handle_imageView);
            firstInRoundImage = itemView.findViewById(R.id.first_in_round_imageView);
        }
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_edit_order, parent, false);
        return new OrderViewHolder(v);
    }

    @SuppressLint("ClickableViewAccessibility") // The handle only starts a drag, there is no click
    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        CharacterModel character = encounter.get(position);

        int color = ContextCompat.getColor(context, character.isPC() ? R.color.pcColor : R.color.npcColor);
        holder.cardView.setCardBackgroundColor(color);
        holder.initiativeTextView.setText(String.valueOf(character.getInitiative()));
        holder.nameTextView.setText(character.getCharacterName());
        holder.firstInRoundImage.setVisibility(character.isFirstInRound() ? View.VISIBLE : View.INVISIBLE);

        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) onStartDrag.accept(holder);
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return encounter.size();
    }
}
