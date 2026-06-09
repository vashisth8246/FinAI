package com.example.finai.ai;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiClient {

    private static final String MODEL = "gemini-1.5-flash";
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent?key=";

    private final OkHttpClient http = new OkHttpClient();
    private final String apiKey;

    public GeminiClient(String apiKey) { this.apiKey = apiKey; }

    @Nullable
    public JSONObject categorizeTransaction(String message) throws IOException, JSONException {
        String prompt = "Categorize this transaction logically (groceries, dining, transport, rent, bills, others). Return JSON with category + confidence. Message: " + message;
        return call(prompt);
    }

    @Nullable
    public JSONObject summarizeInsights(JSONArray transactions) throws IOException, JSONException {
        String prompt = "Analyze user's transactions for the past 30 days and summarize: - Top spending categories - Weekly total trend - Possible saving suggestion. Return JSON + natural summary text. Transactions: " + transactions.toString();
        return call(prompt);
    }

    @Nullable
    public JSONObject chat(String query, JSONArray context) throws IOException, JSONException {
        String prompt = "You are FinAI, a helpful finance coach. Context: " + context.toString() + "\nUser: " + query;
        return call(prompt);
    }

    @Nullable
    private JSONObject call(String prompt) throws IOException, JSONException {
        if (apiKey == null || apiKey.isEmpty()) return null;
        JSONObject content = new JSONObject()
                .put("contents", new JSONArray().put(new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", prompt)))));
        RequestBody body = RequestBody.create(content.toString(), MediaType.parse("application/json"));
        Request req = new Request.Builder().url(ENDPOINT + apiKey).post(body).build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) return null;
            String raw = resp.body().string();
            return new JSONObject(raw);
        }
    }
}