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
public class HitPointAndDyingChangeHelper {

    private static HitPointAndDyingChangeHelper sInstance = null;

    public static synchronized HitPointAndDyingChangeHelper getInstance() {
        if (sInstance == null) {
            sInstance = new HitPointAndDyingChangeHelper();
        }
        return sInstance;
    }

    /**
     * Goes through all characters and checks if they are a PC.<br>
     *     If the pc's max hp value is higher then its current hp value it gets updated,<br>
     *         but if not its name is added to a list and displayed so the user know.<br>
     *             The wounded value of PCs also get reset.
     *
     */
    public void setPCsToMaxHP(AppExtension app) {
        StringBuilder unUpdatedPcNames = new StringBuilder();
        boolean isNotUpdated = false;
        boolean isWoundedValueReset = false;
        for (CharacterModel c : app.getCharacterAdapter().getList()) {
            if (c.isPC()) {
                if (c.getMaxHp() >= c.getHp()) {
                    c.setHp(c.getMaxHp());
                    c.setWoundedValue(0);
                    isWoundedValueReset = true;
                } else {
                    isNotUpdated = true;
                    unUpdatedPcNames.append(c.getCharacterName());
                    unUpdatedPcNames.append("\n");
                }
            }
        }
        if (isNotUpdated) {
            String toastMessage = app.getString(R.string.toast_max_hp, unUpdatedPcNames.toString());
            Toast.makeText(app, toastMessage, Toast.LENGTH_LONG).show();
        }
        if (isWoundedValueReset) {
            Toast.makeText(app, app.getString(R.string.toast_wounded_removed), Toast.LENGTH_SHORT).show();
        }
        app.getCharacterAdapter().notifyDataSetChanged();
    }

