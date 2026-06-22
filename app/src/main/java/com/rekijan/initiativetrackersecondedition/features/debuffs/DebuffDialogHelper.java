package com.rekijan.initiativetrackersecondedition.features.debuffs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.rekijan.initiativetrackersecondedition.core.AppExtension;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.data.model.DebuffModel;
import com.rekijan.initiativetrackersecondedition.features.health.HitPointDialogHelper;

/**
 * Helper class for managing debuff-related dialogs.
 * Primarily handles the logic for applying and checking persistent damage at the end of a character's turn.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 24-3-2021
 */
public class DebuffDialogHelper {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private static DebuffDialogHelper sInstance = null;
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Initialization    ////////////////////////////////////////////////////////////////////////////////////////////////    public static synchronized DebuffDialogHelper getInstance() {        if (sInstance == null) {
            sInstance = new DebuffDialogHelper();
        }
        return sInstance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods    ////////////////////////////////////////////////////////////////////////////////////////////////    public void promptPersistentDamageDialog(CharacterModel character, DebuffModel debuff, Context context) {        //Build a dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        //Add custom layout to dialog
        LayoutInflater inflater = LayoutInflater.from(context);
        final View alertDialogView = inflater.inflate(R.layout.dialog_persistent_damage_activate, null);

        builder.setTitle(String.format(context.getString(R.string.persistent_damage_dialog_title), character.getCharacterName()));
        //Set button to close and cancel
        builder.setNegativeButton(context.getString(R.string.dialog_close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        //Get views
        TextView nameTextView = alertDialogView.findViewById(R.id.persistent_damage_apply_name_textView);
        TextView valuesTextView = alertDialogView.findViewById(R.id.persistent_damage_apply_values_textView);
        TextView descriptionTextView = alertDialogView.findViewById(R.id.persistent_damage_apply_description_textView);
        EditText valueEditText = alertDialogView.findViewById(R.id.persistent_damage_apply_overridden_editText);
        Button damageButton = alertDialogView.findViewById(R.id.persistent_damage_take_damage_button);
        Button flatCheckButton = alertDialogView.findViewById(R.id.persistent_damage_flat_check_button);

        nameTextView.setText(debuff.getName());
        String values = String.format(context.getString(R.string.apply_persistent_values), debuff.getDamageType(), debuff.getDamageValue(), debuff.getDifficultyClass(), debuff.getDuration());
        valuesTextView.setText(values);

        if (TextUtils.isEmpty(debuff.getDescription()))
        {
            descriptionTextView.setVisibility(View.GONE);
        }
        else
        {
            descriptionTextView.setText(debuff.getDescription());
        }

        if (TextUtils.isDigitsOnly(debuff.getDamageValue())) {
            valueEditText.setText(debuff.getDamageValue());
        } else {
            valueEditText.setText("");
            valueEditText.setHint(debuff.getDamageValue());
        }

        damageButton.setOnClickListener(view -> {
            String input = valueEditText.getText().toString();
            if (TextUtils.isEmpty(input) || !TextUtils.isDigitsOnly(input)) {
                valueEditText.setError(context.getString(R.string.persistent_damage_must_override));
                Toast.makeText(context, context.getString(R.string.persistent_damage_must_override), Toast.LENGTH_SHORT).show();
                return;
            }

            int inputValue = Integer.parseInt(input);
            int newValue = character.getHp() - inputValue;
            character.setHp(newValue);
            HitPointDialogHelper.getInstance().handleCharacterHpChange(character, character.getHp(), context);
            damageButton.setVisibility(View.GONE);
        });

        flatCheckButton.setOnClickListener(view -> {
            character.getDebuffList().remove(debuff);
            Toast.makeText(context, context.getString(R.string.removed_debuff), Toast.LENGTH_LONG).show();
            flatCheckButton.setVisibility(View.GONE);
            AppExtension app = (AppExtension) context.getApplicationContext();
            app.getInitiativeAdapter().notifyCharacterChanged(character);
        });


        //Bind view to the dialog builder and create it
        builder.setView(alertDialogView);
        final AlertDialog dialog = builder.create();

        //Show the main dialog
        dialog.show();
    }
}