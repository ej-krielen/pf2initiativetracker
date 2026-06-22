package com.rekijan.initiativetrackersecondedition.core;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.features.billing.BillingClientHelper;
import com.rekijan.initiativetrackersecondedition.features.character.CharacterDetailFragment;
import com.rekijan.initiativetrackersecondedition.features.debuffs.DebuffWizardFragment;
import com.rekijan.initiativetrackersecondedition.features.reactions.ReactionsDetailFragment;
import com.rekijan.initiativetrackersecondedition.features.editorder.EditOrderFragment;

/**
 * The main activity of the application.
 * Manages fragment navigation and top-level menu actions.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 17-9-2015
 */
public class MainActivity extends AppCompatActivity {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private BillingClientHelper billingClientHelper;
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Override methods    ////////////////////////////////////////////////////////////////////////////////////////////////    @Override    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setPopupTheme(R.style.PopupMenuStyle);

        AppExtension app = (AppExtension) this.getApplicationContext();
        app.initializeData(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment_container, MainActivityFragment.newInstance())
                    .commit();

            if (getResources().getBoolean(R.bool.isTablet) && !app.getInitiativeManager().getCharacters().isEmpty()) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.second_fragment_container, CharacterDetailFragment.newInstance(0))
                        .commit();
            }
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(app.showBackNavigation());
        }

        billingClientHelper = new BillingClientHelper(this, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        //Disable the back button in action bar only if it is the last fragment
        boolean showBack = getSupportFragmentManager().getBackStackEntryCount() > 1;
        toggleBackNavigation(showBack);
        getSupportFragmentManager().popBackStack();
        return true;
    }

    @Override
    protected void onDestroy() {
        if (billingClientHelper != null) {
            billingClientHelper.onDestroy();
        }
        super.onDestroy();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Public helpers    ////////////////////////////////////////////////////////////////////////////////////////////////    public boolean onOptionsItemSelected(MenuItem item) {        int itemId = item.getItemId();

        if (itemId == R.id.action_settings_next_turn) {
            closeDetailIfMobile();
            nextTurn();
            return false;
        } else if (itemId == R.id.action_settings_add_character) {
            closeDetailIfMobile();
            return false;
        } else if (itemId == R.id.action_settings_about) {
            CommonDialogHelper.getInstance().aboutInfo(this);
            return false;
        } else if (itemId == R.id.action_settings_privacy) {
            CommonDialogHelper.getInstance().privacyPolicyInfo(this);
            return false;
        } else if (itemId == R.id.action_settings_tip) {
            CommonDialogHelper.getInstance().openTipDialog(this, billingClientHelper);
            return false;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods    ////////////////////////////////////////////////////////////////////////////////////////////////    /**     * Navigates to the Character Detail fragment.
     *
     * @param position The index of the character to display.
     */
    public void replaceCharacterDetailFragment(int position) {
        navigateTo(CharacterDetailFragment.newInstance(position), true);
    }

    /**
     * Navigates to the Reactions Detail fragment.
     *
     * @param position The index of the character to display reactions for.
     */
    public void replaceReactionDetailFragment(int position) {
        navigateTo(ReactionsDetailFragment.newInstance(position), true);
    }

    /**
     * Navigates to the Debuff Wizard fragment.
     *
     * @param position The index of the character to add debuffs to.
     */
    public void replaceDebuffWizardFragment(int position) {
        navigateTo(DebuffWizardFragment.newInstance(position), true);
    }

    /**
     * Navigates to the Edit Order screen.
     */
    public void replaceEditOrderFragment() {
        navigateTo(EditOrderFragment.newInstance(), true);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Private helpers    ////////////////////////////////////////////////////////////////////////////////////////////////    /**     * Toggles the display of the back arrow in the action bar.
     */
    private void toggleBackNavigation(boolean show) {
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(show);
        AppExtension app = (AppExtension) this.getApplicationContext();
        app.setShowBackNavigation(show);
    }

    /**
     * Closes the detail fragment if the device is a phone (not a tablet).
     */
    private void closeDetailIfMobile() {
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);
        boolean fragmentExists = getSupportFragmentManager().getBackStackEntryCount() > 0;
        if (!isTablet && fragmentExists) onSupportNavigateUp();
    }

    /**
     * Triggers turn logic updates for tablet UI.
     */
    private void nextTurn() {
        if (getResources().getBoolean(R.bool.isTablet)) {
            navigateTo(CharacterDetailFragment.newInstance(0), false);
        }
    }

    /**
     * Standardized navigation logic handling tablet/mobile containers and backstack.
     */
    private void navigateTo(Fragment fragment, boolean allowBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);

        if (isTablet && !(fragment instanceof EditOrderFragment)) {
            transaction.replace(R.id.second_fragment_container, fragment);
        } else {
            transaction.replace(R.id.main_fragment_container, fragment);
            if (allowBackStack) {
                transaction.addToBackStack(null);
                toggleBackNavigation(true);
            }
        }
        transaction.commit();
    }
}