    /**
     * If the CharacterModel has fast healing or regeneration user will be prompted if they want to add that.<br>
     *     If user says yes calls {@link #addAutomaticHealing(CharacterModel, int, Context)}
     * @param characterModel
     */
    public void automaticHealingCheck(final CharacterModel characterModel, final Context context) {
        if (characterModel.getFastHealing() > 0)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
            builder.setMessage(context.getString(R.string.auto_healing_dialog_fast_healing_message))
                    .setTitle(context.getString(R.string.auto_healing_dialog_fast_healing_title));
            builder.setPositiveButton(context.getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    addAutomaticHealing(characterModel, characterModel.getFastHealing(), context);
                }
            });
            builder.setNegativeButton(context.getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {}
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        if (characterModel.getRegeneration() > 0){
            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
            builder.setMessage(context.getString(R.string.auto_healing_dialog_regeneration_message))
                    .setTitle(context.getString(R.string.auto_healing_dialog_regeneration_title));
            builder.setPositiveButton(context.getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    addAutomaticHealing(characterModel, characterModel.getRegeneration(), context);
                }
            });
            builder.setNegativeButton(context.getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {}
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * Adds the amount of healing to the CharacterModel, then calls {@link #handleCharacterHpChange(CharacterModel, int, Context)}.<br>
     * If this healing puts the characterModel over its max hp {@link #resolveExceededMaxHpDialog(CharacterModel, Context)} is called to handle that.
     * @param characterModel
     * @param healing
     */
    private void addAutomaticHealing(CharacterModel characterModel, int healing, Context context) {
        final AppExtension mApp = (AppExtension) context.getApplicationContext();
        characterModel.setHp(characterModel.getHp()+healing);
        mApp.getCharacterAdapter().notifyDataSetChanged();
        if (characterModel.getHp() > characterModel.getMaxHp()) {
            resolveExceededMaxHpDialog(characterModel, context);
        }
        handleCharacterHpChange(characterModel, characterModel.getHp(), context);
    }

    /**
     * User gets prompted to add healing in full or to cap it to the characterModel's max hp value
     * @param characterModel
     */
    private void resolveExceededMaxHpDialog(final CharacterModel characterModel, final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        builder.setMessage(context.getString(R.string.auto_healing_dialog_max_exceeded_message))
                .setTitle(context.getString(R.string.auto_healing_dialog_max_exceeded_title));
        builder.setPositiveButton(context.getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                final AppExtension mApp = (AppExtension) context.getApplicationContext();
                characterModel.setHp(characterModel.getMaxHp());
                mApp.getCharacterAdapter().notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(context.getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * After a CharacterModel's hp changed several things might need to be updated.<br>
     *     If a monster's hp goes to 0 or lower user will be asked to remove it.<br>
     *         If a PC goes to 0 or lower user will be asked to set it to dying.<br>
     *             If a character was dying and now is at positive hp it is no longer dying and this will be updated.<br>
     *                 If a character was dying and receives damage it will increase the dying value.<br>
     *                     Various other methods are then called to resolve each issue.
     * @param character
     * @param newValue
     */
    public void handleCharacterHpChange(final CharacterModel character, int newValue, final Context context) {
        final AppExtension app = (AppExtension) context.getApplicationContext();
        //If the character is an NPC and the new hp value is less than or equal to zero then it is probably dead, ask if the user wants to remove it
        if (!character.isPC() && newValue <= 0 && !character.isDying()) {
            //Build second dialog
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
                    setCharacterToDyingDialog(character, context);
                }
            });

            AlertDialog removalDialog = removalBuilder.create();
            removalDialog.show();
        }

        // If the character is a PC and the new hp value is less than zero then it is dying, ask if the user wants to confirm this
        if (character.isPC() && newValue <= 0 && !character.isDying()) {
            character.setHp(0);
            //Build second dialog
            AlertDialog.Builder pcDyingBuilder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
            pcDyingBuilder.setTitle(context.getString(R.string.dialog_dying_pc_title))
                    .setMessage(context.getString(R.string.dialog_dying_pc));
            pcDyingBuilder.setPositiveButton(context.getString(R.string.dialog_dying_pc_positive), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    setCharacterToDyingDialog(character, context);
                }
            });
            pcDyingBuilder.setNegativeButton(context.getString(R.string.dialog_pc_dying_negative), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });

            AlertDialog removalDialog = pcDyingBuilder.create();
            removalDialog.show();
        }

        if (character.isDying() && newValue > 0) {
            character.setIsDying(false);
            character.setDyingValue(0);
            character.setWoundedValue(character.getWoundedValue()+1);
            String toastString = context.getString(R.string.toast_recovered_from_dying_by_healing, character.getCharacterName());
            Toast.makeText(context, toastString, Toast.LENGTH_LONG).show();
        }

        if (character.isDying() && newValue < 0) {
            increaseDyingDialog(character, context);
            character.setHp(0);
        }
    }

    /**
     * Before we can set the Character to dying we first need to ask if it was a critical hit or not because that impacts the value of dying.
     * @param character
     */
    private void setCharacterToDyingDialog(final CharacterModel character, final Context context) {
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
                promptHeroPointUseDialog(character, context);
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
    private void increaseDyingDialog(final CharacterModel character, final Context context) {
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
        promptHeroPointUseDialog(character, context);
    }

    /**
     * Increases the character's dying value by the amount, then check if stabilized
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
            character.setHp(0);
            Toast.makeText(context, context.getString(R.string.toast_recovered_from_dying_by_recovery_checks, character.getCharacterName()), Toast.LENGTH_LONG).show();
        }
        app.getCharacterAdapter().notifyDataSetChanged();
    }


    /**
     * Ask if the character wants to use its remaining hero points to negate the dying (offered when dying value increases)<br>
     *     If yes then handle it in {@link #useHeroPoint(CharacterModel, Context)} otherwise goes to {@link #checkIfCharacterDied(CharacterModel, Context)}
     * @param character
     * @param context
     */
    private void promptHeroPointUseDialog(final CharacterModel character, final Context context)
    {
        if (character.getHeroPoints() > 0) {
            //Build dialog
            AlertDialog.Builder useHeroPointDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
            useHeroPointDialog.setTitle(context.getString(R.string.dialog_dying_hero_point_title))
                    .setMessage(context.getString(R.string.dialog_dying_hero_point));
            useHeroPointDialog.setPositiveButton(context.getString(R.string.dialog_dying_hero_point_positive), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    useHeroPoint(character, context);
                }
            });
            useHeroPointDialog.setNegativeButton(context.getString(R.string.dialog_dying_hero_point_negative), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    checkIfCharacterDied(character, context);
                }
            });

            AlertDialog removalDialog = useHeroPointDialog.create();
            removalDialog.show();
        } else {
            checkIfCharacterDied(character, context);
        }
    }

    /**
     * If the character uses all its hero points to prevent death, set its hero points and hit points to 0 and remove the dying condition.
     * @param character
     * @param context
     */
    private void useHeroPoint(CharacterModel character, Context context) {
        character.setHeroPoints(0);
        character.setIsDying(false);
        character.setDyingValue(0);
        character.setHp(0);
        AppExtension app = (AppExtension) context.getApplicationContext();
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
        final View alertDialogView = inflater.inflate(R.layout.recovery_dialog, null);
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