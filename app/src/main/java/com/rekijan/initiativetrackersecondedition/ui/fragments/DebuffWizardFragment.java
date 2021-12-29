package com.rekijan.initiativetrackersecondedition.ui.fragments;

import static com.rekijan.initiativetrackersecondedition.AppConstants.POSITION;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rekijan.initiativetrackersecondedition.AppExtension;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.character.adapter.DebuffWizardAdapter;
import com.rekijan.initiativetrackersecondedition.character.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.character.model.DebuffModel;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * A fragment in which the user can create a DebuffModel
 */
public class DebuffWizardFragment extends Fragment {

    private int position;

    public DebuffWizardFragment() {
    }

    public static DebuffWizardFragment newInstance(int position) {
        DebuffWizardFragment fragment = new DebuffWizardFragment();
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        menu.clear();
        inflater.inflate(R.menu.menu_minimum,menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_debuff_wizard, container, false);
        AppExtension app = (AppExtension) getActivity().getApplicationContext();

        //Get the corresponding CharacterModel
        final CharacterModel characterModel = app.getCharacterAdapter().getList().get(position);

        //Setup RecyclerView by binding the adapter to it.
        RecyclerView debuffWizardRecyclerView = rootView.findViewById(R.id.debuff_wizard_recyclerView);
        debuffWizardRecyclerView.setHasFixedSize(false);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        debuffWizardRecyclerView.setLayoutManager(llm);

        final DebuffWizardAdapter adapter = new DebuffWizardAdapter(characterModel, app, this);
        ArrayList<DebuffModel> debuffList = app.getConditionList();
        adapter.addAll(debuffList);
        debuffWizardRecyclerView.setAdapter(adapter);

        SearchView searchView = rootView.findViewById(R.id.debuff_wizard_searchView);
        EditText searchEditText = (EditText) searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchEditText.setTextColor(getResources().getColor(R.color.white));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });

        CheckBox hideDescriptionCheckBox = rootView.findViewById(R.id.debuff_wizard_description_checkBox);
        hideDescriptionCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                app.setHideDescriptions(checked);
                adapter.notifyDataSetChanged();
            }
        });
        hideDescriptionCheckBox.setChecked(app.getHideDescription());

        //Buttons
        Button addEmptyDebuffButton = rootView.findViewById(R.id.debuff_wizard_add_empty_button);
        addEmptyDebuffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DebuffModel debuffModel = new DebuffModel();
                adapter.add(debuffModel);
                characterModel.getDebuffList().add(debuffModel);
                Toast.makeText(app, getString(R.string.debuff_added_empty), Toast.LENGTH_LONG).show();
                getParentFragmentManager().popBackStack();
            }
        });

        Button addPoisonButton = rootView.findViewById(R.id.debuff_wizard_add_poison_button);
        addPoisonButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO poison wizard
                Toast.makeText(app, "This feature is not yet implemented", Toast.LENGTH_LONG).show();
            }
        });

        Context context = requireActivity();
        Button addPersistentDamageButton = rootView.findViewById(R.id.debuff_wizard_add_persistent_damage_button);
        addPersistentDamageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                persistentDamageWizardDialog(context, adapter, characterModel, app);
            }
        });

        return rootView;
    }

    private void persistentDamageWizardDialog(Context context, DebuffWizardAdapter adapter, CharacterModel characterModel, AppExtension app) {
        //Build a dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        //Add custom layout to dialog
        LayoutInflater inflater = LayoutInflater.from(context);
        final View alertDialogView = inflater.inflate(R.layout.dialog_persistent_damage_create, null);
        builder.setTitle(context.getString(R.string.persistent_dialog_title));

        //Get views
        final EditText damageTypeEditText = alertDialogView.findViewById(R.id.persistent_damage_dialog_type_editText);
        final EditText damageValueEditText = alertDialogView.findViewById(R.id.persistent_damage_dialog_value_editText);
        final EditText difficultyClassEditText = alertDialogView.findViewById(R.id.persistent_damage_dialog_dc_editText);
        final EditText durationEditText = alertDialogView.findViewById(R.id.persistent_damage_dialog_duration_editText);
        final EditText descriptionEditText = alertDialogView.findViewById(R.id.persistent_damage_dialog_description_editText);
        final Button addButton = alertDialogView.findViewById(R.id.dialog_add_persistent_damage_button);
        final TextView warningTextView = alertDialogView.findViewById(R.id.dialog_persistent_input_warning_textView);

        warningTextView.setVisibility(View.GONE);

        //Set button to close and cancel
        builder.setNegativeButton(context.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        //Bind view to the dialog builder and create it
        builder.setView(alertDialogView);
        final AlertDialog dialog = builder.create();

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String damageType = damageTypeEditText.getText().toString();
                if (TextUtils.isEmpty(damageType)) {
                    warningTextView.setVisibility(View.VISIBLE);
                    return;
                }
                String damage = damageValueEditText.getText().toString();
                if (TextUtils.isEmpty(damage))
                    damage = context.getString(R.string.persistent_damage_hint);
                int duration = inputIntText(durationEditText.getText().toString());
                int difficultyClass;
                if (TextUtils.isEmpty(difficultyClassEditText.getText().toString())) {
                    difficultyClass = inputIntText(context.getString(R.string.persistent_dc_hint));
                } else {
                    difficultyClass = inputIntText(difficultyClassEditText.getText().toString());
                }
                String description = descriptionEditText.getText().toString();

                String name = String.format(context.getResources().getString(R.string.persistent_name), damageType, damage);

                DebuffModel debuffModel = new DebuffModel(name, damageType, duration, damage, difficultyClass, description);
                adapter.add(debuffModel);
                characterModel.getDebuffList().add(debuffModel);
                dialog.dismiss();
                Toast.makeText(app, app.getResources().getString(R.string.debuff_added_persistent_damage), Toast.LENGTH_LONG).show();
                getParentFragmentManager().popBackStack();
            }
        });

        //Show the main dialog
        dialog.show();

    }

    private int inputIntText(String text) {

        BigInteger maxInt = BigInteger.valueOf(Integer.MAX_VALUE);
        BigInteger value = new BigInteger(TextUtils.isEmpty(text) ? "0" : text);

        if (value.compareTo(maxInt) > 0)
        {
            value = maxInt;
        }
        return value.intValue();
    }
}