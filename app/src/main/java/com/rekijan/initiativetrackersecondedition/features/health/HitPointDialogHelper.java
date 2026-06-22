package com.rekijan.initiativetrackersecondedition.features.health;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.AppExtension;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;

/**
 * Helper class for managing character HP changes and related rule triggers.
 * Handles massive HP resets for PCs, automatic healing (Fast Healing/Regeneration),
 * and transitions to dying or dead states based on HP reaching 0.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 24-3-2021
 */
public class HitPointDialogHelper {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private static HitPointDialogHelper sInstance = null;
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Initialization    ////////////////////////////////////////////////////////////////////////////////////////////////    public static synchronized HitPointDialogHelper getInstance() {        if (sInstance == null) {
            sInstance = new HitPointDialogHelper();
        }
        return sInstance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods    ////////////////////////////////////////////////////////////////////////////////////////////////    public void setPCsToMaxHP(AppExtension app) {        StringBuilder unUpdatedPcNames = new StringBuilder();
        boolean isNotUpdated = false;
        boolean isWoundedValueReset = false;
        for (CharacterModel c : app.getInitiativeManager().getCharacters()) {
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
        app.getInitiativeAdapter().notifyItemRangeChanged(0, app.getInitiativeAdapter().getItemCount());
    }

    public void automaticHealingCheck(final CharacterModel characterModel, final Context context) {
        if (characterModel.getFastHealing() > 0)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
            builder.setMessage(context.getString(R.string.auto_healing_dialog_fast_healing_message))
                    .setTitle(context.getString(R.string.auto_healing_dialog_fast_healing_title));
            builder.setPositiveButton(context.getString(R.string.dialog_yes), (dialog, id) -> addAutomaticHealing(characterModel, characterModel.getFastHealing(), context));
            builder.setNegativeButton(context.getString(R.string.dialog_no), (dialogInterface, i) -> {});
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        if (characterModel.getRegeneration() > 0){
            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
            builder.setMessage(context.getString(R.string.auto_healing_dialog_regeneration_message))
                    .setTitle(context.getString(R.string.auto_healing_dialog_regeneration_title));
            builder.setPositiveButton(context.getString(R.string.dialog_yes), (dialog, id) -> addAutomaticHealing(characterModel, characterModel.getRegeneration(), context));
            builder.setNegativeButton(context.getString(R.string.dialog_no), (dialogInterface, i) -> {});
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * Adds the specified healing amount and handles over-healing logic.
     */
    private void addAutomaticHealing(CharacterModel characterModel, int healing, Context context) {
        final AppExtension mApp = (AppExtension) context.getApplicationContext();
        characterModel.setHp(characterModel.getHp()+healing);
        mApp.getInitiativeAdapter().notifyCharacterChanged(characterModel);
        if (characterModel.getHp() > characterModel.getMaxHp()) {
            resolveExceededMaxHpDialog(characterModel, context);
        }
        handleCharacterHpChange(characterModel, characterModel.getHp(), context);
    }

    /**
     * Asks the user whether to cap automatic healing at the character's Max HP.
     */
    private void resolveExceededMaxHpDialog(final CharacterModel characterModel, final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        builder.setMessage(context.getString(R.string.auto_healing_dialog_max_exceeded_message))
                .setTitle(context.getString(R.string.auto_healing_dialog_max_exceeded_title));
        builder.setPositiveButton(context.getString(R.string.dialog_yes), (dialog, id) -> {
            final AppExtension mApp = (AppExtension) context.getApplicationContext();
            characterModel.setHp(characterModel.getMaxHp());
            mApp.getInitiativeAdapter().notifyCharacterChanged(characterModel);
        });
        builder.setNegativeButton(context.getString(R.string.dialog_no), (dialogInterface, i) -> {});
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void handleCharacterHpChange(final CharacterModel character, int newValue, final Context context) {
        final AppExtension app = (AppExtension) context.getApplicationContext();
        //If the character is an NPC and the new hp value is less than or equal to zero then it is probably dead, ask if the user wants to remove it
        if (!character.isPC() && newValue <= 0 && !character.isDying()) {
            //Build second dialog
            AlertDialog.Builder removalBuilder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
            removalBuilder.setTitle(context.getString(R.string.dialog_delete_low_title))
                    .setMessage(context.getString(R.string.dialog_delete_low));
            removalBuilder.setPositiveButton(context.getString(R.string.dialog_delete_low_positive), (dialogInterface, i) -> {
                app.getInitiativeAdapter().remove(character);
            });
            removalBuilder.setNegativeButton(context.getString(R.string.dialog_delete_low_negative), (dialogInterface, i) -> DyingDialogHelper.getInstance().setCharacterToDyingDialog(character, context));

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
            pcDyingBuilder.setPositiveButton(context.getString(R.string.dialog_dying_pc_positive), (dialogInterface, i) -> DyingDialogHelper.getInstance().setCharacterToDyingDialog(character, context));
            pcDyingBuilder.setNegativeButton(context.getString(R.string.dialog_pc_dying_negative), (dialogInterface, i) -> {

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
        app.getInitiativeAdapter().notifyCharacterChanged(character);
    }
}