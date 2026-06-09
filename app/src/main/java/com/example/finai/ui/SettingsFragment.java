package com.example.finai.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finai.databinding.FragmentSettingsBinding;
import com.example.finai.util.CloudSync;
import com.example.finai.util.CsvUtil;

import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private ActivityResultLauncher<String> importPicker;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        boolean enabled = CloudSync.isEnabled(requireContext());
        binding.switchCloud.setChecked(enabled);
        binding.switchCloud.setOnCheckedChangeListener((buttonView, isChecked) -> CloudSync.setEnabled(requireContext(), isChecked));

        binding.btnExportCsv.setOnClickListener(v -> {
            String csv = CsvUtil.buildCsv(requireContext());
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/csv");
            share.putExtra(Intent.EXTRA_SUBJECT, "FinAI export");
            share.putExtra(Intent.EXTRA_TEXT, csv);
            startActivity(Intent.createChooser(share, "Share CSV"));
        });

        importPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), (Uri uri) -> {
            if (uri == null) return;
            try {
                InputStream is = requireContext().getContentResolver().openInputStream(uri);
                if (is == null) return;
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line; while ((line = br.readLine()) != null) { sb.append(line).append('\n'); }
                br.close(); is.close();
                CsvUtil.importCsv(requireContext(), sb.toString());
                android.widget.Toast.makeText(requireContext(), "Imported", android.widget.Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                android.widget.Toast.makeText(requireContext(), "Import failed", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnImportCsv.setOnClickListener(v -> importPicker.launch("text/*"));

        binding.btnClearLocal.setOnClickListener(v -> {
            new com.example.finai.data.LocalTransactionsRepository(requireContext()).clear();
            android.widget.Toast.makeText(requireContext(), "Cleared local transactions", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}