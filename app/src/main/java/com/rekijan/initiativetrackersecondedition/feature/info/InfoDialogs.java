package com.rekijan.initiativetrackersecondedition.feature.info;

import android.content.Context;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.core.util.DialogHost;
import com.rekijan.initiativetrackersecondedition.core.util.Dialogs;

/**
 * The info dialogs from the menu: About (links to the website), Privacy Policy (short summary
 * with a link to the full policy), how to delete a character, and what happens at the start and
 * end of a turn.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public final class InfoDialogs {

    private static final String WEBSITE_URL = "https://rekijan.nl/";
    private static final String PRIVACY_URL = "https://rekijan.nl/privacypolicy-rekijancombattracker.php";

    private InfoDialogs() {
    }

    public static void about(Context context) {
        DialogHost.show(c -> Dialogs.builder(c)
                .setTitle(R.string.dialog_about_info_title)
                .setMessage(R.string.dialog_about_info)
                .setPositiveButton(R.string.dialog_about_ok, (d, w) -> Dialogs.openUrl(c, WEBSITE_URL))
                .setNeutralButton(R.string.licenses_button, (d, w) -> licenses(c))
                .setNegativeButton(R.string.dialog_cancel, null)
                .create());
    }

    /**
     * Shows the ORC notice for the game rules text, and the licenses of the icons and the bundled
     * fonts (Manrope, Cinzel). The ORC License asks for the notice to ship with the app; the SIL Open Font
     * License asks for the font license text to ship with the app.
     */
    public static void licenses(Context context) {
        DialogHost.show(c -> Dialogs.builder(c)
                .setTitle(R.string.licenses_title)
                .setMessage(Dialogs.readRawText(c, R.raw.licenses))
                .setPositiveButton(R.string.dialog_ok, null)
                .create());
    }

    public static void privacyPolicy(Context context) {
        DialogHost.show(c -> Dialogs.builder(c)
                .setTitle(R.string.dialog_privacy_info_title)
                .setMessage(R.string.dialog_privacy_info)
                .setPositiveButton(R.string.dialog_about_ok, (d, w) -> Dialogs.openUrl(c, PRIVACY_URL))
                .setNegativeButton(R.string.dialog_cancel, null)
                .create());
    }

    public static void howToDeleteCharacter(Context context) {
        message(context, R.string.dialog_delete_info_title, R.string.dialog_delete_info);
    }

    public static void startOfTurn(Context context) {
        message(context, R.string.dialog_turn_start_info_title, R.string.dialog_turn_start_info);
    }

    public static void endOfTurn(Context context) {
        message(context, R.string.dialog_turn_end_info_title, R.string.dialog_turn_end_info);
    }

    private static void message(Context context, int titleRes, int messageRes) {
        DialogHost.show(c -> Dialogs.builder(c)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setPositiveButton(R.string.dialog_ok, null)
                .create());
    }
}
