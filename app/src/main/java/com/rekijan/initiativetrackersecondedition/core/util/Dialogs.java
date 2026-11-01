package com.rekijan.initiativetrackersecondedition.core.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * One place where the app's dialogs are built: MaterialAlertDialogBuilder, so they follow the
 * theme (Cinzel title, gold buttons, see ThemeOverlay.Rekijan.Dialog). Their dismiss listener only
 * runs when the user closes them, not when {@link DialogHost} closes them for a rotation; set it on the
 * builder, not on the dialog.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 27-10-2026
 */
public final class Dialogs {

    private static final String LOG_TAG = "Dialogs";

    private Dialogs() {
    }

    public static MaterialAlertDialogBuilder builder(Context context) {
        return new HostedBuilder(context);
    }

    /**
     * Tells {@link DialogHost} when a dialog closes, and keeps its dismiss listener from running
     * when DialogHost closed it for a rotation.
     */
    private static final class HostedBuilder extends MaterialAlertDialogBuilder {

        @Nullable
        private DialogInterface.OnDismissListener onDismiss;

        HostedBuilder(Context context) {
            super(context);
        }

        @NonNull
        @Override
        public MaterialAlertDialogBuilder setOnDismissListener(@Nullable DialogInterface.OnDismissListener listener) {
            onDismiss = listener;
            return this;
        }

        @NonNull
        @Override
        public AlertDialog create() {
            AlertDialog dialog = super.create();
            DialogHost.onCreated(dialog);
            DialogInterface.OnDismissListener listener = onDismiss;
            dialog.setOnDismissListener(d -> {
                if (DialogHost.onDismissed(d) && listener != null) listener.onDismiss(d);
            });
            return dialog;
        }
    }

    /** Opens a web page, or tells the user when there is no browser. */
    public static void openUrl(Context context, String url) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, url, Toast.LENGTH_LONG).show();
        }
    }

    /** Reads a text file from res/raw. Returns an empty string if it cannot be read. */
    public static String readRawText(Context context, @RawRes int rawResId) {
        StringBuilder text = new StringBuilder();
        try (InputStream in = context.getResources().openRawResource(rawResId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Could not read raw resource " + rawResId, e);
        }
        return text.toString();
    }
}
