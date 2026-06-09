package com.example.finai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.example.finai.databinding.ActivityMainBinding;
import com.example.finai.ui.AnalyticsFragment;
import com.example.finai.ui.GoalsFragment;
import com.example.finai.ui.HomeFragment;
import com.example.finai.ui.TransactionsFragment;
import com.example.finai.ui.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Default fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, new HomeFragment())
                .commit();
        if (binding.toolbar != null) binding.toolbar.setTitle("Home");

        // Toolbar menu
        if (binding.toolbar != null) {
            binding.toolbar.inflateMenu(R.menu.menu_main);
            binding.toolbar.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.action_settings) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.nav_host_fragment, new    SettingsFragment())
                            .commit();
                    if (binding.toolbar != null) binding.toolbar.setTitle("Settings");
                    return true;
                }
                return false;
            });
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, new HomeFragment())
                        .commit();
                if (binding.toolbar != null) binding.toolbar.setTitle("Home");
                return true;
            } else if (id == R.id.menu_transactions) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, new TransactionsFragment())
                        .commit();
                if (binding.toolbar != null) binding.toolbar.setTitle("Transactions");
                return true;
            } else if (id == R.id.menu_analytics) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, new AnalyticsFragment())
                        .commit();
                if (binding.toolbar != null) binding.toolbar.setTitle("Analytics");
                return true;
            } else if (id == R.id.menu_goals) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, new GoalsFragment())
                        .commit();
                if (binding.toolbar != null) binding.toolbar.setTitle("Goals");
                return true;
            }
            return false;
        });

        requestRuntimePermissions();
        maybeShowOnboarding();
    }

    private void maybeShowOnboarding() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean seen = sp.getBoolean("onboarding_shown", false);
        if (!seen) {
            new AlertDialog.Builder(this)
                    .setTitle("Welcome to FinAI")
                    .setMessage("• Home shows your summary and trends\n• Analytics visualizes spends\n• Goals lets you track saving targets (works offline)\n\nTip: Tap + to add your first goal.")
                    .setPositiveButton("Got it", (d,w) -> sp.edit().putBoolean("onboarding_shown", true).apply())
                    .show();
        }
    }

    private void requestRuntimePermissions() {
        String[] perms = new String[]{
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.RECORD_AUDIO
        };
        if (Build.VERSION.SDK_INT >= 33) {
            // POST_NOTIFICATIONS is runtime on Android 13+
            perms = new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        }
        boolean need = false;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                need = true; break;
            }
        }
        if (need) ActivityCompat.requestPermissions(this, perms, 101);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
