package com.example.finai.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.finai.data.LocalTransactionsRepository;
import com.example.finai.databinding.FragmentTransactionsBinding;
import com.example.finai.model.TransactionModel;
import com.example.finai.utils.SmsParser;
import com.example.finai.util.CsvUtil;
import com.google.android.material.chip.Chip;

import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionsFragment extends Fragment {

    private static final int REQ_EXPORT_CSV = 601;
    private static final int REQ_IMPORT_CSV = 602;

    private FragmentTransactionsBinding binding;
    private TransactionsAdapter adapter;
    private List<TransactionModel> all = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            adapter = new TransactionsAdapter();
            binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.list.setAdapter(adapter);

            if (binding.search != null) {
                binding.search.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
                    @Override public void afterTextChanged(Editable s) {}
                });
            }

            setupChip(binding.chipAll);
            setupChip(binding.chipGroceries);
            setupChip(binding.chipTravel);
            setupChip(binding.chipBills);
            setupChip(binding.chipFood);
            setupChip(binding.chipRent);
            setupChip(binding.chipOthers);
            // Type chips
            setupChip(binding.chipTypeAll);
            setupChip(binding.chipDebits);
            setupChip(binding.chipCredits);
            binding.btnScanSms.setOnClickListener(v -> scanInbox());
            binding.btnSeedSample.setOnClickListener(v -> seedSample());

            // Hide "Add Sample" if we already have transactions
            try {
                LocalTransactionsRepository repo = new LocalTransactionsRepository(requireContext());
                List<TransactionModel> existing = repo.getAll();
                boolean hasData = existing != null && !existing.isEmpty();
                binding.btnSeedSample.setVisibility(hasData ? View.GONE : View.VISIBLE);
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            android.widget.Toast.makeText(requireContext(), "Transactions screen error", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void seedSample() {
        LocalTransactionsRepository repo = new LocalTransactionsRepository(requireContext());
        // Clear existing local transactions so you get a clean 7-day dataset
        repo.clear();
        String[][] samples = new String[][]{
                {"Swiggy","1200","food"},
                {"Uber","450","travel"},
                {"Reliance Fresh","2300","groceries"},
                {"Airtel Recharge","799","bills"},
                {"Zomato","650","food"},
                {"Metro","320","travel"},
                {"Amazon","2999","shopping"},
                {"Netflix","499","entertainment"},
                {"HP Petrol","1100","fuel"},
                {"Apollo Pharmacy","850","health"},
                {"D-Mart","1560","groceries"},
                {"Rapido","180","travel"},
                {"BESCOM","1340","bills"},
                {"Myntra","2149","shopping"},
                {"CCD","220","food"},
                {"Decathlon","1899","others"}
        };
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        java.util.Random rnd = new java.util.Random();
        int count = 0;
        for (int i = 0; i < 7; i++) {
            Date d = new Date(System.currentTimeMillis() - i*24L*60*60*1000);
            int perDay = 1 + rnd.nextInt(2); // 1 or 2 per day
            for (int k = 0; k < perDay; k++) {
                int idx = (i*2 + k) % samples.length;
                TransactionModel t = new TransactionModel();
                try { t.amount = Double.parseDouble(samples[idx][1]); } catch (Exception ignored) {}
                t.merchant = samples[idx][0];
                t.category = samples[idx][2];
                t.type = "debit";
                t.dateIso = sdf.format(d);
                repo.add(t.merchant, t);
                count++;
            }
        }
        android.widget.Toast.makeText(requireContext(), "Seeded " + count + " sample transactions", android.widget.Toast.LENGTH_SHORT).show();
        // Hide the seed button now that data exists
        if (binding != null) binding.btnSeedSample.setVisibility(View.GONE);
        onResume();
    }

    private void exportCsv() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/csv");
        i.putExtra(Intent.EXTRA_TITLE, "finai_export_" + System.currentTimeMillis() + ".csv");
        startActivityForResult(i, REQ_EXPORT_CSV);
    }

    private void importCsv() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/*");
        startActivityForResult(i, REQ_IMPORT_CSV);
    }

    private void scanInbox() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_SMS}, 401);
            return;
        }
        int imported = 0;
        try {
            Uri uri = Telephony.Sms.Inbox.CONTENT_URI; // content://sms/inbox
            String[] proj = new String[]{Telephony.Sms.BODY, Telephony.Sms.DATE};
            Cursor c = requireContext().getContentResolver().query(uri, proj, null, null, Telephony.Sms.DEFAULT_SORT_ORDER + " LIMIT 200");
            if (c != null) {
                LocalTransactionsRepository repo = new LocalTransactionsRepository(requireContext());
                while (c.moveToNext()) {
                    String body = c.getString(0);
                    long when = c.getLong(1);
                    if (TextUtils.isEmpty(body)) continue;
                    TransactionModel t = SmsParser.parse(body, when);
                    if (t != null) {
                        repo.add(body, t);
                        imported++;
                    }
                }
                c.close();
            }
        } catch (Exception ignored) {}
        android.widget.Toast.makeText(requireContext(), imported + " imported", android.widget.Toast.LENGTH_SHORT).show();
        onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 401 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanInbox();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode != android.app.Activity.RESULT_OK || data == null) return;
            Uri uri = data.getData(); if (uri == null) return;
            if (requestCode == REQ_EXPORT_CSV) {
                String csv = CsvUtil.buildCsv(requireContext());
                java.io.OutputStream os = requireContext().getContentResolver().openOutputStream(uri, "w");
                if (os != null) { os.write(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8)); os.close(); }
                android.widget.Toast.makeText(requireContext(), "Exported CSV", android.widget.Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQ_IMPORT_CSV) {
                java.io.InputStream is = requireContext().getContentResolver().openInputStream(uri);
                if (is != null) {
                    String csv = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    is.close();
                    CsvUtil.importCsv(requireContext(), csv);
                    android.widget.Toast.makeText(requireContext(), "Imported CSV", android.widget.Toast.LENGTH_SHORT).show();
                    onResume();
                }
            }
        } catch (Exception ignored) {}
    }

    private void setupChip(Chip chip) {
        chip.setOnClickListener(v -> applyFilters());
    }

    private void applyFilters() {
        String q = binding.search.getText() != null ? binding.search.getText().toString().toLowerCase() : "";
        String category = selectedCategory();
        String type = selectedType();
        List<TransactionModel> out = new ArrayList<>();
        for (TransactionModel t : all) {
            boolean matchesQ = q.isEmpty() || (String.valueOf(t.amount).contains(q)) ||
                    (t.merchant != null && t.merchant.toLowerCase().contains(q)) ||
                    (t.category != null && t.category.toLowerCase().contains(q)) ||
                    (t.rawMessage != null && t.rawMessage.toLowerCase().contains(q));
            boolean matchesCat = category.equals("all") || (t.category != null && t.category.equalsIgnoreCase(category));
            boolean matchesType;
            if ("credit".equals(type)) {
                matchesType = t.type != null && t.type.equalsIgnoreCase("credit");
            } else if ("debit".equals(type)) {
                // Treat empty type as debit (from manual entries)
                matchesType = (t.type == null) || !t.type.equalsIgnoreCase("credit");
            } else {
                matchesType = true;
            }
            if (matchesQ && matchesCat && matchesType) out.add(t);
        }
        adapter.submit(out);
        binding.empty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String selectedCategory() {
        if (binding.chipGroceries.isChecked()) return "groceries";
        if (binding.chipTravel.isChecked()) return "travel";
        if (binding.chipBills.isChecked()) return "bills";
        if (binding.chipFood.isChecked()) return "food";
        if (binding.chipRent.isChecked()) return "rent";
        if (binding.chipOthers.isChecked()) return "others";
        return "all";
    }

    private String selectedType() {
        if (binding.chipDebits.isChecked()) return "debit";
        if (binding.chipCredits.isChecked()) return "credit";
        return "all";
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalTransactionsRepository repo = new LocalTransactionsRepository(requireContext());
        all = repo.getAll();
        applyFilters();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}