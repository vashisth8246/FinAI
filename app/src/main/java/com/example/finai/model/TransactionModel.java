package com.example.finai.model;

public class TransactionModel {
    public double amount;
    public String merchant;
    public String category;
    public double confidence;
    public String dateIso;
    public String type; // credit/debit

    // New: raw SMS message and flag to indicate message-only entries (no parsed amount)
    public String rawMessage;
    public boolean messageOnly;

    public TransactionModel() {}

    public TransactionModel(double amount, String merchant, String category, double confidence, String dateIso, String type) {
        this.amount = amount;
        this.merchant = merchant;
        this.category = category;
        this.confidence = confidence;
        this.dateIso = dateIso;
        this.type = type;
        this.messageOnly = amount <= 0;
    }
}
