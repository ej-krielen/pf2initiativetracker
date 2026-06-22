package com.rekijan.initiativetrackersecondedition.features.billing;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for managing Google Play Billing integrations (v9.1.0).
 * Handles product querying, purchase flows, and consumption for in-app donations.
 */
public class BillingClientHelper implements PurchasesUpdatedListener {

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Constants    ////////////////////////////////////////////////////////////////////////////////////////////////    private static final String LOG_TAG = "BillingClientHelper";
    public static final String ITEM_SKU_FIVE = "fiveeurodonation";
    public static final String ITEM_SKU_TEN = "teneurodonation";

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Members    ////////////////////////////////////////////////////////////////////////////////////////////////    private final Activity activity;    private BillingClient billingClient;
    private ProductDetails fiveProductDetails;
    private ProductDetails tenProductDetails;
    private final BillingHelperListener listener;

    public interface BillingHelperListener {
        void onProductDetailsLoaded();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Constructor    ////////////////////////////////////////////////////////////////////////////////////////////////    public BillingClientHelper(Activity activity, BillingHelperListener listener) {        this.activity = activity;
        this.listener = listener;
        setUpBillingClient();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Methods    ////////////////////////////////////////////////////////////////////////////////////////////////    /**     * Starts the Google Play purchase flow for a specific product.
     *
     * @param productId The SKU of the item to purchase.
     */
    public void launchBillingFlow(String productId) {
        ProductDetails productDetails = productId.equals(ITEM_SKU_FIVE) ? fiveProductDetails : tenProductDetails;
        if (billingClient != null && billingClient.isReady() && productDetails != null) {
            List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
            productDetailsParamsList.add(BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build());

            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build();
            billingClient.launchBillingFlow(activity, flowParams);
        }
    }

    /**
     * Synchronizes local state with Google Play to handle any pending or unconsumed purchases.
     */
    public void queryPurchases() {
        if (billingClient != null && billingClient.isReady()) {
            QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build();
            billingClient.queryPurchasesAsync(params, (billingResult, list) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (Purchase purchase : list) {
                        if (purchase.getProducts().contains(ITEM_SKU_FIVE) || purchase.getProducts().contains(ITEM_SKU_TEN)) {
                            consumePurchase(purchase.getPurchaseToken());
                        }
                    }
                }
            });
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Override methods    ////////////////////////////////////////////////////////////////////////////////////////////////    @Override    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Public helpers    ////////////////////////////////////////////////////////////////////////////////////////////////    public ProductDetails getFiveProductDetails() {        return fiveProductDetails;
    }

    public ProductDetails getTenProductDetails() {
        return tenProductDetails;
    }

    public void onDestroy() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////    /// Private helpers    ////////////////////////////////////////////////////////////////////////////////////////////////    private void setUpBillingClient() {        billingClient = BillingClient.newBuilder(activity)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build())
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails();
                    queryPurchases();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.d(LOG_TAG, "Billing service disconnected");
            }
        });
    }

    private void queryProductDetails() {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(ITEM_SKU_FIVE)
                .setProductType(BillingClient.ProductType.INAPP)
                .build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(ITEM_SKU_TEN)
                .setProductType(BillingClient.ProductType.INAPP)
                .build());

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsResult) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                List<ProductDetails> productDetailsList = productDetailsResult.getProductDetailsList();
                for (ProductDetails productDetails : productDetailsList) {
                    if (ITEM_SKU_FIVE.equals(productDetails.getProductId())) {
                        fiveProductDetails = productDetails;
                    }
                    else if (ITEM_SKU_TEN.equals(productDetails.getProductId())) {
                        tenProductDetails = productDetails;
                    }
                }
                if (listener != null) {
                    listener.onProductDetailsLoaded();
                }
            }
        });
    }

    private void handlePurchase(Purchase purchase) {
        if ((purchase.getProducts().contains(ITEM_SKU_FIVE) || purchase.getProducts().contains(ITEM_SKU_TEN))
                && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        activity.runOnUiThread(() -> Toast.makeText(activity, "Thank you for your tip", Toast.LENGTH_LONG).show());
                    }
                });
            }
        }
    }

    private void consumePurchase(String purchaseToken) {
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build();
        billingClient.consumeAsync(consumeParams, (billingResult, token) -> Log.d(LOG_TAG, "Consumed purchase"));
    }
}