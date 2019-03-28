package com.rekijan.initiativetracker.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rekijan.initiativetracker.R;
import com.rekijan.initiativetracker.character.adapter.CharacterAdapter;
import com.rekijan.initiativetracker.character.model.CharacterModel;

import java.lang.reflect.Type;
import java.util.ArrayList;

import static com.rekijan.initiativetracker.AppConstants.GSON_TAG;
import static com.rekijan.initiativetracker.AppConstants.ROUND_COUNTER;
import static com.rekijan.initiativetracker.AppConstants.SHARED_PREF_TAG;

/**
 * A fragment containing the list of CharacterModels
 */
public class MainActivityFragment extends Fragment {

    private CharacterAdapter mAdapter;
    private int mRoundCounter;
    private TextView counterTextView;

    public MainActivityFragment() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mAdapter = new CharacterAdapter(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        //Round counter
        counterTextView = rootView.findViewById(R.id.round_counter_textView);
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
        counterTextView.setText(String.valueOf(sharedPreferences.getInt(ROUND_COUNTER, 0)));

        Button plusButton = rootView.findViewById(R.id.plus_round_button);
        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int oldNumber = Integer.parseInt(counterTextView.getText().toString());
                mRoundCounter = oldNumber+1;
                counterTextView.setText(String.valueOf(mRoundCounter));
            }
        });

        Button minusButton = rootView.findViewById(R.id.minus_round_button);
        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int oldNumber = Integer.parseInt(counterTextView.getText().toString());
                mRoundCounter = oldNumber-1;
                if (mRoundCounter < 0) mRoundCounter = 0;
                counterTextView.setText(String.valueOf(mRoundCounter));
            }
        });

        Button resetButton = rootView.findViewById(R.id.reset_round_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                counterTextView.setText(String.valueOf(0));
            }
        });

        //Setup RecyclerView by binding the adapter to it.
        RecyclerView charactersRecyclerView = rootView.findViewById(R.id.characters_recyclerView);
        charactersRecyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        charactersRecyclerView.setLayoutManager(llm);
        charactersRecyclerView.setAdapter(mAdapter);

        //Setup long click listener to remove character
        charactersRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), charactersRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {

            }

            @Override
            public void onLongClick(View view, final int position) {
                //Create a dialog to ask for confirmation before deleting
                String characterName = mAdapter.getList().get(position).getCharacterName();
                characterName = TextUtils.isEmpty(characterName) ? getString(R.string.empty_character_name) : characterName;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle);
                builder.setMessage(getString(R.string.dialog_delete) + characterName + "?")
                        .setTitle(getString(R.string.dialog_delete_title));
                builder.setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mAdapter.remove(position);
                    }
                });
                builder.setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }));
        return rootView;
    }

    /**
     * Looks for previously saved data and restores that, else it will add 5 empty characters
     */
    private void initializeData() {
        Context context = getContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(GSON_TAG, null);
        Type type = new TypeToken<ArrayList<CharacterModel>>() {
        }.getType();
        ArrayList<CharacterModel> characters;
        characters = gson.fromJson(json, type);
        if (characters != null) {
            for (CharacterModel c: characters) {
                c.setContext(context);
            }
            mAdapter.addAll(characters);
        } else {
            mAdapter.add(new CharacterModel(context));
            mAdapter.add(new CharacterModel(context));
            mAdapter.add(new CharacterModel(context));
            mAdapter.add(new CharacterModel(context));
            mAdapter.add(new CharacterModel(context));
        }
    }

    @Override
    public void onPause() {
        saveData(); //save data when app is going to the background
        super.onPause();
    }

    /**
     * Saves all character data
     */
    public void saveData() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        ArrayList<CharacterModel> characters = mAdapter.getList();
        String json = gson.toJson(characters);
        editor.putString(GSON_TAG, json);
        editor.putInt(ROUND_COUNTER, mRoundCounter);
        editor.apply();
    }

    private void addCharacter() {
        mAdapter.add(new CharacterModel(getContext()));
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Show dialog to explain how to remove a character
     */
    public void deleteCharacterInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle);
        builder.setMessage(getString(R.string.dialog_delete_info))
                .setTitle(getString(R.string.dialog_delete_info_title));
        builder.setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Show dialog explaining more info is available on the website
     */
    public void aboutInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle);
        builder.setMessage(getString(R.string.dialog_about_info))
                .setTitle(getString(R.string.dialog_about_info_title));
        builder.setPositiveButton(getString(R.string.dialog_about_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent siteIntent =
                        new Intent("android.intent.action.VIEW",
                                Uri.parse("http://www.rekijan.nl/"));
                startActivity(siteIntent);
            }
        });
        builder.setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelableArrayList("characters", mAdapter.getList());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            ArrayList<CharacterModel> list = savedInstanceState.getParcelableArrayList("characters");
            if (list != null) {
                mAdapter.addAll(list);
            }
        } else {
            initializeData();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Do something that differs the Activity's menu here
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings_next_turn:
                boolean isNextRound = mAdapter.nextTurn();
                if (isNextRound) nextRound();
                return true;
            case R.id.action_settings_sort:
                mAdapter.sortInitiative();
                //TODO dialog to confirm player to have first round (own option?)
                return true;
            case R.id.action_settings_add_character:
                addCharacter();
                return true;
            case R.id.action_settings_delete_character:
                deleteCharacterInfo();
                return true;
            case R.id.action_settings_about:
                aboutInfo();
                return true;
//          //TODO go to select party screen add in later
            //TODO remove/reset character(s)
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Called from {@link CharacterAdapter#nextTurn()} if the {@link CharacterModel} whose turn it is marks the start of a new round<br>
     *     Ups the round counter by 1
     */
    private void nextRound() {
        int oldNumber = Integer.parseInt(counterTextView.getText().toString());
        mRoundCounter = oldNumber+1;
        counterTextView.setText(String.valueOf(mRoundCounter));
    }

    /* BEGIN REGION Needed to setup long click listener on the RecyclerView */
    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    public static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private GestureDetector gestureDetector;
        private MainActivityFragment.ClickListener clickListener;

        public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final MainActivityFragment.ClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildAdapterPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }
    }

    /* END REGION setup long click listener */
}