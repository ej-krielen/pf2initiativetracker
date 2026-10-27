package com.rekijan.initiativetrackersecondedition.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests {@link DialogQueue}: a step only starts after the previous one is done, and calling done
 * twice does not skip a step.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 28-10-2026
 */
public class DialogQueueTest {

    @Test
    public void nextStepWaitsUntilThePreviousOneIsDone() {
        List<String> shown = new ArrayList<>();
        Runnable[] firstDone = new Runnable[1];
        DialogQueue.run(Arrays.asList(
                done -> {
                    shown.add("persistent damage");
                    firstDone[0] = done;
                },
                done -> {
                    shown.add("recovery check");
                    done.run();
                }));

        assertEquals(Arrays.asList("persistent damage"), shown);
        firstDone[0].run();
        assertEquals(Arrays.asList("persistent damage", "recovery check"), shown);
    }

    @Test
    public void doneTwiceDoesNotSkipAStep() {
        List<String> shown = new ArrayList<>();
        DialogQueue.run(Arrays.asList(
                done -> {
                    shown.add("a");
                    done.run();
                    done.run();
                },
                done -> shown.add("b"),
                done -> shown.add("c")));

        assertEquals(Arrays.asList("a", "b"), shown);
    }

    @Test
    public void onceRunsOnlyOnce() {
        int[] calls = {0};
        Runnable once = DialogQueue.once(() -> calls[0]++);
        once.run();
        once.run();
        assertEquals(1, calls[0]);
    }
}
