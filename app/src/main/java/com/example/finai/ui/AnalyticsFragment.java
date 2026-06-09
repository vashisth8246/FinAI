package com.example.finai.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finai.databinding.FragmentAnalyticsBinding;
import com.example.finai.data.LocalTransactionsRepository;
import com.example.finai.model.TransactionModel;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsFragment extends Fragment {

    private FragmentAnalyticsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAnalyticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshAnalytics();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAnalytics();
    }

    private void refreshAnalytics() {
        // Build daily totals (7d)
        List<BarEntry> entries = new ArrayList<>();
        float[] daily = last7Totals(new LocalTransactionsRepository(requireContext()).getAll());
        for (int i = 0; i < daily.length; i++) entries.add(new BarEntry(i, daily[i]));
        BarDataSet set = new BarDataSet(entries, "Daily Spend (7d)");
        // Make values visible and readable
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(12f);
        BarData data = new BarData(set);
        binding.barChart.setData(data);
        Description d = new Description(); d.setText("");
        binding.barChart.setDescription(d);

        // Make axes and legend readable on dark background
        binding.barChart.getAxisLeft().setTextColor(Color.WHITE);
        binding.barChart.getAxisLeft().setTextSize(12f);
        binding.barChart.getAxisLeft().setAxisLineColor(Color.LTGRAY);
        binding.barChart.getAxisLeft().setGridColor(Color.DKGRAY);

        binding.barChart.getAxisRight().setTextColor(Color.WHITE);
        binding.barChart.getAxisRight().setTextSize(12f);
        binding.barChart.getAxisRight().setAxisLineColor(Color.LTGRAY);
        binding.barChart.getAxisRight().setGridColor(Color.DKGRAY);

        com.github.mikephil.charting.components.XAxis xAxis = binding.barChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setTextSize(12f);
        xAxis.setAxisLineColor(Color.LTGRAY);
        xAxis.setGridColor(Color.DKGRAY);

        com.github.mikephil.charting.components.Legend legend = binding.barChart.getLegend();
        legend.setTextColor(Color.WHITE);
        legend.setTextSize(12f);

        binding.barChart.invalidate();

        // Insights from last 30 days
        String insights = insights30(new LocalTransactionsRepository(requireContext()).getAll());
        binding.analyticsInsights.setText(insights);
    }

    private float[] last7Totals(List<TransactionModel> list) {
        float[] totals = new float[7];
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            long today = sdf.parse(sdf.format(new Date())).getTime();
            long dayMs = 24L*60*60*1000;
            if (list != null) for (TransactionModel t : list) {
                if (t == null || t.dateIso == null) continue;
                Date dt = sdf.parse(t.dateIso); if (dt == null) continue;
                long diff = (today - dt.getTime())/dayMs;
                if (diff >=0 && diff < 7) {
                    if (t.type == null || !t.type.equalsIgnoreCase("credit")) totals[(int)(6-diff)] += (float)Math.max(0, t.amount);
                }
            }
        } catch (Exception ignored) {}
        return totals;
    }

    private String insights30(List<TransactionModel> list) {
        // Sum last 30 days by category and by week
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            long today = sdf.parse(sdf.format(new Date())).getTime();
            long dayMs = 24L*60*60*1000;
            Map<String, Float> cat = new HashMap<>();
            float week1=0, week2=0, week3=0, week4=0;
            if (list != null) for (TransactionModel t : list) {
                if (t == null || t.dateIso == null) continue;
                Date dt = sdf.parse(t.dateIso); if (dt == null) continue;
                long diff = (today - dt.getTime())/dayMs;
                if (diff >=0 && diff < 30) {
                    float amt = (t.type == null || !t.type.equalsIgnoreCase("credit")) ? (float)Math.max(0, t.amount) : 0f;
                    String c = (t.category == null || t.category.isEmpty()) ? "others" : t.category.toLowerCase(Locale.US);
                    cat.put(c, cat.getOrDefault(c, 0f) + amt);
                    if (diff < 7) week4 += amt; else if (diff < 14) week3 += amt; else if (diff < 21) week2 += amt; else week1 += amt;
                }
            }
            // Top category
            String topCat = "-"; float topVal = 0f; float total = 0f;
            for (Map.Entry<String, Float> e : cat.entrySet()) { total += e.getValue(); if (e.getValue() > topVal) { topVal = e.getValue(); topCat = e.getKey(); } }
            String trend = String.format(Locale.US, "Weekly totals: W-3 ₹%d, W-2 ₹%d, W-1 ₹%d, W ₹%d", (long)week1,(long)week2,(long)week3,(long)week4);
            String top = topCat.equals("-") ? "No spend yet" : String.format(Locale.US, "Top category: %s (₹%d)", topCat, (long)topVal);
            return top + "\n" + trend + "\nTotal (30d): ₹" + (long)total;
        } catch (Exception ignored) {}
        return "No data yet";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}