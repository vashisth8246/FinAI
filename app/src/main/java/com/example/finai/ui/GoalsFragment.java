package com.example.finai.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;

import com.example.finai.data.FirebaseRepository;
import com.example.finai.data.LocalGoalsRepository;
import com.example.finai.databinding.FragmentGoalsBinding;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class GoalsFragment extends Fragment {

    private FragmentGoalsBinding binding;
    private GoalsAdapter adapter;
    private ListenerRegistration reg;
    private boolean useLocal = false;
    private LocalGoalsRepository localRepo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGoalsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new GoalsAdapter();
        binding.goalsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.goalsList.setAdapter(adapter);

        if (!FirebaseRepository.getInstance().isReady()) {
            // Fallback to local storage
            useLocal = true;
            binding.emptyText.setVisibility(View.GONE);
            localRepo = new LocalGoalsRepository(requireContext());
            refreshLocal();
        } else {
            reg = FirebaseRepository.getInstance().listenGoals(new FirebaseRepository.GoalsListener() {
                @Override public void onChange(List<com.example.finai.model.GoalModel> list) {
                    adapter.submit(list);
                    if (list == null || list.isEmpty()) {
                        binding.emptyText.setVisibility(View.VISIBLE);
                        binding.emptyText.setText("No goals yet. Tap + to create your first goal.");
                    } else {
                        binding.emptyText.setVisibility(View.GONE);
                    }
                }
                @Override public void onError(Exception e) { Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show(); }
            });
        }

        binding.fabAddGoal.setOnClickListener(v -> showAddGoalDialog());

        attachSwipeHandlers();
    }

    private void attachSwipeHandlers() {
        ItemTouchHelper.SimpleCallback cb = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getBindingAdapterPosition();
                com.example.finai.model.GoalModel g = adapter.get(pos);
                if (g == null) return;
                if (dir == ItemTouchHelper.LEFT) {
                    // delete
                    if (useLocal) {
                        localRepo.removeGoal(g.id);
                        refreshLocal();
Snackbar.make(binding.getRoot(), "Goal deleted", Snackbar.LENGTH_LONG)
                                .setAction("Undo", v -> { localRepo.addGoal(g.title, g.targetAmount, g.category, g.periodDays > 0 ? g.periodDays : 30); refreshLocal(); })
                                .show();
                    } else {
                        FirebaseRepository.getInstance().deleteGoal(g.id)
                                .addOnSuccessListener(v -> Snackbar.make(binding.getRoot(), "Goal deleted", Snackbar.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    // edit
                    showEditGoalDialog(g);
                }
            }
        };
        new ItemTouchHelper(cb).attachToRecyclerView(binding.goalsList);
    }

    private void showEditGoalDialog(com.example.finai.model.GoalModel g) {
        EditText title = new EditText(requireContext());
        title.setHint("Title"); title.setText(g.title);
        EditText amt = new EditText(requireContext());
        amt.setHint("Target amount (₹)");
        amt.setInputType(InputType.TYPE_CLASS_NUMBER);
        amt.setText(String.valueOf((long)g.targetAmount));
        EditText cat = new EditText(requireContext());
        cat.setHint("Category (food, groceries, travel, bills, rent, others)");
        cat.setText(g.category == null ? "" : g.category);
        EditText days = new EditText(requireContext());
        days.setHint("Period in days (e.g., 30)");
        days.setInputType(InputType.TYPE_CLASS_NUMBER);
        days.setText(String.valueOf(g.periodDays > 0 ? g.periodDays : 30));
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad,pad,pad,pad);
        box.addView(title);
        box.addView(amt);
        box.addView(cat);
        box.addView(days);
        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Goal")
                .setView(box)
                .setPositiveButton("Save", (d, w) -> {
                    String t = title.getText().toString().trim();
                    String a = amt.getText().toString().trim();
                    String c = cat.getText().toString().trim().toLowerCase();
                    String pd = days.getText().toString().trim();
                    double val = 0; try { val = Double.parseDouble(a); } catch (Exception ignored) {}
                    int dd = 30; try { dd = Integer.parseInt(pd); } catch (Exception ignored) {}
                    if (useLocal) {
                        localRepo.updateGoal(g.id, t, val, c, dd);
                        refreshLocal();
                        Snackbar.make(binding.getRoot(), "Goal updated", Snackbar.LENGTH_SHORT).show();
                    } else {
                        FirebaseRepository.getInstance().updateGoal(g.id, t, val)
                                .addOnSuccessListener(r -> Snackbar.make(binding.getRoot(), "Goal updated", Snackbar.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", (d,w) -> adapter.notifyDataSetChanged())
                .show();
    }

    private void refreshLocal() {
        if (localRepo != null) {
            List<com.example.finai.model.GoalModel> list = localRepo.getGoals();
            // Compute progress from transactions
            com.example.finai.data.LocalTransactionsRepository txRepo = new com.example.finai.data.LocalTransactionsRepository(requireContext());
            java.util.List<com.example.finai.model.TransactionModel> txs = txRepo.getAll();
            for (com.example.finai.model.GoalModel g : list) {
                g.spentSoFar = (float) sumSpend(txs, g.category == null ? "others" : g.category, g.periodDays > 0 ? g.periodDays : 30);
                if (g.targetAmount > 0 && g.spentSoFar >= g.targetAmount) g.status = "achieved";
            }
            adapter.submit(list);
        }
    }

    private double sumSpend(java.util.List<com.example.finai.model.TransactionModel> txs, String category, int days) {
        double total = 0;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            long today = sdf.parse(new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date())).getTime();
            long dayMs = 24L*60*60*1000;
            for (com.example.finai.model.TransactionModel t : txs) {
                if (t == null || t.dateIso == null) continue;
                if (t.type != null && t.type.equalsIgnoreCase("credit")) continue; // only spending
                if (category != null && !category.equalsIgnoreCase("all") && t.category != null && !t.category.equalsIgnoreCase(category)) continue;
                java.util.Date dt = sdf.parse(t.dateIso);
                if (dt == null) continue;
                long diff = (today - dt.getTime())/dayMs;
                if (diff >= 0 && diff < days) total += Math.max(0, t.amount);
            }
        } catch (Exception ignored) {}
        return total;
    }

    private void showAddGoalDialog() {
        EditText title = new EditText(requireContext());
        title.setHint("Title");
        EditText amt = new EditText(requireContext());
        amt.setHint("Target amount (₹)");
        amt.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText cat = new EditText(requireContext());
        cat.setHint("Category (food, groceries, travel, bills, rent, others)");
        EditText days = new EditText(requireContext());
        days.setHint("Period in days (e.g., 30)");
        days.setInputType(InputType.TYPE_CLASS_NUMBER);
        days.setText("30");
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad,pad,pad,pad);
        box.addView(title);
        box.addView(amt);
        box.addView(cat);
        box.addView(days);
        new AlertDialog.Builder(requireContext())
                .setTitle("New Goal")
                .setView(box)
                .setPositiveButton("Save", (d, w) -> {
                    String t = title.getText().toString().trim();
                    String a = amt.getText().toString().trim();
                    String c = cat.getText().toString().trim().toLowerCase();
                    String pd = days.getText().toString().trim();
                    double val = 0; try { val = Double.parseDouble(a); } catch (Exception ignored) {}
                    int dd = 30; try { dd = Integer.parseInt(pd); } catch (Exception ignored) {}
                    if (useLocal) {
                        localRepo.addGoal(t, val, c, dd);
                        refreshLocal();
                        Snackbar.make(binding.getRoot(), "Goal added", Snackbar.LENGTH_SHORT).show();
                    } else {
                        FirebaseRepository.getInstance().addGoal(t, val)
                                .addOnSuccessListener(r -> Snackbar.make(binding.getRoot(), "Goal added", Snackbar.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reg != null) reg.remove();
        binding = null;
    }
}
