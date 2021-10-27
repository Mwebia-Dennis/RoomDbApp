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

import java.util.List;

public class SubscrtiptionsAdapter extends RecyclerView.Adapter<SubscrtiptionsAdapter.ViewHolder> {

    private final List<SkuDetails> skuDetails;
    private final Context context;
    private final BillingClient billingClient;
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
        holder.subTitle.setText(subscription.getTitle());
        holder.subDescription.setText(subscription.getDescription());
        holder.subPrice.setText(String.format("%s %s", subscription.getPriceCurrencyCode(), subscription.getPrice()));
        holder.subscribeBtn.setOnClickListener(v->{

//            bp.subscribe((Activity) context, subscription.productId);
            // An activity reference from which the billing flow will be launched.
            // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(subscription)
                    .build();
            int responseCode = billingClient.launchBillingFlow((Activity) context, billingFlowParams).getResponseCode();
            if(responseCode == BillingClient.BillingResponseCode.OK) {

                Util.setSubscriptionId(context, subscription.getSku());
                Log.d("launching billing", "successful");
            }

        });
    }

    @Override
    public int getItemCount() {
        return skuDetails.size();
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
