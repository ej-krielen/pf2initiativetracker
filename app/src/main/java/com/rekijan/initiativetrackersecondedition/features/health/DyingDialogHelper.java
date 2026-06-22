package com.rekijan.initiativetrackersecondedition.features.health;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.rekijan.initiativetrackersecondedition.core.AppExtension;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;

/**
 * Helper class with methods to handle stuff having to do with hit points and death and dying.
 * Manages the UI and logic for recovery checks, dying value changes, and hero point usage prompts.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 24-3-2021
 */
public class DyingDialogHelper {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private static DyingDialogHelper sInstance = null;
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Initialization    ////////////////////////////////////////////////////////////////////////////////////////////////    public static synchronized DyingDialogHelper getInstance() {        if (sInstance == null) {
            sInstance = new DyingDialogHelper();
        }
        return sInstance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods    ////////////////////////////////////////////////////////////////////////////////////////////////    public void setCharacterToDyingDialog(final CharacterModel character, final Context context) {        //Build dialog
        AlertDialog.Builder dyingCritDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        dyingCritDialog.setTitle(context.getString(R.string.dialog_dying_crit_title))
                .setMessage(context.getString(R.string.dialog_dying_crit));
        dyingCritDialog.setPositiveButton(context.getString(R.string.dialog_dying_crit_positive), (dialogInterface, i) -> setCharacterToDying(character, 2, context));
        dyingCritDialog.setNegativeButton(context.getString(R.string.dialog_dying_crit_negative), (dialogInterface, i) -> setCharacterToDying(character, 1, context));

        AlertDialog removalDialog = dyingCritDialog.create();
        removalDialog.show();
    }

    public void setCharacterToDying(CharacterModel character, int dyingValueIncrease, Context context) {
        AppExtension app = (AppExtension) context.getApplicationContext();
        app.getInitiativeManager().moveCharacterToDying(character, dyingValueIncrease);
        HeroPointDialogHelper.getInstance().promptHeroPointUseDialog(character, context);
        app.getInitiativeAdapter().notifyItemRangeChanged(0, app.getInitiativeAdapter().getItemCount());
    }

    public void increaseDyingDialog(final CharacterModel character, final Context context) {
        //Build dialog
        AlertDialog.Builder dyingCritDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        dyingCritDialog.setTitle(context.getString(R.string.dialog_dying_crit_title))
                .setMessage(context.getString(R.string.dialog_dying_damaged_again));
        dyingCritDialog.setPositiveButton(context.getString(R.string.dialog_dying_damaged_again_positive), (dialogInterface, i) -> increaseCharacterDyingValue(character, 2, context));
        dyingCritDialog.setNegativeButton(context.getString(R.string.dialog_dying_damaged_again_negative), (dialogInterface, i) -> increaseCharacterDyingValue(character, 1, context));

        AlertDialog removalDialog = dyingCritDialog.create();
        removalDialog.show();
    }

    public void increaseCharacterDyingValue(CharacterModel character, int dyingValueIncrease, Context context) {
        character.setDyingValue(character.getDyingValue()+dyingValueIncrease);
        AppExtension app = (AppExtension) context.getApplicationContext();
        app.getInitiativeAdapter().notifyCharacterChanged(character);
        HeroPointDialogHelper.getInstance().promptHeroPointUseDialog(character, context);
    }

    /**
     * Technical helper to decrease dying value and check for recovery.
     */
    private void decreaseCharacterDyingValue(CharacterModel character, int dyingValueDecrease, Context context) {
        AppExtension app = (AppExtension) context.getApplicationContext();

        character.setDyingValue(character.getDyingValue()-dyingValueDecrease);

        if (character.getDyingValue() <= 0)
        {
            character.setIsDying(false);
            character.setDyingValue(0);
            character.setWoundedValue(character.getWoundedValue()+1);
            character.setHp(0);
            Toast.makeText(context, context.getString(R.string.toast_recovered_from_dying_by_recovery_checks, character.getCharacterName()), Toast.LENGTH_LONG).show();
        }
        app.getInitiativeAdapter().notifyCharacterChanged(character);
    }

    public void checkIfCharacterDied(final CharacterModel character, Context context){
        final AppExtension app = (AppExtension) context.getApplicationContext();

        if (character.getDyingValue() >= character.getMaxDyingValue()-character.getDoomedValue())
        {
            AlertDialog.Builder removalBuilder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
            removalBuilder.setTitle(context.getString(R.string.dialog_delete_low_title))
                    .setMessage(context.getString(R.string.dialog_delete_low));
            removalBuilder.setPositiveButton(context.getString(R.string.dialog_delete_low_positive), (dialogInterface, i) -> {
                app.getInitiativeManager().removeCharacter(character);
                app.getInitiativeAdapter().notifyItemRangeChanged(0, app.getInitiativeAdapter().getItemCount());
            });
            removalBuilder.setNegativeButton(context.getString(R.string.dialog_delete_low_negative), (dialogInterface, i) -> {
            });

            AlertDialog removalDialog = removalBuilder.create();
            removalDialog.show();
        }
    }

    public void promptRecoveryCheckDialog(final CharacterModel character, final Context context) {
        final AppExtension app = (AppExtension) context.getApplicationContext();

        //Build a dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        //Add custom layout to dialog
        LayoutInflater inflater = LayoutInflater.from(context);
        final View alertDialogView = inflater.inflate(R.layout.dialog_recovery, null);
        builder.setTitle(context.getString(R.string.dialog_recovery_title));
        //Set button to close and cancel
        builder.setNegativeButton(context.getString(R.string.dialog_cancel), (dialogInterface, i) -> {});

        //Get views
        TextView recoveryDcTextView = alertDialogView.findViewById(R.id.recovery_dialog_dc_label);
        Button recoveryCritSuccessButton = alertDialogView.findViewById(R.id.recovery_dialog_crit_success_btn);
        Button recoverySuccessButton = alertDialogView.findViewById(R.id.recovery_dialog_success_btn);
        Button recoveryFailureButton = alertDialogView.findViewById(R.id.recovery_dialog_failure_btn);
        Button recoveryCritFailureButton = alertDialogView.findViewById(R.id.recovery_dialog_crit_failure_btn);

        //Bind view to the dialog builder and create it
        builder.setView(alertDialogView);
        final AlertDialog dialog = builder.create();

        //Set views
        recoveryDcTextView.setText(context.getString(R.string.dialog_recovery_dc, character.getRecoveryDC()+character.getDyingValue()+character.getWoundedValue()));

        recoveryCritSuccessButton.setOnClickListener(v -> {
            decreaseCharacterDyingValue(character, 2, context);
            dialog.dismiss();
        });

        recoverySuccessButton.setOnClickListener(v -> {
            decreaseCharacterDyingValue(character, 1, context);
            dialog.dismiss();
        });

        recoveryFailureButton.setOnClickListener(v -> {
            increaseCharacterDyingValue(character, 1, context);
            dialog.dismiss();
        });

        recoveryCritFailureButton.setOnClickListener(v -> {
            increaseCharacterDyingValue(character, 2, context);
            dialog.dismiss();
        });

        //Show the main dialog
        dialog.show();
    }

    /**
     * Show dialog explaining more info about death and dying rules.
     * Includes a link to official rules.
     */
    public void deathDyingRulesDialog(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        builder.setMessage(context.getString(R.string.dialog_dying_rules))
                .setTitle(context.getString(R.string.dialog_dying_rules_title));
        builder.setPositiveButton(context.getString(R.string.dialog_about_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent siteIntent =
                        new Intent("android.intent.action.VIEW",
                                Uri.parse("https://2e.aonprd.com/Rules.aspx?ID=372"));
                context.startActivity(siteIntent);
            }
        });
        builder.setNegativeButton(context.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}