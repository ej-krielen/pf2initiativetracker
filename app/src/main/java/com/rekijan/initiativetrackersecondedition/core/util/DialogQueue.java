package com.rekijan.initiativetrackersecondedition.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a series of dialogs one after the other instead of stacked on top of each other. Each
 * step shows its dialog (and any follow-up dialogs) and calls the Runnable it gets once it is
 * completely finished; only then the next step starts. Plain Java, so the order is unit tested.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-10-2026
 */
public final class DialogQueue {

    /** One dialog in the queue. */
    public interface Step {
        /**
         * @param done Call when this step and its follow-ups are finished. Extra calls are ignored.
         */
        void show(Runnable done);
    }

    private DialogQueue() {
    }

    /** Runs the steps in order, each one after the previous one called done. */
    public static void run(List<Step> steps) {
        runFrom(new ArrayList<>(steps), 0);
    }

    /** @return A Runnable that only does something the first time it is called. */
    public static Runnable once(Runnable action) {
        boolean[] called = {false};
        return () -> {
            if (called[0]) return;
            called[0] = true;
            action.run();
        };
    }

    private static void runFrom(List<Step> steps, int index) {
        if (index >= steps.size()) return;
        steps.get(index).show(once(() -> runFrom(steps, index + 1)));
    }
}
