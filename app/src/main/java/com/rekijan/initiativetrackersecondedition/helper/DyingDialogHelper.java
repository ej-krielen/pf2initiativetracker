package com.rekijan.initiativetrackersecondedition.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.rekijan.initiativetrackersecondedition.AppExtension;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.character.model.CharacterModel;

/**
 * Helper class with methods to handle stuff having to do with hit points and death and dying.
 *
 * @author Erik-Jan Krielen ej.krielen@gmail.com
 * @since 24-3-2021
 */
public class DyingDialogHelper {

    private static DyingDialogHelper sInstance = null;

    public static synchronized DyingDialogHelper getInstance() {
        if (sInstance == null) {
            sInstance = new DyingDialogHelper();
        }
        return sInstance;
    }

    /**
     * Before we can set the Character to dying we first need to ask if it was a critical hit or not because that impacts the value of dying.
     * @param character
     */
    void setCharacterToDyingDialog(final CharacterModel character, final Context context) {
        //Build dialog
        AlertDialog.Builder dyingCritDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        dyingCritDialog.setTitle(context.getString(R.string.dialog_dying_crit_title))
                .setMessage(context.getString(R.string.dialog_dying_crit));
        dyingCritDialog.setPositiveButton(context.getString(R.string.dialog_dying_crit_positive), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setCharacterToDying(character, 2, context);
            }
        });
        dyingCritDialog.setNegativeButton(context.getString(R.string.dialog_dying_crit_negative), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setCharacterToDying(character, 1, context);
            }
        });

        AlertDialog removalDialog = dyingCritDialog.create();
        removalDialog.show();
    }

    /**
     * Moves the selected param to the bottom of the list. It also checks if its the first in round and if it needs to adjust that.
     * @param character the CharacterModel who just went to dying
     */
    public void setCharacterToDying(CharacterModel character, int dyingValueIncrease, Context context) {
        AppExtension app = (AppExtension) context.getApplicationContext();
        int posToRemove = -1;
        for (int i = 0; i < app.getCharacterAdapter().getList().size(); i++) {
            if (app.getCharacterAdapter().getList().get(i) == character) {
                if(character.isFirstRound() && i != app.getCharacterAdapter().getList().size()-1) {
                    character.setIsFirstRound(false);
                    app.getCharacterAdapter().getList().get(i+1).setIsFirstRound(true);
                }
                character.setIsDying(true);
                character.setDyingValue(dyingValueIncrease+character.getWoundedValue());
                HeroPointDialogHelper.getInstance().promptHeroPointUseDialog(character, context);
                app.getCharacterAdapter().getList().add(character);
                posToRemove = i;
                break;
            }
        }
        app.getCharacterAdapter().remove(posToRemove);
        app.getCharacterAdapter().notifyDataSetChanged();
    }

    /**
     * Before we can increase the Character's dying value we first need to ask if it was a critical hit or not because that determines how much it increases.
     * @param character
     */
    void increaseDyingDialog(final CharacterModel character, final Context context) {
        //Build dialog
        AlertDialog.Builder dyingCritDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        dyingCritDialog.setTitle(context.getString(R.string.dialog_dying_crit_title))
                .setMessage(context.getString(R.string.dialog_dying_damaged_again));
        dyingCritDialog.setPositiveButton(context.getString(R.string.dialog_dying_damaged_again_positive), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                increaseCharacterDyingValue(character, 2, context);
            }
        });
        dyingCritDialog.setNegativeButton(context.getString(R.string.dialog_dying_damaged_again_negative), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                increaseCharacterDyingValue(character, 1, context);
            }
        });

        AlertDialog removalDialog = dyingCritDialog.create();
        removalDialog.show();
    }

    /**
     * Increases the character's dying value by the amount, and handle rest of process
     * @param character
     * @param dyingValueIncrease
     */
    public void increaseCharacterDyingValue(CharacterModel character, int dyingValueIncrease, Context context) {
        character.setDyingValue(character.getDyingValue()+dyingValueIncrease);
        HeroPointDialogHelper.getInstance().promptHeroPointUseDialog(character, context);
    }

    /**
     * Decrease the character's dying value by the amount, then check if stabilized
     * @param character
     * @param dyingValueDecrease
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
        app.getCharacterAdapter().notifyDataSetChanged();
    }

    /**
     *
     * @param character
     */
    public void checkIfCharacterDied(final CharacterModel character, Context context){
        final AppExtension app = (AppExtension) context.getApplicationContext();

        if (character.getDyingValue() >= character.getMaxDyingValue()-character.getDoomedValue())
        {
            AlertDialog.Builder removalBuilder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
            removalBuilder.setTitle(context.getString(R.string.dialog_delete_low_title))
                    .setMessage(context.getString(R.string.dialog_delete_low));
            removalBuilder.setPositiveButton(context.getString(R.string.dialog_delete_low_positive), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    app.getCharacterAdapter().remove(character);
                    app.getCharacterAdapter().notifyDataSetChanged();

                }
            });
            removalBuilder.setNegativeButton(context.getString(R.string.dialog_delete_low_negative), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });

            AlertDialog removalDialog = removalBuilder.create();
            removalDialog.show();
        }
        app.getCharacterAdapter().notifyDataSetChanged();
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
        builder.setNegativeButton(context.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        //Get views
        TextView recoveryDcTextView = alertDialogView.findViewById(R.id.recovery_dialog_dc_label);
        Button recoveryCritSuccessButton = alertDialogView.findViewById(R.id.recovery_dialog_crit_success_btn);
        Button recoverySuccessButton = alertDialogView.findViewById(R.id.recovery_dialog_success_btn);
        Button recoveryFailureButton = alertDialogView.findViewById(R.id.recovery_dialog_failure_btn);
        Button recoveryCritFailureButton = alertDialogView.findViewById(R.id.recovery_dialog_crit_failure_btn);

        //Bind view to the dialog builder and create it
        builder.setView(alertDialogView);
        final AlertDialog dialog = builder.create();

        // Set views
        recoveryDcTextView.setText(context.getString(R.string.dialog_recovery_dc, character.getRecoveryDC()+character.getDyingValue()+character.getWoundedValue()));

        recoveryCritSuccessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decreaseCharacterDyingValue(character, 2, context);
                dialog.dismiss();
            }
        });

        recoverySuccessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decreaseCharacterDyingValue(character, 1, context);
                dialog.dismiss();
            }
        });

        recoveryFailureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increaseCharacterDyingValue(character, 1, context);
                dialog.dismiss();
            }
        });

        recoveryCritFailureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increaseCharacterDyingValue(character, 2, context);
                dialog.dismiss();
            }
        });

        //Show the main dialog
        dialog.show();
    }
}