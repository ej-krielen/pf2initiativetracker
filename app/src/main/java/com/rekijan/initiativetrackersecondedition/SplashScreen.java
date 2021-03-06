package com.rekijan.initiativetrackersecondedition;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.rekijan.initiativetrackersecondedition.ui.activities.MainActivity;
import static com.rekijan.initiativetrackersecondedition.AppConstants.*;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //Show splash screen with a timer.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // This method will be executed once the timer is over
                startActivity(new Intent(SplashScreen.this, MainActivity.class));
                // Close this activity
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}