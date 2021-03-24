package com.rekijan.initiativetrackersecondedition.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

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

    public void simpleDialog(Activity activity, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogStyle);
        builder.setMessage(message)
                .setTitle(title);
        builder.setNegativeButton(activity.getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
