package com.rekijan.initiativetrackersecondedition.listeners;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.character.adapter.CharacterAdapter;
import com.rekijan.initiativetrackersecondedition.character.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.helper.HitPointAndDyingChangeHelper;

/**
 * <p>Listener specifically for HP</p>
 * See {@link CharacterModel} and {@link com.rekijan.initiativetrackersecondedition.character.adapter.CharacterAdapter}
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 26-10-2015
 */
public class HpTextWatcher implements View.OnClickListener {

    private final CharacterModel character;
    private final Context context;
    private final CharacterAdapter characterAdapter;

    public HpTextWatcher(CharacterModel character, CharacterAdapter characterAdapter, Context context) {
        this.character = character;
        this.context = context;
        this.characterAdapter = characterAdapter;
    }

    @Override
    public void onClick(View view) {
        //Build a dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        //Add custom layout to dialog
        LayoutInflater inflater = LayoutInflater.from(context);
        final View alertDialogView = inflater.inflate(R.layout.hp_dialog, null);
        builder.setTitle(context.getString(R.string.hp_dialog_title));
        //Set button to close and cancel
        builder.setNegativeButton(context.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        //Get views
        TextView oldValueTextView = alertDialogView.findViewById(R.id.old_value_textView);
        final TextView addPreviewTextView = alertDialogView.findViewById(R.id.add_preview_textView);
        final TextView setPreviewTextView = alertDialogView.findViewById(R.id.set_preview_textView);
        final TextView subtractPreviewTextView = alertDialogView.findViewById(R.id.subtract_preview_textView);
        final EditText inputTextView = alertDialogView.findViewById(R.id.input_editText);

        //Set initial values
        String initialValue = String.valueOf(character.getHp());
        oldValueTextView.setText(initialValue);
        addPreviewTextView.setText(initialValue);
        setPreviewTextView.setText(initialValue);
        subtractPreviewTextView.setText(initialValue);

        //Add text change listener
        inputTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                //After the input value changed update the preview TextViews
                String inputValueString = inputTextView.getText().toString();
                int inputValue = TextUtils.isEmpty(inputValueString) ? 0 : Integer.parseInt(inputValueString);
                addPreviewTextView.setText(String.valueOf(character.getHp() + inputValue));
                setPreviewTextView.setText(String.valueOf(inputValue));
                subtractPreviewTextView.setText(String.valueOf(character.getHp() - inputValue));

            }
        });

        //Bind view to the dialog builder and create it
        builder.setView(alertDialogView);
        final AlertDialog dialog = builder.create();

        //Get buttons and set their listeners
        Button addButton = alertDialogView.findViewById(R.id.add_btn);
        Button setButton = alertDialogView.findViewById(R.id.set_btn);
        Button subtractButton = alertDialogView.findViewById(R.id.subtract_btn);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String inputValueString = inputTextView.getText().toString();
                int inputValue = TextUtils.isEmpty(inputValueString) ? 0 : Integer.parseInt(inputValueString);
                int newValue = character.getHp() + inputValue;
                character.setHp(newValue);
                characterAdapter.notifyDataSetChanged();
                dialog.dismiss();
                HitPointAndDyingChangeHelper.getInstance().handleCharacterHpChange(character, newValue, context);
            }
        });

        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String inputValueString = inputTextView.getText().toString();
                int inputValue = TextUtils.isEmpty(inputValueString) ? 0 : Integer.parseInt(inputValueString);
                character.setHp(inputValue);
                characterAdapter.notifyDataSetChanged();
                dialog.dismiss();
                HitPointAndDyingChangeHelper.getInstance().handleCharacterHpChange(character, inputValue, context);
            }
        });

        subtractButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String inputValueString = inputTextView.getText().toString();
                int inputValue = TextUtils.isEmpty(inputValueString) ? 0 : Integer.parseInt(inputValueString);
                int newValue = character.getHp() - inputValue;
                character.setHp(newValue);
                characterAdapter.notifyDataSetChanged();
                dialog.dismiss();
                HitPointAndDyingChangeHelper.getInstance().handleCharacterHpChange(character, newValue, context);
            }
        });
        //Show the main dialog
        dialog.show();
    }
}