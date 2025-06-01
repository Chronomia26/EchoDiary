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

    public static String summarizeText(Context context, String userText) {
        OkHttpClient client = new OkHttpClient();

        try {
            // Read system prompt from JSON in res/raw
            String systemPrompt = loadSystemPromptFromRaw(context);

            // Create part 1: system instruction
            JSONObject sysPart = new JSONObject();
            sysPart.put("text", systemPrompt);

            JSONObject userPart = new JSONObject();
            userPart.put("text", userText);

            JSONObject sysMessage = new JSONObject();
            sysMessage.put("role", "user");
            sysMessage.put("parts", new JSONArray().put(sysPart));

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("parts", new JSONArray().put(userPart));

            JSONArray contents = new JSONArray();
            contents.put(sysMessage);
            contents.put(userMessage);

            JSONObject requestJson = new JSONObject();
            requestJson.put("contents", contents);

            RequestBody body = RequestBody.create(requestJson.toString(), JSON);
            Request request = new Request.Builder()
                    .url(ENDPOINT)
                    .post(body)
                    .build();

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
                return text;
            } else {
                Log.e("GeminiAPI", "API Error: " + response.body().string());
                return "Error: " + response.code();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
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
