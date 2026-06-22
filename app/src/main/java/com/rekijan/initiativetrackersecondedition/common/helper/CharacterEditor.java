package com.rekijan.initiativetrackersecondedition.common.helper;

import android.app.AlertDialog;
import android.content.Context;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.AppExtension;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;
import com.rekijan.initiativetrackersecondedition.data.model.DebuffModel;
import com.rekijan.initiativetrackersecondedition.data.model.ReactionModel;

/**
 * Helper class for performing bulk updates or complex logic on character models.
 * Handles debuff duration tracking and reaction resets.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 20-6-2026
 */
public class CharacterEditor {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private static CharacterEditor sInstance = null;
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Initialization    ////////////////////////////////////////////////////////////////////////////////////////////////    public static synchronized CharacterEditor getInstance () {        if (sInstance == null) {
            sInstance = new CharacterEditor();
        }
        return sInstance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods    ////////////////////////////////////////////////////////////////////////////////////////////////    public void updateDebuffs(Context context, CharacterModel characterModel) {        // We iterate over a copy or use a traditional loop to avoid concurrent modification issues
        // though since dialogs are async, the safest way is to handle removal by object reference.
        for (DebuffModel d: characterModel.getDebuffList()) {
            int duration = d.getDuration();
            duration--;
            if (duration == 0) askRoundResetConfirmation(context, characterModel, d);
            if (duration < 0) duration = 0;
            d.setDuration(duration);
        }
    }

    public void updateReactions(CharacterModel characterModel) {
        characterModel.setReactionAvailable(true);
        for (ReactionModel r: characterModel.getReactionList()) {
            r.setAvailable(true);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Private helpers    ////////////////////////////////////////////////////////////////////////////////////////////////    /**     * Shows a confirmation dialog when a debuff expires, allowing the user to keep it or remove it.
     */
    private void askRoundResetConfirmation(Context context, CharacterModel characterModel, DebuffModel debuff) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        builder.setMessage(String.format(context.getString(R.string.debuff_expired), debuff.getName()))
                .setTitle(context.getString(R.string.dialog_debuff_expired_title));
        builder.setPositiveButton(context.getString(R.string.dialog_debuff_remove), (dialog, id) -> {
            characterModel.getDebuffList().remove(debuff);
            // Notify the adapter to refresh the card
            AppExtension app = (AppExtension) context.getApplicationContext();
            if (app.getInitiativeAdapter() != null) {
                app.getInitiativeAdapter().notifyCharacterChanged(characterModel);
            }
        });
        builder.setNegativeButton(context.getString(R.string.dialog_debuff_keep), (dialogInterface, i) -> {});

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}