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
public class HeroPointDialogHelper {

    private static HeroPointDialogHelper sInstance = null;

    public static synchronized HeroPointDialogHelper getInstance() {
        if (sInstance == null) {
            sInstance = new HeroPointDialogHelper();
        }
        return sInstance;
    }


    /**
     * Ask if the character wants to use its remaining hero points to negate the dying (offered when dying value increases)<br>
     *     If yes then handle it in {@link #useHeroPoint(CharacterModel, Context)} otherwise goes to {@link DyingDialogHelper#checkIfCharacterDied(CharacterModel, Context)}
     * @param character
     * @param context
     */
    void promptHeroPointUseDialog(final CharacterModel character, final Context context)
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
                    DyingDialogHelper.getInstance().checkIfCharacterDied(character, context);
                }
            });

            AlertDialog removalDialog = useHeroPointDialog.create();
            removalDialog.show();
        } else {
            DyingDialogHelper.getInstance().checkIfCharacterDied(character, context);
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
        Toast.makeText(context, context.getString(R.string.toast_recovered_from_dying_by_hero_points, character.getCharacterName()), Toast.LENGTH_LONG).show();
    }
}