package com.rekijan.initiativetrackersecondedition.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.AppExtension;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.character.adapter.CharacterAdapter;
import com.rekijan.initiativetrackersecondedition.character.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.helper.DialogHelper;
import com.rekijan.initiativetrackersecondedition.helper.HitPointAndDyingChangeHelper;
import com.rekijan.initiativetrackersecondedition.ui.activities.MainActivity;

import static com.rekijan.initiativetrackersecondedition.AppConstants.ROUND_COUNTER;
import static com.rekijan.initiativetrackersecondedition.AppConstants.SHARED_PREF_TAG;

/**
 * A fragment containing the list of CharacterModels
 */
public class MainActivityFragment extends Fragment {

    private int mRoundCounter;
    private TextView counterTextView;

    public MainActivityFragment() {}

    public static MainActivityFragment newInstance() { return new MainActivityFragment(); }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        //Round counter
        counterTextView = rootView.findViewById(R.id.round_counter_textView);
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
        counterTextView.setText(String.valueOf(sharedPreferences.getInt(ROUND_COUNTER, 1)));

        Button plusButton = rootView.findViewById(R.id.up_edit_order_button);
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
                counterTextView.setText(String.valueOf(1));
            }
        });

        Button editOrderButton = rootView.findViewById(R.id.edit_order_button);
        editOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)requireActivity()).replaceEditOrderFragment();

            }
        });

        //Setup RecyclerView by binding the adapter to it.
        RecyclerView charactersRecyclerView = rootView.findViewById(R.id.characters_recyclerView);
        charactersRecyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        charactersRecyclerView.setLayoutManager(llm);

        AppExtension app = (AppExtension) getActivity().getApplicationContext();
        final CharacterAdapter adapter = app.getCharacterAdapter();
        charactersRecyclerView.setAdapter(adapter);

        //Setup long click listener to remove character
        charactersRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), charactersRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) { }

            @Override
            public void onLongClick(View view, final int position) {
                //Create a dialog to ask for confirmation before deleting
                String characterName = adapter.getList().get(position).getCharacterName();
                characterName = TextUtils.isEmpty(characterName) ? getString(R.string.empty_character_name) : characterName;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle);
                builder.setMessage(getString(R.string.dialog_delete) + characterName + "?")
                        .setTitle(getString(R.string.dialog_delete_title));
                builder.setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        adapter.remove(position);
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

    @Override
    public void onPause() {
        AppExtension app = (AppExtension) getActivity().getApplicationContext();
        app.saveData(mRoundCounter);
        super.onPause();
    }

    private void addCharacter() {
        AppExtension app = (AppExtension) getActivity().getApplicationContext();
        app.getCharacterAdapter().add(new CharacterModel(getContext()));
        app.getCharacterAdapter().notifyDataSetChanged();
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
        AppExtension app = (AppExtension) getActivity().getApplicationContext();
        savedInstanceState.putParcelableArrayList("characters", app.getCharacterAdapter().getList());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Do something that differs the Activity's menu here
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AppExtension app = (AppExtension) getActivity().getApplicationContext();

        int itemId = item.getItemId();

        if (itemId == R.id.action_settings_next_turn) {
            boolean isNextRound = app.getCharacterAdapter().nextTurn(getActivity());
            if (isNextRound) nextRound();
            return true;
        } else if (itemId == R.id.action_settings_sort) {
            askRoundResetConfirmation(app.getCharacterAdapter().sortInitiative());
            return true;
        } else if (itemId == R.id.action_settings_add_character) {
            addCharacter();
            return true;
        } else if (itemId == R.id.action_settings_delete_character) {
            deleteCharacterInfo();
            return true;
        } else if (itemId == R.id.action_settings_set_pcs_max_hp) {
            HitPointAndDyingChangeHelper.getInstance().setPCsToMaxHP(app);
            return true;
        } else if (itemId == R.id.action_settings_start_turn_info) {
            DialogHelper.getInstance().simpleDialog(getActivity(), getString(R.string.dialog_turn_start_info_title), getString(R.string.dialog_turn_start_info));
            return true;
        } else if (itemId == R.id.action_settings_end_turn_info) {
            DialogHelper.getInstance().simpleDialog(getActivity(), getString(R.string.dialog_turn_end_info_title), getString(R.string.dialog_turn_end_info));
            return true;
        } else if (itemId == R.id.action_settings_about) {
            aboutInfo();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Called from {@link CharacterAdapter#nextTurn(androidx.fragment.app.FragmentActivity)} if the {@link CharacterModel} whose turn it is marks the start of a new round<br>
     *     Ups the round counter by 1
     */
    private void nextRound() {
        int oldNumber = Integer.parseInt(counterTextView.getText().toString());
        mRoundCounter = oldNumber+1;
        counterTextView.setText(String.valueOf(mRoundCounter));
    }

    private void askRoundResetConfirmation(boolean warnForDoubleInitiative) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle);
        String message = warnForDoubleInitiative ? getString(R.string.dialog_reset_round_counter) +  getString(R.string.dialog_reset_round_double_init) : getString(R.string.dialog_reset_round_counter);
        builder.setMessage(message)
                .setTitle(getString(R.string.dialog_reset_round_counter_title));
        builder.setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                counterTextView.setText(String.valueOf(1));
            }
        });
        builder.setNegativeButton(getString(R.string.dialog_reset_round_counter_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    /* BEGIN REGION Needed to setup long click listener on the RecyclerView */
    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    public static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private final GestureDetector gestureDetector;
        private final MainActivityFragment.ClickListener clickListener;

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