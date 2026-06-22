package com.rekijan.initiativetrackersecondedition.common.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;

import com.rekijan.initiativetrackersecondedition.R;

import java.math.BigInteger;

/**
 * Helper class for creating standard and custom Android dialogs.
 * Provides utility methods for common UI patterns like confirmation dialogs and safe integer parsing.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 20-6-2026
 */
public class DialogHelper {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members
    ////////////////////////////////////////////////////////////////////////////////////////////////    private static DialogHelper sInstance = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Initialization
    ////////////////////////////////////////////////////////////////////////////////////////////////    public static synchronized DialogHelper getInstance () {
        if (sInstance == null) {
            sInstance = new DialogHelper();
        }
        return sInstance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////    public void simpleDialog (Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        builder.setMessage(message).setTitle(title);
        builder.setNegativeButton(context.getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialogInterface, int i) {
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Safely parses a string to an integer, handling empty strings and potential overflows.
     *
     * @param text The string to parse.
     * @return The parsed integer, or 0 if parsing fails. Capped at Integer.MAX_VALUE.
     */
    public int parseIntSafe (String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        try {
            BigInteger maxInt = BigInteger.valueOf(Integer.MAX_VALUE);
            BigInteger value = new BigInteger(text);
            if (value.compareTo(maxInt) > 0) {
                return Integer.MAX_VALUE;
            }
            return value.intValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public View initCustomDialog(Context context, AlertDialog.Builder builder, int layoutResId, String title) {
        return initCustomDialog(context, builder, layoutResId, title, null);
    }

    public View initCustomDialog(Context context, AlertDialog.Builder builder, int layoutResId, String title, String cancelButtonText) {
        View view = LayoutInflater.from(context).inflate(layoutResId, null);
        String buttonText = cancelButtonText != null ? cancelButtonText : context.getString(R.string.dialog_cancel);
        builder.setView(view).setTitle(title).setNegativeButton(buttonText, (dialog, which) -> {
        });
        return view;
    }

    public void confirmationDialog(Context context, String title, String message, Runnable onConfirm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> onConfirm.run())
                .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> {});
        builder.show();
    }
}