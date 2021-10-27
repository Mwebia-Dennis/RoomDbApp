package com.penguinstech.cloudy.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.SkuDetails;
import com.penguinstech.cloudy.R;
import com.penguinstech.cloudy.SubscribeActivity;
import com.penguinstech.cloudy.room_db.AppDatabase;

import java.util.List;

public class SubscrtiptionsAdapter extends RecyclerView.Adapter<SubscrtiptionsAdapter.ViewHolder> {

    private final List<SkuDetails> skuDetails;
    private final Context context;
    private final BillingClient billingClient;
    private String subscriptionStoreId = "";
    private String purchaseToken = "";
    public SubscrtiptionsAdapter(Context context, List<SkuDetails> skuDetails, BillingClient billingClient) {
        this.skuDetails = skuDetails;
        this.context = context;
        this.billingClient = billingClient;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.subscription_card_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        SkuDetails subscription = skuDetails.get(position);
        String sku = subscription.getSku();
        if (!subscriptionStoreId.equals(subscription.getSku())) {

            holder.subTitle.setText(subscription.getTitle());
            holder.subDescription.setText(subscription.getDescription());
            holder.subPrice.setText(String.format("%s %s", subscription.getPriceCurrencyCode(), subscription.getPrice()));

            //if purchase token is empty, then subsciption is new
            //else subscription is an upgrade or downgrade
            if (purchaseToken.equals("")) {

                holder.subscribeBtn.setText("Subscribe");
                holder.subscribeBtn.setOnClickListener(v->{
                    // An activity reference from which the billing flow will be launched.
                    // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(subscription)
                            .build();
                    int responseCode = billingClient.launchBillingFlow((Activity) context, billingFlowParams).getResponseCode();
                    if(responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d("launching billing", "successful");
                    }

                });
            }else {

                holder.subscribeBtn.setText("Upgrade");
                holder.subscribeBtn.setOnClickListener(v->{
                    // An activity reference from which the billing flow will be launched.
                    // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                            .setSubscriptionUpdateParams(BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                                    .setOldSkuPurchaseToken(purchaseToken)
                                    .setReplaceSkusProrationMode(BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION)
                                    .build())
                            .setSkuDetails(subscription)
                            .build();
                    int responseCode = billingClient.launchBillingFlow((Activity)context, billingFlowParams).getResponseCode();
                    if(responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d("launching billing", "successful");
                    }

                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return skuDetails.size();
    }

    public void setPurchaseToken(String purchaseToken) {

        this.purchaseToken = purchaseToken;
    }

    public void setSubscriptionStoreId(String subscriptionStoreId) {
        this.subscriptionStoreId = subscriptionStoreId;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView subTitle, subDescription, subPrice;
        Button subscribeBtn;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            subTitle = itemView.findViewById(R.id.subTitle);
            subDescription = itemView.findViewById(R.id.subDescription);
            subPrice = itemView.findViewById(R.id.subPrice);
            subscribeBtn = itemView.findViewById(R.id.subscribeBtn);
        }
    }
}
