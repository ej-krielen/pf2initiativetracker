package com.rekijan.initiativetrackersecondedition.features.initiative;

import android.widget.TextView;

/**
 * Manages the round counter and its associated header UI.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 21-6-2026
 */
public class RoundController {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members
    ////////////////////////////////////////////////////////////////////////////////////////////////    private int roundCounter;
    private final TextView counterTextView;

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Constructor
    ////////////////////////////////////////////////////////////////////////////////////////////////    public RoundController(int initialValue, TextView counterTextView) {
        this.roundCounter = initialValue;
        this.counterTextView = counterTextView;
        updateCounterDisplay();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////    public void changeRound(int diff) {
        roundCounter = Math.max(0, roundCounter + diff);
        updateCounterDisplay();
    }

    public void resetRound(int value) {
        roundCounter = value;
        updateCounterDisplay();
    }

    public int getRoundCounter() {
        return roundCounter;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Private helpers
    ////////////////////////////////////////////////////////////////////////////////////////////////    private void updateCounterDisplay() {
        if (counterTextView != null) {
            counterTextView.setText(String.valueOf(roundCounter));
        }
    }
}