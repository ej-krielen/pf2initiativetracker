package com.rekijan.initiativetrackersecondedition.common.listeners;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.common.helper.DialogHelper;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.features.initiative.InitiativeAdapter;
import com.rekijan.initiativetrackersecondedition.features.health.HitPointDialogHelper;

/**
 * Click listener that opens a specialized HP modification dialog.
 * Provides live previews for adding, setting, or subtracting health points.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 26-10-2015
 */
public class HpTextWatcher implements View.OnClickListener {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private final CharacterModel character;    private final Context context;
    private final InitiativeAdapter initiativeAdapter;

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Constructor    ////////////////////////////////////////////////////////////////////////////////////////////////    public HpTextWatcher(CharacterModel character, InitiativeAdapter initiativeAdapter, Context context) {        this.character = character;
        this.context = context;
        this.initiativeAdapter = initiativeAdapter;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Overridden methods    ////////////////////////////////////////////////////////////////////////////////////////////////    @Override    public void onClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        View alertDialogView = DialogHelper.getInstance().initCustomDialog(context, builder,
                R.layout.dialog_hp, context.getString(R.string.hp_dialog_title), context.getString(R.string.dialog_cancel));

        //Get views
        TextView oldValueTextView = alertDialogView.findViewById(R.id.old_value_textView);
        final TextView addPreviewTextView = alertDialogView.findViewById(R.id.add_preview_textView);
        final TextView setPreviewTextView = alertDialogView.findViewById(R.id.set_preview_textView);
        final TextView subtractPreviewTextView = alertDialogView.findViewById(R.id.subtract_preview_textView);
        final EditText inputTextView = alertDialogView.findViewById(R.id.input_editText);

        setInitialValues(oldValueTextView, addPreviewTextView, setPreviewTextView, subtractPreviewTextView);

        //Add text change listener
        inputTextView.addTextChangedListener(new EditTextWatcher(text -> {
            //After the input value changed update the preview TextViews
            int inputValue = DialogHelper.getInstance().parseIntSafe(text);
            addPreviewTextView.setText(String.valueOf(character.getHp() + inputValue));
            setPreviewTextView.setText(String.valueOf(inputValue));
            subtractPreviewTextView.setText(String.valueOf(character.getHp() - inputValue));
        }));

        //Bind view to the dialog builder and create it
        builder.setView(alertDialogView);
        final AlertDialog dialog = builder.create();

        //Get buttons and set their listeners
        Button addButton = alertDialogView.findViewById(R.id.add_btn);
        Button setButton = alertDialogView.findViewById(R.id.set_btn);
        Button subtractButton = alertDialogView.findViewById(R.id.subtract_btn);

        addButton.setOnClickListener(v -> {
            int inputValue = DialogHelper.getInstance().parseIntSafe(inputTextView.getText().toString());
            applyHpChangeAndDismiss(character.getHp() + inputValue, dialog);
        });

        setButton.setOnClickListener(v -> {
            int inputValue = DialogHelper.getInstance().parseIntSafe(inputTextView.getText().toString());
            applyHpChangeAndDismiss(inputValue, dialog);
        });

        subtractButton.setOnClickListener(v -> {
            int inputValue = DialogHelper.getInstance().parseIntSafe(inputTextView.getText().toString());
            applyHpChangeAndDismiss(character.getHp() - inputValue, dialog);
        });
        //Show the main dialog
        dialog.show();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Private helpers    ////////////////////////////////////////////////////////////////////////////////////////////////    /**     * Updates the model, dismisses the dialog, and triggers rule checks (dying, death, etc.).
     */
    private void applyHpChangeAndDismiss(int newValue, AlertDialog dialog) {
        character.setHp(newValue);
        initiativeAdapter.notifyCharacterChanged(character);
        dialog.dismiss();
        HitPointDialogHelper.getInstance().handleCharacterHpChange(character, newValue, context);
    }

    /**
     * Sets the starting labels in the dialog based on the character's current HP.
     */
    private void setInitialValues (TextView oldValueTextView, TextView addPreviewTextView, TextView setPreviewTextView, TextView subtractPreviewTextView) {
        String initialValue = String.valueOf(character.getHp());
        oldValueTextView.setText(initialValue);
        addPreviewTextView.setText(initialValue);
        setPreviewTextView.setText(initialValue);
        subtractPreviewTextView.setText(initialValue);
    }
}