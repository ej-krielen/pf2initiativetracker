package com.rekijan.initiativetrackersecondedition.character.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.character.model.CharacterModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Custom RecyclerView.Adapter for the CharacterModel class, updated for drag and drop
 */
public class EditOrderAdapter extends RecyclerView.Adapter<EditOrderAdapter.CharacterViewHolder> {

    private final ArrayList<CharacterModel> characters = new ArrayList<>();
    private final Context context;
    private final OnStartDragListener dragListener;

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public EditOrderAdapter(Context context, OnStartDragListener dragListener) {
        this.context = context;
        this.dragListener = dragListener;
    }

    public void addAll(List<CharacterModel> characters) {
        this.characters.clear();
        this.characters.addAll(characters);
        notifyDataSetChanged();
    }

    public ArrayList<CharacterModel> getList() {
        return characters;
    }

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

    @NonNull
    @Override
    public CharacterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_edit_order, parent, false);
        return new CharacterViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final CharacterViewHolder holder, int position) {
        final CharacterModel character = characters.get(position);

        int color = character.isPC() ? context.getResources().getColor(R.color.pcColor) : context.getResources().getColor(R.color.npcColor);
        holder.editOrderCardView.setCardBackgroundColor(color);

        holder.editOrderInitiativeTextView.setText(String.valueOf(character.getInitiative()));
        holder.editOrderNameTextView.setText(character.getCharacterName());
        holder.firstInRoundImage.setVisibility(character.isFirstRound() ? View.VISIBLE : View.INVISIBLE);

        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                dragListener.onStartDrag(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return characters.size();
    }
}
