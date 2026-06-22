package com.rekijan.initiativetrackersecondedition.features.health;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import com.rekijan.initiativetrackersecondedition.core.AppExtension;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.data.model.CharacterModel;

/**
 * Helper class for managing Hero Point usage during critical gameplay moments.
 * Currently primarily used to prevent character death when the dying value increases.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 24-3-2021
 */
public class HeroPointDialogHelper {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private static HeroPointDialogHelper sInstance = null;
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Initialization    ////////////////////////////////////////////////////////////////////////////////////////////////    public static synchronized HeroPointDialogHelper getInstance() {        if (sInstance == null) {
            sInstance = new HeroPointDialogHelper();
        }
        return sInstance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods    ////////////////////////////////////////////////////////////////////////////////////////////////    void promptHeroPointUseDialog(final CharacterModel character, final Context context)    {
        if (character.getHeroPoints() > 0) {
            //Build dialog
            AlertDialog.Builder useHeroPointDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
            useHeroPointDialog.setTitle(context.getString(R.string.dialog_dying_hero_point_title))
                    .setMessage(context.getString(R.string.dialog_dying_hero_point));
            useHeroPointDialog.setPositiveButton(context.getString(R.string.dialog_dying_hero_point_positive), (dialogInterface, i) -> useHeroPoint(character, context));
            useHeroPointDialog.setNegativeButton(context.getString(R.string.dialog_dying_hero_point_negative), (dialogInterface, i) -> DyingDialogHelper.getInstance().checkIfCharacterDied(character, context));

            AlertDialog removalDialog = useHeroPointDialog.create();
            removalDialog.show();
        } else {
            DyingDialogHelper.getInstance().checkIfCharacterDied(character, context);
        }
    }

    /**
     * Stabilizes a character at the cost of all their hero points.
     * Sets hero points to 0, HP to 0, and removes the dying state.
     */
    private void useHeroPoint(CharacterModel character, Context context) {
        character.setHeroPoints(0);
        character.setIsDying(false);
        character.setDyingValue(0);
        character.setHp(0);
        AppExtension app = (AppExtension) context.getApplicationContext();
        app.getInitiativeAdapter().notifyCharacterChanged(character);
        Toast.makeText(context, context.getString(R.string.toast_recovered_from_dying_by_hero_points, character.getCharacterName()), Toast.LENGTH_LONG).show();
    }
}