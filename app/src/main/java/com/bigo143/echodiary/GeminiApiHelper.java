package com.bigo143.echodiary;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiApiHelper {

    private static final String API_KEY = "AIzaSyD1yYP--eg--usCTCOwIQjAg8Hnh9wwtcM"; // 👈 Replace this
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public static String summarizeText(String userText) {
        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject requestJson = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject message = new JSONObject();

            JSONObject textPart = new JSONObject();
            textPart.put("text", userText); // <-- wrap text into a JSON object

            message.put("role", "user");
            message.put("parts", new JSONArray().put(textPart));


            contents.put(message);
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
}
