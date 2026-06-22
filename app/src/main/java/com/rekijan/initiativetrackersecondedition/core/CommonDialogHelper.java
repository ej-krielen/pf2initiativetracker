package com.rekijan.initiativetrackersecondedition.core;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.billingclient.api.ProductDetails;
import com.rekijan.initiativetrackersecondedition.R;
import com.rekijan.initiativetrackersecondedition.features.billing.BillingClientHelper;

/**
 * Helper class for standard informational dialogs.
 * Provides the "About", "Privacy Policy", and "Tip/Donation" UI flows.
 *
 * @author Erik-Jan Krielen rekijan.apps@gmail.com
 * @since 20-6-2026
 */
public class CommonDialogHelper {
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private static CommonDialogHelper sInstance = null;
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Initialization    ////////////////////////////////////////////////////////////////////////////////////////////////    public static synchronized CommonDialogHelper getInstance() {        if (sInstance == null) {
            sInstance = new CommonDialogHelper();
        }
        return sInstance;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods    ////////////////////////////////////////////////////////////////////////////////////////////////    public void aboutInfo(final Context context) {        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        builder.setMessage(context.getString(R.string.dialog_about_info))
                .setTitle(context.getString(R.string.dialog_about_info_title));
        builder.setPositiveButton(context.getString(R.string.dialog_about_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent siteIntent =
                        new Intent("android.intent.action.VIEW",
                                Uri.parse("http://www.rekijan.nl/"));
                context.startActivity(siteIntent);
            }
        });
        builder.setNegativeButton(context.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void privacyPolicyInfo(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        builder.setMessage(context.getString(R.string.dialog_privacy_info))
                .setTitle(context.getString(R.string.dialog_privacy_info_title));
        builder.setPositiveButton(context.getString(R.string.dialog_about_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent siteIntent =
                        new Intent("android.intent.action.VIEW",
                                Uri.parse("https://rekijan.nl/privacypolicy.php"));
                context.startActivity(siteIntent);
            }
        });
        builder.setNegativeButton(context.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Shows a dialog where the user can choose to send a donation tip to the developer.
     * Integrates with Google Play Billing to fetch current prices and trigger purchase flows.
     *
     * @param context             Android context.
     * @param billingClientHelper Initialized billing helper to manage the transaction.
     */
    public void openTipDialog(final Context context, BillingClientHelper billingClientHelper) {
        billingClientHelper.queryPurchases();

        //Build a dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        //Add custom layout to dialog
        LayoutInflater inflater = LayoutInflater.from(context);
        final View alertDialogView = inflater.inflate(R.layout.dialog_tip, null);
        builder.setTitle(context.getString(R.string.tip_dialog_title));
        //Set button to close and cancel
        builder.setNegativeButton(context.getString(R.string.dialog_cancel), (dialogInterface, i) -> {});

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