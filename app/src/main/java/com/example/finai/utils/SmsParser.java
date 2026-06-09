package com.example.finai.utils;

import com.example.finai.model.TransactionModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsParser {

    // Flexible patterns: amount and merchant
    private static final Pattern AMOUNT = Pattern.compile("(?:INR|Rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern MERCHANT_PREFERRED = Pattern.compile("(?:to|at)\\s+([-A-Za-z0-9 .&_]{1,40})", Pattern.CASE_INSENSITIVE);
    private static final Pattern MERCHANT_FALLBACK = Pattern.compile("from\\s+([-A-Za-z0-9 .&_]{1,40})", Pattern.CASE_INSENSITIVE);

    public static TransactionModel parse(String message, long timestamp) {
        if (message == null) return null;
        String msg = message.trim();

        double amount = 0;
        Matcher ma = AMOUNT.matcher(msg);
        if (ma.find()) {
            String amtStr = ma.group(1).replace(",", "");
            try { amount = Double.parseDouble(amtStr); } catch (Exception ignored) {}
        }

        String merchant = null;
        Matcher mp = MERCHANT_PREFERRED.matcher(msg);
        if (mp.find()) merchant = mp.group(1).trim();
        if ((merchant == null || merchant.isEmpty())) {
            Matcher mf = MERCHANT_FALLBACK.matcher(msg);
            if (mf.find()) merchant = mf.group(1).trim();
        }
        if (merchant == null || merchant.isEmpty()) merchant = "(unknown)";

        // Try to extract date from message; fallback to SMS timestamp
        String dateIso = null;
        try {
            java.util.regex.Matcher md1 = java.util.regex.Pattern.compile("(?:on\\s+)(\\d{1,2}\\s*[A-Za-z]{3})", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(msg);
            java.util.regex.Matcher md2 = java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(msg);
            if (md2.find()) {
                dateIso = md2.group(1);
            } else if (md1.find()) {
                String dmy = md1.group(1).replaceAll("\\s+", " ");
                int year = Integer.parseInt(new SimpleDateFormat("yyyy", Locale.US).format(new Date(timestamp)));
                Date parsed = new SimpleDateFormat("d MMM yyyy", Locale.US).parse(dmy + " " + year);
                if (parsed != null) dateIso = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsed);
            }
        } catch (Exception ignored) {}
        if (dateIso == null) dateIso = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(timestamp));

        String low = msg.toLowerCase(Locale.US);
        String type = (low.contains("credit") || low.contains("credited") || low.contains("received")) ? "credit" : "debit";

        // Categorization tuned for your chips
        String category = "others";
        if (low.contains("swiggy") || low.contains("zomato") || low.contains("food") || low.contains("restaurant") || low.contains("cafe") || low.contains("pizza") || low.contains("burger")) {
            category = "food";
        } else if (low.contains("uber") || low.contains("ola") || low.contains("rapido") || low.contains("metro") || low.contains("train") || low.contains("bus") || low.contains("flight") || low.contains("air") || low.contains("cab") || low.contains("ride")) {
            category = "travel";
        } else if (low.contains("bill") || low.contains("electric") || low.contains("electricity") || low.contains("gas") || low.contains("recharge") || low.contains("postpaid") || low.contains("prepaid") || low.contains("dth") || low.contains("broadband") || low.contains("wifi") || low.contains("airtel") || low.contains("jio") || low.contains("vi") || low.contains("netflix") || low.contains("spotify") || low.contains("hotstar") || low.contains("subscription") || low.contains("membership") || low.contains("prime")) {
            // Treat subscriptions and utilities as Bills
            category = "bills";
        } else if (low.contains("rent")) {
            category = "rent";
        } else if (low.contains("grocery") || low.contains("grocer") || low.contains("d-mart") || low.contains("dmart") || low.contains("reliance fresh") || low.contains("bigbasket") || low.contains("reliance smart")) {
            category = "groceries";
        } else if ((low.contains("amazon") || low.contains("flipkart") || low.contains("myntra") || low.contains("ajio"))) {
            // If clearly a subscription, keep under bills; otherwise others
            if (low.contains("prime") || low.contains("membership") || low.contains("subscription")) category = "bills";
            else category = "others";
        } else if (type.equals("credit") && (low.contains("salary") || low.contains("credited") || low.contains("refund") || low.contains("interest"))) {
            category = "income"; // No chip yet; will appear under All
        }

        // If we couldn't extract amount at all, return null so caller can skip or store as message-only
        if (amount <= 0) {
            return new TransactionModel(0, merchant, category, 0.3, dateIso, type);
        }
        return new TransactionModel(amount, merchant, category, 0.7, dateIso, type);
    }
}
