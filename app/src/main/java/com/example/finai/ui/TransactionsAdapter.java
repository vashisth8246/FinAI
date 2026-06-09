package com.example.finai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finai.R;
import com.example.finai.model.TransactionModel;

import java.util.ArrayList;
import java.util.List;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.VH> {

    private final List<TransactionModel> data = new ArrayList<>();

    public void submit(List<TransactionModel> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int i) {
        TransactionModel t = data.get(i);
        h.merchant.setText(t.merchant != null && !t.merchant.isEmpty() ? t.merchant : "(message)");
        h.amount.setText("₹" + (long) t.amount);
        h.date.setText(t.dateIso);
        h.category.setText(" • " + (t.category == null ? "others" : t.category));
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView merchant, amount, date, category;
        VH(@NonNull View itemView) {
            super(itemView);
            merchant = itemView.findViewById(R.id.merchant);
            amount = itemView.findViewById(R.id.amount);
            date = itemView.findViewById(R.id.date);
            category = itemView.findViewById(R.id.category);
        }
    }
}