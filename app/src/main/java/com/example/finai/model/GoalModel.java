package com.example.finai.model;

public class GoalModel {
    public String id;
    public String title;
    public double targetAmount;
    public double spentSoFar;
    public String status;

    // New: budgeting context
    public String category;   // e.g., food, groceries, travel, bills, rent, others
    public int periodDays;    // rolling window, default 30
}
