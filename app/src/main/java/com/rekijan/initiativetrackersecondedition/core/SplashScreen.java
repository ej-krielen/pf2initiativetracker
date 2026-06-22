package com.rekijan.initiativetrackersecondedition.core;

import static com.rekijan.initiativetrackersecondedition.core.AppConstants.SPLASH_TIME_OUT;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.rekijan.initiativetrackersecondedition.R;

/**
 * Entry activity that displays the application branding.
 * Automatically transitions to {@link MainActivity} after a 1-second delay.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 18-12-2015
 */
@SuppressLint("CustomSplashScreen")
public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Using Looper.getMainLooper() resolves the deprecation warning
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashScreen.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_TIME_OUT);
    }
}