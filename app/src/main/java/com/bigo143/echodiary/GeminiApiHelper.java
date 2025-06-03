package com.bigo143.echodiary;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiApiHelper {

    private static final String API_KEY = "AIzaSyD1yYP--eg--usCTCOwIQjAg8Hnh9wwtcM";
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public static JSONObject summarizeToJson(Context context, String userText) {
        OkHttpClient client = new OkHttpClient();

        try {
            String systemPrompt = loadSystemPromptFromRaw(context);

            JSONObject sysPart = new JSONObject().put("text", systemPrompt);
            JSONObject userPart = new JSONObject().put("text", userText);
            JSONArray contents = new JSONArray();

            contents.put(new JSONObject().put("role", "user").put("parts", new JSONArray().put(sysPart)));
            contents.put(new JSONObject().put("role", "user").put("parts", new JSONArray().put(userPart)));

            JSONObject requestJson = new JSONObject().put("contents", contents);

            RequestBody body = RequestBody.create(requestJson.toString(), JSON);
            Request request = new Request.Builder().url(ENDPOINT).post(body).build();

            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                String text = json
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
                text = text.replaceAll("(?s)```json\\s*|```", "").trim();

                // Parse response string into JSON
                return new JSONObject(text);
            } else {
                Log.e("GeminiAPI", "API Error: " + response.body().string());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public static String summarizeText(Context context, String userText) {
        OkHttpClient client = new OkHttpClient();

        try {
            String systemPrompt = loadSystemPromptFromRaw(context);

            JSONObject sysPart = new JSONObject().put("text", systemPrompt);
            JSONObject userPart = new JSONObject().put("text", userText);
            JSONArray contents = new JSONArray();

            contents.put(new JSONObject().put("role", "user").put("parts", new JSONArray().put(sysPart)));
            contents.put(new JSONObject().put("role", "user").put("parts", new JSONArray().put(userPart)));

            JSONObject requestJson = new JSONObject().put("contents", contents);

            RequestBody body = RequestBody.create(requestJson.toString(), JSON);
            Request request = new Request.Builder().url(ENDPOINT).post(body).build();

            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                String text = json
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
                return text.trim();
            } else {
                Log.e("GeminiAPI", "API Error: " + response.body().string());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Failed to get AI response.";
    }


    private static String loadSystemPromptFromRaw(Context context) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getResources().openRawResource(R.raw.gemini_prompt)))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            Log.e("GeminiAPI", "Failed to load prompt: " + e.getMessage());
            return "Rewrite this journal entry plainly.";
        }
    }
}
