package com.penguinstech.cloud_syncer.utils;

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

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.SkuDetails;
import com.penguinstech.cloud_syncer.R;
import com.penguinstech.cloud_syncer.SubscribeActivity;

import java.util.List;

public class SubscrtiptionsAdapter extends RecyclerView.Adapter<SubscrtiptionsAdapter.ViewHolder> {

    private final List<SkuDetails> skuDetails;
    private final Context context;
    private final BillingProcessor bp;
    public SubscrtiptionsAdapter(Context context, List<SkuDetails> skuDetails, BillingProcessor bp) {
        this.skuDetails = skuDetails;
        this.context = context;
        this.bp = bp;
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
        holder.subTitle.setText(subscription.title);
        holder.subDescription.setText(subscription.description);
        holder.subPrice.setText(String.format("%s %s", subscription.currency, subscription.priceValue));
        holder.subscribeBtn.setOnClickListener(v->{

            bp.subscribe((Activity) context, subscription.productId);
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
