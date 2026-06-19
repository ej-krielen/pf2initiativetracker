package com.rekijan.initiativetrackersecondedition.ui.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import com.android.billingclient.api.ProductDetails;
import com.rekijan.initiativetrackersecondedition.AppExtension;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.helper.BillingClientHelper;
import com.rekijan.initiativetrackersecondedition.helper.DialogHelper;
import com.rekijan.initiativetrackersecondedition.ui.fragments.CharacterDetailFragment;
import com.rekijan.initiativetrackersecondedition.ui.fragments.DebuffWizardFragment;
import com.rekijan.initiativetrackersecondedition.ui.fragments.EditOrderFragment;
import com.rekijan.initiativetrackersecondedition.ui.fragments.MainActivityFragment;
import com.rekijan.initiativetrackersecondedition.ui.fragments.ReactionsDetailFragment;

public class MainActivity extends AppCompatActivity {

    private BillingClientHelper billingClientHelper;

    @Override
    protected void onDestroy() {
        if (billingClientHelper != null) {
            billingClientHelper.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

            if (getResources().getBoolean(R.bool.isTablet) && !app.getCharacterAdapter().getList().isEmpty()) {
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

    public boolean onOptionsItemSelected(MenuItem item) {
        //If done in fragment instead, return false

        //If not on tablet and a CharacterDetailFragment is present pop it from the stack before continuing
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);
        boolean fragmentExists = getSupportFragmentManager().getBackStackEntryCount() > 0;

        int itemId = item.getItemId();

        if (itemId == R.id.action_settings_next_turn) {
            if (!isTablet && fragmentExists) onSupportNavigateUp();
            nextTurn();
            return false;
        } else if (itemId == R.id.action_settings_add_character) {
            if (!isTablet && fragmentExists) onSupportNavigateUp();
            return false;
        } else if (itemId == R.id.action_settings_about) {
            DialogHelper.getInstance().aboutInfo(MainActivity.this);
            return false;
        } else if (itemId == R.id.action_settings_privacy) {
            DialogHelper.getInstance().privacyPolicyInfo(MainActivity.this);
            return false;
        } else if (itemId == R.id.action_settings_tip) {
            billingClientHelper.queryPurchases();
            openTipDialog();
            return false;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void nextTurn() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (getResources().getBoolean(R.bool.isTablet)) {
            transaction.replace(R.id.second_fragment_container, CharacterDetailFragment.newInstance(0));
            transaction.commit();
        }
    }

    public void replaceCharacterDetailFragment(int position) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (getResources().getBoolean(R.bool.isTablet)) {
            transaction.replace(R.id.second_fragment_container, CharacterDetailFragment.newInstance(position));
            transaction.commit();
        } else {
            transaction.replace(R.id.main_fragment_container, CharacterDetailFragment.newInstance(position));
            transaction.addToBackStack(null);
            transaction.commit();
            //Enable the back button in action bar
            if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            AppExtension app = (AppExtension) this.getApplicationContext();
            app.setShowBackNavigation(true);
        }
    }

    public void replaceReactionDetailFragment(int position) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (getResources().getBoolean(R.bool.isTablet)) {
            transaction.replace(R.id.second_fragment_container, ReactionsDetailFragment.newInstance(position));
            transaction.commit();
        } else {
            transaction.replace(R.id.main_fragment_container, ReactionsDetailFragment.newInstance(position));
            transaction.addToBackStack(null);
            transaction.commit();
            //Enable the back button in action bar
            if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            AppExtension app = (AppExtension) this.getApplicationContext();
            app.setShowBackNavigation(true);
        }
    }

    public void replaceDebuffWizardFragment(int position) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (getResources().getBoolean(R.bool.isTablet)) {
            transaction.replace(R.id.second_fragment_container, DebuffWizardFragment.newInstance(position));
            transaction.commit();
        } else {
            transaction.replace(R.id.main_fragment_container, DebuffWizardFragment.newInstance(position));
            transaction.addToBackStack(null);
            transaction.commit();
            //Enable the back button in action bar
            if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            AppExtension app = (AppExtension) this.getApplicationContext();
            app.setShowBackNavigation(true);
        }
    }

    public void replaceEditOrderFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_fragment_container, EditOrderFragment.newInstance());
        transaction.addToBackStack(null);
        transaction.commit();
        //Enable the back button in action bar
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        AppExtension app = (AppExtension) this.getApplicationContext();
        app.setShowBackNavigation(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        //Disable the back button in action bar only if it is the last fragment
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 1);

        AppExtension app = (AppExtension) this.getApplicationContext();
        app.setShowBackNavigation(getSupportFragmentManager().getBackStackEntryCount() > 1);
        getSupportFragmentManager().popBackStack();
        return true;
    }

    public void openTipDialog() {
        //Build a dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogStyle);
        //Add custom layout to dialog
        LayoutInflater inflater = LayoutInflater.from(this);
        final View alertDialogView = inflater.inflate(R.layout.dialog_tip, null);
        builder.setTitle(this.getString(R.string.tip_dialog_title));
        //Set button to close and cancel
        builder.setNegativeButton(this.getString(R.string.dialog_cancel), (dialogInterface, i) -> {});

        //Get views
        TextView fullFivePriceTextView = alertDialogView.findViewById(R.id.five_full_price_textView);
        TextView fullTenPriceTextView = alertDialogView.findViewById(R.id.ten_full_price_textView);
        Button fiveButton = alertDialogView.findViewById(R.id.five_btn);
        Button tenButton = alertDialogView.findViewById(R.id.ten_btn);

        ProductDetails fiveProductDetails = billingClientHelper.getFiveProductDetails();
        ProductDetails tenProductDetails = billingClientHelper.getTenProductDetails();

        if (fiveProductDetails != null && fiveProductDetails.getOneTimePurchaseOfferDetails() != null)
            fullFivePriceTextView.setText(fiveProductDetails.getOneTimePurchaseOfferDetails().getFormattedPrice());
        if (tenProductDetails != null && tenProductDetails.getOneTimePurchaseOfferDetails() != null)
            fullTenPriceTextView.setText(tenProductDetails.getOneTimePurchaseOfferDetails().getFormattedPrice());

        //Bind view to the dialog builder and create it
        builder.setView(alertDialogView);
        final AlertDialog dialog = builder.create();

        fiveButton.setOnClickListener(view -> {
            billingClientHelper.launchBillingFlow(BillingClientHelper.ITEM_SKU_FIVE);
            dialog.dismiss();
        });

        tenButton.setOnClickListener(view -> {
            billingClientHelper.launchBillingFlow(BillingClientHelper.ITEM_SKU_TEN);
            dialog.dismiss();
        });

        //Show the main dialog
        dialog.show();
    }

}
