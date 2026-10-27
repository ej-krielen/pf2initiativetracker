package com.rekijan.initiativetrackersecondedition.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.rekijan.initiativetrackersecondedition.R;

/**
 * Shows the splash screen for a second, then opens {@link MainActivity}. To be replaced by the
 * system splash screen in the 2.0 restyle.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 18-12-2015
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, SPLASH_TIME_OUT);
    }
}
