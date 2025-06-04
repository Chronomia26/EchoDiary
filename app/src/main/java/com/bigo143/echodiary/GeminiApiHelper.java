package com.bigo143.echodiary;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Callback; // Import OkHttp Callback

public class GeminiApiHelper {

    private static final String API_KEY = "AIzaSyD1yYP--eg--usCTCOwIQjAg8Hnh9wwtcM"; // IMPORTANT: Securely handle your API key!
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Define the specific prompt for app usage advice internally (for the new method)
    private static final String APP_USAGE_ADVICE_PROMPT_INTERNAL =
            "You are an assistant that analyzes daily app usage data. Given a JSON object of apps used and duration in minutes, respond with a summary of the user’s digital behavior that day. Suggest how to improve focus, productivity, or digital wellbeing. The advice should be a single, concise statement.\n" +
                    "\n" +
                    "Input will look like this:\n" +
                    "{\n" +
                    "  \"daytrace\": {\n" +
                    "    \"YouTube\": 120,\n" +
                    "    \"Chrome\": 45,\n" +
                    "    \"Instagram\": 90,\n" +
                    "    \"Notion\": 30\n" +
                    "  }\n" +
                    "}\n" +
                    "\n" +
                    "Respond in JSON with the following keys:\n" +
                    "- \"summary\": A short summary of the day’s digital behavior.\n" +
                    "- \"advice\": Personalized advice based on the usage data to improve digital wellbeing.";


