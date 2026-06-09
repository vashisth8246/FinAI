package com.example.finai.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.finai.model.TransactionModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LocalTransactionsRepository {
    private static final String PREF = "finai_local";
    private static final String KEY = "transactions";

    private final SharedPreferences sp;

    public LocalTransactionsRepository(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void add(String message, TransactionModel t) {
        try {
            JSONArray arr = new JSONArray(sp.getString(KEY, "[]"));
            JSONObject o = new JSONObject();
            o.put("message", message);
            o.put("amount", t != null ? t.amount : 0);
            o.put("merchant", t != null ? t.merchant : "");
            o.put("category", t != null ? t.category : "others");
            o.put("confidence", t != null ? t.confidence : 0);
            o.put("dateIso", t != null ? t.dateIso : "");
            o.put("type", t != null ? t.type : "");
            o.put("aiSource", "sms");
            o.put("messageOnly", t == null || t.amount <= 0);
            // Append to preserve all entries (put(index, ...) replaces). If you want newest-first, rebuild array with new first.
            arr.put(o);
            sp.edit().putString(KEY, arr.toString()).apply();
        } catch (JSONException ignored) {}
    }

    public List<TransactionModel> getAll() {
        List<TransactionModel> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp.getString(KEY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                TransactionModel t = new TransactionModel();
                t.amount = o.optDouble("amount", 0);
                t.merchant = o.optString("merchant", "");
                t.category = o.optString("category", "others");
                t.confidence = o.optDouble("confidence", 0);
                t.dateIso = o.optString("dateIso", "");
                t.type = o.optString("type", "");
                t.rawMessage = o.optString("message", "");
                t.messageOnly = o.optBoolean("messageOnly", t.amount <= 0);
                // Reuse merchant to show message if merchant empty
                if (t.merchant == null || t.merchant.isEmpty()) t.merchant = t.rawMessage;
                list.add(t);
            }
        } catch (JSONException ignored) {}
        return list;
    }

    public void clear() {
        sp.edit().remove(KEY).apply();
    }
}
