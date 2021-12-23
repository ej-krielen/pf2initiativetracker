package com.rekijan.initiativetrackersecondedition.ui.fragments;

import static com.rekijan.initiativetrackersecondedition.AppConstants.POSITION;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.AppExtension;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.character.adapter.ReactionAdapter;
import com.rekijan.initiativetrackersecondedition.character.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.character.model.ReactionModel;

import java.util.ArrayList;

/**
 * A fragment containing details of a single CharacterModel
 */
public class ReactionsDetailFragment extends Fragment {

    private int position;

    public ReactionsDetailFragment() {}

    public static ReactionsDetailFragment newInstance(int position) {
        ReactionsDetailFragment fragment =  new ReactionsDetailFragment();
        Bundle args = new Bundle();
        args.putInt(POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            position = getArguments().getInt(POSITION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_reactions_detail, container, false);
        AppExtension app = (AppExtension) getActivity().getApplicationContext();

        //Get the corresponding CharacterModel
        final CharacterModel characterModel = app.getCharacterAdapter().getList().get(position);

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
        clearAllReactionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.removeAll();
                characterModel.getReactionList().clear();
                adapter.notifyDataSetChanged();
            }
        });

        Button addReactionButton = rootView.findViewById(R.id.add_reaction_button);
        addReactionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReactionModel newReaction = new ReactionModel();
                adapter.add(newReaction);
                characterModel.getReactionList().add(newReaction);
                adapter.notifyDataSetChanged();
            }
        });

        return rootView;
    }
}