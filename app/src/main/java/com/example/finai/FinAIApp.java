package com.example.finai;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class FinAIApp extends Application {
    private static FinAIApp INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
        } catch (Throwable ignored) {
            // Allow app to run without google-services.json for now.
        }
    }

    public static FinAIApp get() { return INSTANCE; }
}
