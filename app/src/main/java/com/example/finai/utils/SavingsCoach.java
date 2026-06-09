package com.example.finai.utils;

import com.example.finai.model.TransactionModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SavingsCoach {

    // Analyze last 30 days of debit transactions and return actionable tips
    public static List<String> analyze(List<TransactionModel> all) {
        List<String> tips = new ArrayList<>();
        if (all == null || all.isEmpty()) return tips;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            long today = sdf.parse(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date())).getTime();
            long dayMs = 24L*60*60*1000;
            long window = 30L; // days

            double total = 0;
            Map<String, Double> byCat = new HashMap<>();
            Map<String, Double> byMerchant = new HashMap<>();
            Map<String, Integer> countByMerchant = new HashMap<>();

            for (TransactionModel t : all) {
                if (t == null || t.dateIso == null) continue;
                if (t.type != null && t.type.equalsIgnoreCase("credit")) continue; // focus on spend
                Date dt = sdf.parse(t.dateIso);
                if (dt == null) continue;
                long diff = (today - dt.getTime())/dayMs;
                if (diff < 0 || diff >= window) continue;
                double amt = Math.max(0, t.amount);
                total += amt;
                String cat = norm(t.category);
                byCat.put(cat, byCat.getOrDefault(cat, 0.0) + amt);
                String merch = norm(t.merchant);
                byMerchant.put(merch, byMerchant.getOrDefault(merch, 0.0) + amt);
                countByMerchant.put(merch, countByMerchant.getOrDefault(merch, 0) + 1);
            }

            if (total <= 0) return tips;

            // Tip 1: Top category cap
            String topCat = null; double topCatAmt = 0;
            for (Map.Entry<String, Double> e : byCat.entrySet()) {
                if (e.getValue() > topCatAmt) { topCatAmt = e.getValue(); topCat = e.getKey(); }
            }
            if (topCat != null && topCatAmt > 0) {
                long pct = Math.round((topCatAmt / total) * 100.0);
                double cap = Math.round(topCatAmt * 0.9);
                tips.add(capitalize(topCat) + " is your largest spend (" + pct + "%). Try capping it to ₹" + (long)cap + " next month.");
            }

            // Tip 2: Subscriptions/utilities
            String[] subs = new String[]{"netflix","spotify","prime","hotstar","yt premium","youtube","zee","sonyliv"};
            for (String s : subs) {
                for (Map.Entry<String, Double> e : byMerchant.entrySet()) {
                    if (e.getKey().contains(s) && e.getValue() > 0) {
                        tips.add("Review your " + pretty(s) + " plan — consider pausing/downgrading.");
                        s = null; break;
                    }
                }
                if (s == null) break;
            }

            // Tip 3: Food delivery frequency
            double foodTotal = 0; int foodOrders = 0;
            String[] food = new String[]{"swiggy","zomato","domino","pizza","kfc","mcd"};
            for (String f : food) {
                for (Map.Entry<String, Double> e : byMerchant.entrySet()) {
                    if (e.getKey().contains(f)) {
                        foodTotal += e.getValue();
                        foodOrders += countByMerchant.getOrDefault(e.getKey(), 0);
                    }
                }
            }
            if (foodOrders >= 2) {
                tips.add("You ordered food " + foodOrders + " times (₹" + (long)foodTotal + "). Set a weekly limit or switch 1–2 orders to home cooking.");
            }

            // Tip 4: Daily budget suggestion
            double dailyAvg = total / window;
            long target = Math.max(50, (long)Math.round(dailyAvg * 0.9));
            tips.add("Aim for a daily budget of ₹" + target + " (10% below your current average).");

            // Limit to top 3–4 tips for brevity
            if (tips.size() > 4) return tips.subList(0, 4);
        } catch (Exception ignored) {}
        return tips;
    }

    private static String norm(String s) { return s == null ? "" : s.trim().toLowerCase(Locale.US); }
    private static String capitalize(String s) { if (s == null || s.isEmpty()) return s; return Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    private static String pretty(String s) { if (s == null) return ""; s = s.replace("yt ", "YouTube "); return capitalize(s); }
}
