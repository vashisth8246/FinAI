package com.example.finai.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class CloudSync {
    private static final String KEY = "cloud_sync_enabled";
    public static boolean isEnabled(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getBoolean(KEY, false);
    }
    public static void setEnabled(Context ctx, boolean enabled) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        sp.edit().putBoolean(KEY, enabled).apply();
    }
}