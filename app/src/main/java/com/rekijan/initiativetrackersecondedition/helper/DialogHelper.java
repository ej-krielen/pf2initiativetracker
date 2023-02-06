package com.rekijan.initiativetrackersecondedition.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import com.rekijan.initiativetrackersecondedition.R;

/**
 * Helper class to make simple dialogs
 *
 * @author Erik-Jan Krielen ej.krielen@gmail.com
 * @since 24-3-2021
 */
public class DialogHelper {

    private static DialogHelper sInstance = null;

    public static synchronized DialogHelper getInstance() {
        if (sInstance == null) {
            sInstance = new DialogHelper();
        }
        return sInstance;
    }

    public void simpleDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        builder.setMessage(message)
                .setTitle(title);
        builder.setNegativeButton(context.getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Show dialog explaining more info is available on the website
     */
    public void aboutInfo(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        builder.setMessage(context.getString(R.string.dialog_about_info))
                .setTitle(context.getString(R.string.dialog_about_info_title));
        builder.setPositiveButton(context.getString(R.string.dialog_about_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent siteIntent =
                        new Intent("android.intent.action.VIEW",
                                Uri.parse("http://www.rekijan.nl/"));
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

    /**
     * Show dialog explaining more info is available on the website
     */
    public void privacyPolicyInfo(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        builder.setMessage(context.getString(R.string.dialog_privacy_info))
                .setTitle(context.getString(R.string.dialog_privacy_info_title));
        builder.setPositiveButton(context.getString(R.string.dialog_about_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent siteIntent =
                        new Intent("android.intent.action.VIEW",
                                Uri.parse("https://rekijan.nl/privacypolicy.php"));
                context.startActivity(siteIntent);
            }
        });
        builder.setNegativeButton(context.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Show dialog explaining more info about death and dying rules
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
