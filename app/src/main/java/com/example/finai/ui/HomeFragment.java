package com.example.finai.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finai.databinding.FragmentHomeBinding;
import com.example.finai.R;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.finai.data.LocalTransactionsRepository;
import com.example.finai.model.TransactionModel;
import com.example.finai.utils.SavingsCoach;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private TransactionsAdapter txAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // TODO hook Firestore totals and Gemini insights
        binding.topSummary.setText("This week: ₹12,400");

        // Quick actions
        binding.btnAddGoal.setOnClickListener(v -> {
            com.google.android.material.bottomnavigation.BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_nav);
            nav.setSelectedItemId(R.id.menu_goals);
        });
        binding.btnViewAnalytics.setOnClickListener(v -> {
            com.google.android.material.bottomnavigation.BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_nav);
            nav.setSelectedItemId(R.id.menu_analytics);
        });

        // Recent transactions list
        txAdapter = new TransactionsAdapter();
        binding.transactionsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.transactionsList.setAdapter(txAdapter);

        refreshTransactions();

        // Style and populate line chart from real transactions (last 7 days)
        updateChartFromTransactions();

        Description d = new Description(); d.setText("");
        binding.lineChart.setDescription(d);
        binding.lineChart.getAxisRight().setEnabled(false);
        XAxis x = binding.lineChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(getResources().getColor(R.color.md_onSurfaceVariant));
        binding.lineChart.getAxisLeft().setTextColor(getResources().getColor(R.color.md_onSurfaceVariant));
        Legend l = binding.lineChart.getLegend();
        l.setTextColor(getResources().getColor(R.color.md_onSurfaceVariant));
        // Reduce top spacing above chart
        l.setEnabled(false);
        binding.lineChart.setViewPortOffsets(32f, 2f, 16f, 32f);
        binding.lineChart.invalidate();

        // Category chip filters (demo behavior)
        setupCategoryChip(binding.chipGroceries, "Groceries");
        setupCategoryChip(binding.chipTravel, "Travel");
        setupCategoryChip(binding.chipBills, "Bills");
        setupCategoryChip(binding.chipFood, "Food");
        setupCategoryChip(binding.chipRent, "Rent");
        setupCategoryChip(binding.chipOthers, "Others");

        // Expand/collapse guidelines
        binding.btnGuidelines.setOnClickListener(v -> {
            View panel = binding.guidelinesPanel;
            int vis = panel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            panel.setVisibility(vis);
        });
    }

    private void setupCategoryChip(Chip chip, String category) {
        chip.setOnClickListener(v -> updateChartFromTransactions());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshTransactions();
    }

    private void refreshTransactions() {
        LocalTransactionsRepository repo = new LocalTransactionsRepository(requireContext());
        List<TransactionModel> list = repo.getAll();
        txAdapter.submit(list);
        binding.emptyTransactions.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        updateChartFromTransactions();
    }

    private void updateChartFromTransactions() {
        LocalTransactionsRepository repo = new LocalTransactionsRepository(requireContext());
        List<TransactionModel> list = repo.getAll();
        String cat = selectedCategoryForHome();
        float[] daily = buildLast7DaysTotals(list, cat);
        List<Entry> entries = new ArrayList<>();
        float weekSum = 0f;
        for (int i = 0; i < daily.length; i++) { entries.add(new Entry(i, daily[i])); weekSum += daily[i]; }
        LineDataSet set = new LineDataSet(entries, cat == null ? "Daily Spend (7d)" : (capitalize(cat) + " spend (7d)"));
        set.setColor(getResources().getColor(R.color.neon_blue));
        set.setCircleColor(getResources().getColor(R.color.neon_blue));
        set.setLineWidth(2.5f);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(false); // keep previous clean look
        binding.lineChart.setData(new LineData(set));

        // Keep original minimalist chart; avoid overlapping description
        Description d = new Description(); d.setText("");
        binding.lineChart.setDescription(d);
        // Move legend (dataset label) to top to avoid overlapping the X axis
        Legend legend = binding.lineChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setTextSize(10f);
        legend.setYOffset(-6f); // pull legend closer to chart to reduce empty space
        binding.lineChart.invalidate();
        binding.topSummary.setText("This week: ₹" + (long)weekSum);
        binding.subSummary.setText("Live from your SMS transactions");
        String insight = buildWeekOverWeekInsight(list, cat);
        java.util.List<String> tips = SavingsCoach.analyze(list);
        StringBuilder sb = new StringBuilder();
        if (insight != null && !insight.isEmpty()) sb.append(insight);
        if (tips != null && !tips.isEmpty()) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("Savings ideas:\n");
            for (int i=0;i<tips.size();i++) { sb.append("• ").append(tips.get(i)); if (i<tips.size()-1) sb.append('\n'); }
        }
        binding.insightsText.setText(sb.toString());
    }


    private float[] buildLast7DaysTotals(List<TransactionModel> list, String categoryFilter) {
        float[] totals = new float[7];
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            long today = sdf.parse(new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date())).getTime();
            long dayMs = 24L*60*60*1000;
            if (list != null) {
                for (TransactionModel t : list) {
                    if (t == null || t.dateIso == null) continue;
                    if (categoryFilter != null && t.category != null && !t.category.equalsIgnoreCase(categoryFilter)) continue;
                    java.util.Date dt = sdf.parse(t.dateIso);
                    if (dt == null) continue;
                    long diff = (today - dt.getTime())/dayMs;
                    if (diff >=0 && diff < 7) {
                        if (t.type == null || !t.type.equalsIgnoreCase("credit")) {
                            totals[(int)(6 - diff)] += (float)Math.max(0, t.amount);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return totals;
    }

    private String buildWeekOverWeekInsight(List<TransactionModel> list, String categoryFilter) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            long today = sdf.parse(new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date())).getTime();
            long dayMs = 24L*60*60*1000;
            double last7 = 0, prev7 = 0;
            if (list != null) {
                for (TransactionModel t : list) {
                    if (t == null || t.dateIso == null) continue;
                    if (t.type != null && t.type.equalsIgnoreCase("credit")) continue; // spend only
                    if (categoryFilter != null && t.category != null && !t.category.equalsIgnoreCase(categoryFilter)) continue;
                    java.util.Date dt = sdf.parse(t.dateIso);
                    if (dt == null) continue;
                    long diff = (today - dt.getTime())/dayMs;
                    if (diff >= 0 && diff < 7) last7 += Math.max(0, t.amount);
                    else if (diff >= 7 && diff < 14) prev7 += Math.max(0, t.amount);
                }
            }
            if (prev7 <= 0 && last7 <= 0) return "No spend data yet";
            if (prev7 <= 0) return (categoryFilter == null ? "Spending started this week" : (capitalize(categoryFilter) + ": spending started this week"));
            double change = ((last7 - prev7) / prev7) * 100.0;
            String dir = change >= 0 ? "more" : "less";
            long pct = Math.round(Math.abs(change));
            String label = categoryFilter == null ? "" : (capitalize(categoryFilter) + ": ");
            return label + "You spent " + pct + "% " + dir + " than last week — ₹" + (long)last7 + " vs ₹" + (long)prev7;
        } catch (Exception ignored) {}
        return "Spending insight unavailable";
    }

    private String selectedCategoryForHome() {
        if (binding.chipGroceries.isChecked()) return "groceries";
        if (binding.chipTravel.isChecked()) return "travel";
        if (binding.chipBills.isChecked()) return "bills";
        if (binding.chipFood.isChecked()) return "food";
        if (binding.chipRent.isChecked()) return "rent";
        if (binding.chipOthers.isChecked()) return "others";
        return null; // overall
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
