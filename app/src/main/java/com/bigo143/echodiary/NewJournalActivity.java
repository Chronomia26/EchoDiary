package com.bigo143.echodiary;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;


public class NewJournalActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SPEECH_INPUT = 1;

    private EditText journalTitle, journalContent, journalTags;
    private TextView journalDateTime;
    private ImageView btnVoiceInput, btnSave, journalBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_journal);

        journalTitle = findViewById(R.id.journalTitle);
        journalContent = findViewById(R.id.journalContent);
        journalTags = findViewById(R.id.journalTags);
        journalDateTime = findViewById(R.id.journalDateTime);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        btnSave = findViewById(R.id.btnSave);

        btnVoiceInput.setOnClickListener(v -> startVoiceInput());

        ImageView btnSummarize = findViewById(R.id.btnSummarize);

        btnSummarize.setOnClickListener(v -> {
            new Thread(() -> {
                JSONObject resultJson = GeminiApiHelper.summarizeToJson(this, journalContent.getText().toString());

                runOnUiThread(() -> {
                    if (resultJson != null) {
                        try {
                            String newTitle = resultJson.getString("title");
                            String newBody = resultJson.getString("body");
                            journalTitle.setText(newTitle);
                            journalContent.setText(newBody);
                            Toast.makeText(this, "Rewritten with AI ✨", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to parse AI output", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "AI returned nothing", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });



        journalBack = findViewById(R.id.journalBack);
        journalBack.setOnClickListener(v -> onBackPressed());

        btnSave.setOnClickListener(v -> {
            String title = journalTitle.getText().toString();
            String body = journalContent.getText().toString();
            String subtitle = journalTags.getText().toString();
            String dateTime = journalDateTime.getText().toString().trim();

            if (title.isEmpty() || body.isEmpty()) {
                Toast.makeText(this, "Please fill in the title and content.", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Save to Room database using background thread
            DiaryEntry entry = new DiaryEntry();
            entry.id = System.currentTimeMillis(); // ✅ Prevent primary key collision
            entry.title = title;
            entry.subtitle = subtitle; // Assuming you also want to save tags
            entry.content = body;

            SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd yyyy hh:mm a", Locale.ENGLISH);
            try {
                Date date = sdf.parse(dateTime);
                entry.timestamp = date.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
                entry.timestamp = System.currentTimeMillis();
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                DiaryDatabase.getInstance(this).diaryDao().insert(entry);
            });

            Toast.makeText(this, "Journal saved!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your journal entry...");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "Your device doesn't support speech input.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                journalContent.append(spokenText + " ");
            }
        }
    }
}
