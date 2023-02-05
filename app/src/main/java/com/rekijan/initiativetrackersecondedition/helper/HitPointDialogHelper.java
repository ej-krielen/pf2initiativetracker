package com.rekijan.initiativetrackersecondedition.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
public class HitPointDialogHelper {

    private static HitPointDialogHelper sInstance = null;

    public static synchronized HitPointDialogHelper getInstance() {
        if (sInstance == null) {
            sInstance = new HitPointDialogHelper();
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
                    DyingDialogHelper.getInstance().setCharacterToDyingDialog(character, context);
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
                    DyingDialogHelper.getInstance().setCharacterToDyingDialog(character, context);
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
            DyingDialogHelper.getInstance().increaseDyingDialog(character, context);
            character.setHp(0);
        }
    }
}