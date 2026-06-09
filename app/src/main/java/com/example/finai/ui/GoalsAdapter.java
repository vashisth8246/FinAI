package com.example.finai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finai.R;
import com.example.finai.model.GoalModel;

import java.util.ArrayList;
import java.util.List;

public class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.VH> {

    private final List<GoalModel> data = new ArrayList<>();

    public GoalModel get(int position) {
        return position >=0 && position < data.size() ? data.get(position) : null;
    }

    public void submit(List<GoalModel> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goal, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        GoalModel g = data.get(i);
        h.title.setText(g.title);
        h.target.setText("Target: ₹" + (long)g.targetAmount);
        h.progress.setText("Progress: ₹" + (long)g.spentSoFar + " / ₹" + (long)g.targetAmount);
        h.status.setText("Status: " + (g.status == null ? "active" : g.status));
        int pct = (g.targetAmount > 0) ? (int) Math.min(100, Math.round((g.spentSoFar / g.targetAmount) * 100)) : 0;
        h.progressBar.setProgress(pct);
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, target, progress, status;
        com.google.android.material.progressindicator.LinearProgressIndicator progressBar;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            target = itemView.findViewById(R.id.target);
            progress = itemView.findViewById(R.id.progress);
            status = itemView.findViewById(R.id.status);
            progressBar = itemView.findViewById(R.id.goalProgressBar);
        }
    }
}
