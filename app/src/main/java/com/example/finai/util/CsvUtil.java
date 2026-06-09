package com.example.finai.util;

import android.content.Context;

import com.example.finai.data.LocalGoalsRepository;
import com.example.finai.data.LocalTransactionsRepository;
import com.example.finai.model.GoalModel;
import com.example.finai.model.TransactionModel;

import java.util.List;

public class CsvUtil {

    public static String buildCsv(Context ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("type,date,merchant,amount,category,txn_type,goal_title,targetAmount,spentSoFar,status\n");
        // Transactions
        List<TransactionModel> tx = new LocalTransactionsRepository(ctx).getAll();
        for (TransactionModel t : tx) {
            sb.append("transaction,")
              .append(s(t.dateIso)).append(',')
              .append(s(t.merchant)).append(',')
              .append(n(t.amount)).append(',')
              .append(s(t.category)).append(',')
              .append(s(t.type)).append(',')
              .append(",,,") // goal fields empty
              .append("\n");
        }
        // Goals
        List<GoalModel> goals = new LocalGoalsRepository(ctx).getGoals();
        for (GoalModel g : goals) {
            sb.append("goal,,,") // date/merchant/amount empty
              .append(",,") // category/txn type empty
              .append(s(g.title)).append(',')
              .append(n(g.targetAmount)).append(',')
              .append(n(g.spentSoFar)).append(',')
              .append(s(g.status))
              .append("\n");
        }
        return sb.toString();
    }

    private static String s(String in) {
        if (in == null) return "";
        String out = in.replace("\"", "\"\"");
        if (out.contains(",") || out.contains("\n")) {
            return "\"" + out + "\"";
        }
        return out;
    }

    private static String n(double in) {
        long l = Math.round(in);
        return String.valueOf(l);
    }

    public static void importCsv(Context ctx, String csv) {
        if (csv == null) return;
        String[] lines = csv.split("\r?\n");
        if (lines.length <= 1) return; // header only
        LocalTransactionsRepository txRepo = new LocalTransactionsRepository(ctx);
        LocalGoalsRepository goalRepo = new LocalGoalsRepository(ctx);
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim(); if (line.isEmpty()) continue;
            String[] cols = splitCsv(line, 10);
            if (cols.length < 10) continue;
            String type = unq(cols[0]);
            if ("transaction".equalsIgnoreCase(type)) {
                TransactionModel t = new TransactionModel();
                t.dateIso = unq(cols[1]);
                t.merchant = unq(cols[2]);
                try { t.amount = Double.parseDouble(unq(cols[3])); } catch (Exception ignored) {}
                t.category = unq(cols[4]);
                t.type = unq(cols[5]);
                txRepo.add(t.merchant, t);
            } else if ("goal".equalsIgnoreCase(type)) {
                String title = unq(cols[6]);
                double target = 0; try { target = Double.parseDouble(unq(cols[7])); } catch (Exception ignored) {}
                goalRepo.addGoal(title, target, "others", 30);
            }
        }
    }

    private static String[] splitCsv(String line, int expected) {
        String[] out = new String[expected];
        int idx = 0; boolean inQ = false; StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') { inQ = !inQ; cur.append(c); }
            else if (c == ',' && !inQ) { out[idx++] = cur.toString(); cur.setLength(0); }
            else { cur.append(c); }
            if (idx >= expected-1) {}
        }
        out[idx] = cur.toString();
        return out;
    }

    private static String unq(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length()-1).replace("\"\"", "\"");
        }
        return s;
    }
}