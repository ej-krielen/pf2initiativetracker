package com.rekijan.initiativetrackersecondedition.features.reactions;

import static com.rekijan.initiativetrackersecondedition.core.AppConstants.POSITION;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.AppExtension;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.data.model.ReactionModel;

import java.util.ArrayList;

/**
 * A fragment that allows users to manage a character's reactions.
 * Users can add new reactions, remove them, or edit their details.
 */
public class ReactionsDetailFragment extends Fragment {

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private int position;
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Constructor    ////////////////////////////////////////////////////////////////////////////////////////////////    public ReactionsDetailFragment() {}
    public static ReactionsDetailFragment newInstance(int position) {
        ReactionsDetailFragment fragment =  new ReactionsDetailFragment();
        Bundle args = new Bundle();
        args.putInt(POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Override methods    ////////////////////////////////////////////////////////////////////////////////////////////////    @Override    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            position = getArguments().getInt(POSITION);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater){
        if (!getResources().getBoolean(R.bool.isTablet))
        {
            menu.clear();
            inflater.inflate(R.menu.menu_minimum,menu);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_reactions_detail, container, false);
        AppExtension app = (AppExtension) requireActivity().getApplicationContext();

        //Get the corresponding CharacterModel
        final CharacterModel characterModel = app.getInitiativeManager().getCharacters().get(position);

        //Get fields and set their values
        TextView textView = rootView.findViewById(R.id.reactions_character_detail_textView);
        textView.setText(String.format(getString(R.string.reaction_details_name_title), characterModel.getCharacterName()));


        //Setup RecyclerView by binding the adapter to it.
        RecyclerView reactionRecyclerView = rootView.findViewById(R.id.reactions_details_recyclerView);
        reactionRecyclerView.setHasFixedSize(false);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        reactionRecyclerView.setLayoutManager(llm);

        final ReactionAdapter adapter = new ReactionAdapter(characterModel);
        ArrayList<ReactionModel> reactionList = characterModel.getReactionList();
        adapter.addAll(reactionList);
        reactionRecyclerView.setAdapter(adapter);

        //Buttons
        Button clearAllReactionsButton = rootView.findViewById(R.id.clear_all_reactions_button);
        clearAllReactionsButton.setOnClickListener(view -> {
            adapter.removeAll();
            characterModel.getReactionList().clear();
        });

        Button addReactionButton = rootView.findViewById(R.id.add_reaction_button);
        addReactionButton.setOnClickListener(view -> {
            ReactionModel newReaction = new ReactionModel();
            adapter.add(newReaction);
            characterModel.getReactionList().add(newReaction);
        });

        return rootView;
    }
}