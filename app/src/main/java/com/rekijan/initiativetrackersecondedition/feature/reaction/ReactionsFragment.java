package com.rekijan.initiativetrackersecondedition.feature.reaction;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.app.CombatTrackerApp;
import com.rekijan.initiativetrackersecondedition.feature.character.CharacterModel;
import com.rekijan.initiativetrackersecondedition.feature.character.ReactionModel;
import com.rekijan.initiativetrackersecondedition.feature.encounter.Encounter;

/**
 * The custom reactions of one character (next to the standard reaction on the card). They are
 * available again at the start of the character's turn.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 1-6-2019
 */
public class ReactionsFragment extends Fragment {

    private static final String ARG_POSITION = "POSITION";

    public static ReactionsFragment newInstance(int position) {
        ReactionsFragment fragment = new ReactionsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reactions, container, false);
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

        TextView titleTextView = view.findViewById(R.id.reactions_character_detail_textView);
        titleTextView.setText(getString(R.string.reaction_details_name_title, character.getCharacterName()));

        RecyclerView recyclerView = view.findViewById(R.id.reactions_details_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        ReactionAdapter adapter = new ReactionAdapter(character, encounter::notifyChanged);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.clear_all_reactions_button).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View v) {
                character.getReactionList().clear();
                adapter.notifyDataSetChanged();
                encounter.notifyChanged();
            }
        });
        view.findViewById(R.id.add_reaction_button).setOnClickListener(v -> {
            character.getReactionList().add(new ReactionModel());
            adapter.notifyItemInserted(character.getReactionList().size() - 1);
            encounter.notifyChanged();
        });
    }
}
