package com.example.finai.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.finai.model.GoalModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LocalGoalsRepository {
    private static final String PREF = "finai_local";
    private static final String KEY = "goals";

    private final SharedPreferences sp;

    public LocalGoalsRepository(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public List<GoalModel> getGoals() {
        String raw = sp.getString(KEY, "[]");
        List<GoalModel> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                GoalModel g = new GoalModel();
                g.id = o.optString("id");
                g.title = o.optString("title");
                g.targetAmount = o.optDouble("targetAmount", 0);
                g.spentSoFar = o.optDouble("spentSoFar", 0);
                g.status = o.optString("status", "active");
                g.category = o.optString("category", "others");
                g.periodDays = o.optInt("periodDays", 30);
                list.add(g);
            }
        } catch (JSONException ignored) {}
        return list;
    }

    public void addGoal(String title, double target, String category, int periodDays) {
        try {
            JSONArray arr = new JSONArray(sp.getString(KEY, "[]"));
            JSONObject o = new JSONObject();
            o.put("id", String.valueOf(System.currentTimeMillis()));
            o.put("title", title);
            o.put("targetAmount", target);
            o.put("spentSoFar", 0);
            o.put("status", "active");
            o.put("category", category == null || category.isEmpty() ? "others" : category.toLowerCase());
            o.put("periodDays", periodDays <= 0 ? 30 : periodDays);
            arr.put(o);
            sp.edit().putString(KEY, arr.toString()).apply();
        } catch (JSONException ignored) {}
    }

    public void removeGoal(String id) {
        try {
            JSONArray arr = new JSONArray(sp.getString(KEY, "[]"));
            JSONArray out = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (!id.equals(o.optString("id"))) out.put(o);
            }
            sp.edit().putString(KEY, out.toString()).apply();
        } catch (JSONException ignored) {}
    }

    public void updateGoal(String id, String title, double target, String category, int periodDays) {
        try {
            JSONArray arr = new JSONArray(sp.getString(KEY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (id.equals(o.optString("id"))) {
                    o.put("title", title);
                    o.put("targetAmount", target);
                    o.put("category", category == null || category.isEmpty() ? o.optString("category", "others") : category.toLowerCase());
                    if (periodDays > 0) o.put("periodDays", periodDays);
                }
            }
            sp.edit().putString(KEY, arr.toString()).apply();
        } catch (JSONException ignored) {}
    }
}
