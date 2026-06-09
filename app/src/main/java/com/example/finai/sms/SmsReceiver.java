package com.example.finai.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import com.example.finai.data.FirebaseRepository;
import com.example.finai.data.LocalTransactionsRepository;
import com.example.finai.utils.SmsParser;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                for (SmsMessage sms : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    String body = sms.getMessageBody();
                    var parsed = SmsParser.parse(body, sms.getTimestampMillis());
                    // Save locally first so UI always shows it
                    new LocalTransactionsRepository(context.getApplicationContext()).add(body, parsed != null ? parsed : new com.example.finai.model.TransactionModel());
                    // If Firebase configured, sync to cloud
                    if (parsed != null && com.example.finai.util.CloudSync.isEnabled(context)) {
                        try { FirebaseRepository.getInstance().addTransactionFromSms(body, parsed); } catch (Throwable ignored) {}
                    }
                }
            }
        }
    }
}