package com.example.finai.data;

import com.example.finai.model.TransactionModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.finai.FinAIApp;
import com.example.finai.util.CloudSync;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class FirebaseRepository {

    public interface GoalsListener {
        void onChange(java.util.List<com.example.finai.model.GoalModel> list);
        void onError(Exception e);
    }

private static FirebaseRepository INSTANCE;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private boolean ready;

public static synchronized FirebaseRepository getInstance() {
        if (INSTANCE == null) INSTANCE = new FirebaseRepository();
        return INSTANCE;
    }

    private FirebaseRepository() {
        try {
            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();
            ready = true;
        } catch (IllegalStateException e) {
            ready = false;
        }
    }

    public boolean isReady() { return ready; }

private String uid() { return (auth != null && auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : "guest"; }

public Task<DocumentReference> addTransactionFromSms(String message, TransactionModel parsed) {
        if (!ready || !CloudSync.isEnabled(FinAIApp.get())) return Tasks.forException(new IllegalStateException("Cloud sync disabled"));
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        data.put("amount", parsed.amount);
        data.put("merchant", parsed.merchant);
        data.put("category", parsed.category);
        data.put("confidence", parsed.confidence);
        data.put("date", parsed.dateIso);
        data.put("type", parsed.type);
        data.put("aiSource", "sms_regex");
        return db.collection("users").document(uid())
                .collection("transactions").add(data);
    }

public com.google.firebase.firestore.ListenerRegistration listenGoals(GoalsListener listener) {
        if (!ready) return null;
        return db.collection("users").document(uid())
                .collection("goals")
                .addSnapshotListener((snap, err) -> {
                    if (err != null) { listener.onError(err); return; }
                    java.util.List<com.example.finai.model.GoalModel> list = new java.util.ArrayList<>();
                    if (snap != null) for (var d : snap.getDocuments()) {
                        com.example.finai.model.GoalModel g = d.toObject(com.example.finai.model.GoalModel.class);
                        if (g != null) { g.id = d.getId(); list.add(g); }
                    }
                    listener.onChange(list);
                });
    }

public Task<DocumentReference> addGoal(String title, double targetAmount) {
        if (!ready || !CloudSync.isEnabled(FinAIApp.get())) return Tasks.forException(new IllegalStateException("Cloud sync disabled"));
        Map<String,Object> data = new HashMap<>();
        data.put("title", title);
        data.put("targetAmount", targetAmount);
        data.put("spentSoFar", 0.0);
        data.put("status", "active");
        return db.collection("users").document(uid())
                .collection("goals").add(data);
    }

public com.google.android.gms.tasks.Task<Void> deleteGoal(String id) {
        if (!ready || !CloudSync.isEnabled(FinAIApp.get())) return Tasks.forException(new IllegalStateException("Cloud sync disabled"));
        return db.collection("users").document(uid())
                .collection("goals").document(id).delete();
    }

public com.google.android.gms.tasks.Task<Void> updateGoal(String id, String title, double targetAmount) {
        if (!ready || !CloudSync.isEnabled(FinAIApp.get())) return Tasks.forException(new IllegalStateException("Cloud sync disabled"));
        Map<String,Object> data = new HashMap<>();
        data.put("title", title);
        data.put("targetAmount", targetAmount);
        return db.collection("users").document(uid())
                .collection("goals").document(id).update(data);
    }
}
