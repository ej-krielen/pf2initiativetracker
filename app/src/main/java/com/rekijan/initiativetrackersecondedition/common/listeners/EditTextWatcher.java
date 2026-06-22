package com.rekijan.initiativetrackersecondedition.common.listeners;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;

import java.math.BigInteger;
import java.util.function.Consumer;

/**
 * Custom TextWatcher implementation that uses a functional callback for convenience.
 * Primarily used to sync EditText changes directly to model fields.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 26-10-2015
 */
public class EditTextWatcher implements TextWatcher {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private final Consumer<String> callback;
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Initialization    ////////////////////////////////////////////////////////////////////////////////////////////////    public EditTextWatcher(Consumer<String> callback) {        this.callback = callback;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// TextWatcher implementations    ////////////////////////////////////////////////////////////////////////////////////////////////    @Override    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        callback.accept(s.toString());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Helper methods    ////////////////////////////////////////////////////////////////////////////////////////////////    public static int parseInt(String text) {        if (TextUtils.isEmpty(text)) {
            return 0;
        }
        try {
            BigInteger maxInt = BigInteger.valueOf(Integer.MAX_VALUE);
            BigInteger value = new BigInteger(text);
            return value.min(maxInt).intValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}