    // ✅ 1. JSON-parsing version (already exists - UNCHANGED)
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
                return new JSONObject(text);
            } else {
                String errorBody = response.body().string();
                Log.e("GeminiAPI", "API Error: " + response.code() + " - " + errorBody);
            }
        } catch (Exception e) {
            Log.e("GeminiAPI", "SummarizeToJson failed: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // ✅ 2. Simple summarization (plain string - UNCHANGED)
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
                String errorBody = response.body().string();
                Log.e("GeminiAPI", "API Error: " + response.code() + " - " + errorBody);
            }
        } catch (Exception e) {
            Log.e("GeminiAPI", "SummarizeText failed: " + e.getMessage());
            e.printStackTrace();
        }

        return "Failed to get AI response.";
    }

    // ✅ Reuse the shared system prompt (this method is still here for other uses - UNCHANGED)
    private static String loadSystemPromptFromRaw(Context context) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getResources().openRawResource(R.raw.gemini_prompt)))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException | android.content.res.Resources.NotFoundException e) {
            Log.e("GeminiAPI", "Failed to load prompt from raw resource: " + e.getMessage());
            // Provide a default prompt or handle the error appropriately if this method is called elsewhere
            return "You are a helpful assistant.";
        }
    }

    // Existing interface for asynchronous API responses (UNCHANGED)
    public interface ApiResponseCallback<T> {
        void onSuccess(T result);
        void onFailure(Throwable error);
    }

    // Existing method for getting app usage advice asynchronously (uses the resource file prompt - UNCHANGED)
    // Keep this method as requested by the user to not touch existing code
    public static void getAppUsageAdviceAsync(Context context, Map<String, Long> appUsageData, ApiResponseCallback<String> callback) {
        OkHttpClient client = new OkHttpClient();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        JSONObject trace = new JSONObject();
        try {
            for (Map.Entry<String, Long> entry : appUsageData.entrySet()) {
                long durationMinutes = entry.getValue() / (1000 * 60);
                if (durationMinutes > 0) {
                    trace.put(entry.getKey(), durationMinutes);
                }
            }

            JSONObject daytrace = new JSONObject();
            daytrace.put("daytrace", trace);

            String userText = daytrace.toString();
            // This method uses the prompt loaded from the resource file
            String systemPrompt = loadSystemPromptFromRaw(context);


            JSONObject sysPart = new JSONObject().put("text", systemPrompt);
            JSONObject userPart = new JSONObject().put("text", userText);
            JSONArray contents = new JSONArray();

            contents.put(new JSONObject().put("role", "user").put("parts", new JSONArray().put(sysPart)));
            contents.put(new JSONObject().put("role", "user").put("parts", new JSONArray().put(userPart)));

            JSONObject requestJson = new JSONObject().put("contents", contents);

            RequestBody body = RequestBody.create(requestJson.toString(), JSON);
            Request request = new Request.Builder().url(ENDPOINT).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e("GeminiAPI", "Async call failed (Resource Prompt): " + e.getMessage(), e);
                    mainHandler.post(() -> callback.onFailure(e));
                }

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    final String responseBodyString;
                    try {
                        responseBodyString = response.body().string();
                        Log.d("GeminiAPI", "Raw API Response (Resource Prompt): " + responseBodyString);

                        if (response.isSuccessful()) {
                            try {
                                JSONObject json = new JSONObject(responseBodyString);
                                String text = json
                                        .getJSONArray("candidates")
                                        .getJSONObject(0)
                                        .getJSONObject("content")
                                        .getJSONArray("parts")
                                        .getJSONObject(0)
                                        .getString("text");

                                text = text.replaceAll("(?s)```json\\s*|```", "").trim();

                                // Attempt to parse the cleaned text as JSON to get the 'advice' field
                                JSONObject adviceJson = new JSONObject(text);
                                String advice = adviceJson.optString("advice", ""); // Extract the "advice" field

                                mainHandler.post(() -> callback.onSuccess(advice));
                            } catch (JSONException e) {
                                Log.e("GeminiAPI", "Response parsing failed (Resource Prompt): " + e.getMessage(), e);
                                Log.e("GeminiAPI", "Failed to parse JSON from successful body (Resource Prompt): " + responseBodyString);
                                mainHandler.post(() -> callback.onFailure(e));
                            } catch (Exception e) {
                                Log.e("GeminiAPI", "An unexpected error occurred during successful response handling (Resource Prompt): " + e.getMessage(), e);
                                Log.e("GeminiAPI", "Error occurred after parsing successful body (Resource Prompt): " + responseBodyString);
                                mainHandler.post(() -> callback.onFailure(e));
                            }
                        } else {
                            Log.e("GeminiAPI", "API Error (Async Resource Prompt): " + response.code() + " - " + responseBodyString);
                            mainHandler.post(() -> callback.onFailure(new Exception("API Error (Resource Prompt): " + response.code() + " - " + responseBodyString)));
                        }
                    } catch (IOException e) {
                        Log.e("GeminiAPI", "Failed to read response body (Resource Prompt): " + e.getMessage(), e);
                        mainHandler.post(() -> callback.onFailure(e));
                    } finally {
                        if (response.body() != null) {
                            response.body().close();
                        }
                    }
                }
            });

        } catch (JSONException e) {
            Log.e("GeminiAPI", "Request JSON building failed (Resource Prompt): " + e.getMessage(), e);
            mainHandler.post(() -> callback.onFailure(e));
        } catch (Exception e) {
            Log.e("GeminiAPI", "Request setup failed (Resource Prompt): " + e.getMessage(), e);
            mainHandler.post(() -> callback.onFailure(e));
        }
    }


    // New method for getting app usage advice asynchronously (uses the internal prompt)
    // This method is specifically for the ActivityFragment
    public static void getAppUsageAdviceAsyncWithInternalPrompt(Context context, Map<String, Long> appUsageData, ApiResponseCallback<String> callback) {
        OkHttpClient client = new OkHttpClient();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        // Build the JSON input string expected by the Gemini API based on your format
        JSONObject trace = new JSONObject();
        try {
            for (Map.Entry<String, Long> entry : appUsageData.entrySet()) {
                // Convert duration from milliseconds to minutes for the prompt
                long durationMinutes = entry.getValue() / (1000 * 60);
                // Only include apps with at least 1 minute of usage
                if (durationMinutes > 0) {
                    trace.put(entry.getKey(), durationMinutes);
                }
            }

            JSONObject daytrace = new JSONObject();
            daytrace.put("daytrace", trace);

            String userText = daytrace.toString();
            // Use the hardcoded prompt directly here for the ActivityFragment
            String systemPrompt = APP_USAGE_ADVICE_PROMPT_INTERNAL;


            JSONObject sysPart = new JSONObject().put("text", systemPrompt);
            JSONObject userPart = new JSONObject().put("text", userText);
            JSONArray contents = new JSONArray();

            // Add roles and parts as required by the Gemini API
            contents.put(new JSONObject().put("role", "user").put("parts", new JSONArray().put(sysPart)));
            contents.put(new JSONObject().put("role", "user").put("parts", new JSONArray().put(userPart)));

            JSONObject requestJson = new JSONObject().put("contents", contents);

            RequestBody body = RequestBody.create(requestJson.toString(), JSON);
            Request request = new Request.Builder().url(ENDPOINT).post(body).build();

            // Make the asynchronous network call
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e("GeminiAPI", "Async call failed (Internal Prompt): " + e.getMessage(), e); // Log the error with stack trace
                    mainHandler.post(() -> callback.onFailure(e));
                }

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    final String responseBodyString; // Declare as final here
                    try {
                        // Read the response body first
                        responseBodyString = response.body().string();
                        Log.d("GeminiAPI", "Raw API Response (Internal Prompt): " + responseBodyString); // Log the raw response

                        if (response.isSuccessful()) {
                            try {
                                JSONObject json = new JSONObject(responseBodyString);
                                Log.d("GeminiAPI", "Parsed Response JSON (Internal Prompt): " + json.toString(4)); // Log the parsed JSON (formatted)

                                // Extract the text content from the response structure
                                String text = json
                                        .getJSONArray("candidates")
                                        .getJSONObject(0)
                                        .getJSONObject("content")
                                        .getJSONArray("parts")
                                        .getJSONObject(0)
                                        .getString("text");
                                Log.d("GeminiAPI", "Extracted Text Content (Internal Prompt): " + text); // Log the extracted text

                                // Clean up markdown fences and trim whitespace
                                text = text.replaceAll("(?s)```json\\s*|```", "").trim();
                                Log.d("GeminiAPI", "Cleaned Text Content (Internal Prompt): " + text); // Log the cleaned text

                                // Attempt to parse the cleaned text as JSON to get the 'advice' field
                                JSONObject adviceJson = new JSONObject(text);
                                Log.d("GeminiAPI", "Parsed Advice JSON (Internal Prompt): " + adviceJson.toString(4)); // Log the parsed advice JSON
                                String advice = adviceJson.optString("advice", ""); // Extract the "advice" field
                                Log.d("GeminiAPI", "Extracted Advice Field (Internal Prompt): '" + advice + "'"); // Log the extracted advice field


                                // Post the result back to the main thread
                                mainHandler.post(() -> callback.onSuccess(advice));
                            } catch (JSONException e) {
                                Log.e("GeminiAPI", "Response parsing failed (Internal Prompt): " + e.getMessage(), e); // Log JSON error with stack trace
                                Log.e("GeminiAPI", "Failed to parse JSON from successful body (Internal Prompt): " + responseBodyString);
                                mainHandler.post(() -> callback.onFailure(e));
                            } catch (Exception e) {
                                Log.e("GeminiAPI", "An unexpected error occurred during successful response handling (Internal Prompt): " + e.getMessage(), e); // Log other errors
                                Log.e("GeminiAPI", "Error occurred after parsing successful body (Internal Prompt): " + responseBodyString);
                                mainHandler.post(() -> callback.onFailure(e));
                            }
                        } else {
                            // If not successful, log the error body string and call onFailure
                            Log.e("GeminiAPI", "API Error (Async Internal Prompt): " + response.code() + " - " + responseBodyString);
                            mainHandler.post(() -> callback.onFailure(new Exception("API Error (Internal Prompt): " + response.code() + " - " + responseBodyString)));
                        }
                    } catch (IOException e) {
                        // This catch block handles errors specifically from response.body().string()
                        Log.e("GeminiAPI", "Failed to read response body (Internal Prompt): " + e.getMessage(), e);
                        mainHandler.post(() -> callback.onFailure(e)); // Report the failure to read the body
                    } finally {
                        if (response.body() != null) {
                            response.body().close();
                        }
                    }
                }
            });

        } catch (JSONException e) {
            // Handle errors during JSON building for the request
            Log.e("GeminiAPI", "Request JSON building failed (Internal Prompt): " + e.getMessage(), e); // Log JSON error with stack trace
            mainHandler.post(() -> callback.onFailure(e));
        } catch (Exception e) {
            // Handle other potential errors during setup
            Log.e("GeminiAPI", "Request setup failed (Internal Prompt): " + e.getMessage(), e); // Log other errors
            mainHandler.post(() -> callback.onFailure(e));
        }
    }
}