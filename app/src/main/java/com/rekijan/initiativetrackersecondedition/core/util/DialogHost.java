package com.rekijan.initiativetrackersecondedition.core.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Keeps the open dialogs through a rotation. A dialog is shown from a {@link Spec} that builds it
 * for the activity in front. When that activity is recreated, the dialogs close without running
 * their listeners or buttons, and are built again for the new activity with the state of their
 * views (typed text, ticked boxes). So a question that is still open, and the questions queued
 * after it ({@link DialogQueue}), are asked once, on the new screen. A dialog the user closes is
 * forgotten; when the activity finishes, all of them are. Dialogs shown without a Spec, whose
 * buttons belong to the screen that opened them, close quietly on a rotation.
 * <p>
 * State a dialog changes outside its views (for example "the damage was taken") belongs outside
 * the Spec, so the rebuilt dialog shows it too.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 01-11-2026
 */
public final class DialogHost {

    /** Builds a dialog (without showing it) for the given activity. */
    public interface Spec {
        @NonNull
        AlertDialog create(@NonNull Context context);
    }

    private static final class Entry {
        final Spec spec;
        @Nullable
        AlertDialog dialog;
        @Nullable
        Bundle state;

        Entry(Spec spec) {
            this.spec = spec;
        }
    }

    private static final List<Entry> open = new ArrayList<>();
    /** Dialogs of {@link Dialogs#builder} shown without a Spec; they close on a rotation and are not shown again. */
    private static final Set<DialogInterface> loose = Collections.newSetFromMap(new WeakHashMap<>());
    /** Dialogs this class closes itself; their dismiss listeners must not run. */
    private static final Set<DialogInterface> closing = new HashSet<>();
    private static WeakReference<Activity> activity = new WeakReference<>(null);

    private DialogHost() {
    }

    /**
     * Shows a dialog now, or as soon as an activity is in front again.
     *
     * @return The dialog when it was shown right away, else null.
     */
    @Nullable
    public static AlertDialog show(@NonNull Spec spec) {
        Entry entry = new Entry(spec);
        open.add(entry);
        Activity current = activity.get();
        if (current != null) build(entry, current);
        return entry.dialog;
    }

    /** The activity is in front: dialogs that were waiting for it are built and shown. */
    public static void attach(@NonNull Activity activity) {
        DialogHost.activity = new WeakReference<>(activity);
        for (Entry entry : new ArrayList<>(open)) {
            if (entry.dialog == null) build(entry, activity);
        }
    }

    /**
     * The activity is destroyed. Its dialogs close quietly; they come back on the next activity
     * when this one is only being recreated (rotation, dark theme, font size), else they are dropped.
     */
    public static void detach(@NonNull Activity activity) {
        if (DialogHost.activity.get() != activity) return;
        DialogHost.activity = new WeakReference<>(null);
        boolean recreating = activity.isChangingConfigurations();
        for (DialogInterface dialog : new ArrayList<>(loose)) {
            if (dialog instanceof AlertDialog && ((AlertDialog) dialog).isShowing()) {
                closing.add(dialog);
                dialog.dismiss();
            }
        }
        loose.clear();
        for (Entry entry : new ArrayList<>(open)) {
            AlertDialog dialog = entry.dialog;
            if (dialog == null) continue;
            if (recreating) entry.state = dialog.onSaveInstanceState();
            entry.dialog = null;
            closing.add(dialog);
            dialog.dismiss();
        }
        if (!recreating) open.clear();
    }

    /** Called by every dialog of {@link Dialogs#builder} when it is built. */
    static void onCreated(@NonNull AlertDialog dialog) {
        loose.add(dialog);
    }

    /**
     * Called by every dialog of {@link Dialogs#builder} when it is dismissed.
     *
     * @return False when this class closed it for a rotation, so its own listener must not run.
     */
    static boolean onDismissed(@NonNull DialogInterface dialog) {
        loose.remove(dialog);
        if (closing.remove(dialog)) return false;
        for (int i = 0; i < open.size(); i++) {
            if (open.get(i).dialog == dialog) {
                open.remove(i);
                break;
            }
        }
        return true;
    }

    private static void build(Entry entry, Activity activity) {
        AlertDialog dialog = entry.spec.create(activity);
        loose.remove(dialog);
        entry.dialog = dialog;
        Bundle state = entry.state;
        entry.state = null;
        // Restoring also shows it when it was showing
        if (state != null) dialog.onRestoreInstanceState(state);
        if (!dialog.isShowing()) dialog.show();
    }
}
