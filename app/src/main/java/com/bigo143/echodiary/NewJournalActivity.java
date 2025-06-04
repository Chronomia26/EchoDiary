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
import android.util.Log;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class NewJournalActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SPEECH_INPUT = 1;
    private String originalBeforeRewrite = null;
    private boolean isRewrittenFlag = false;

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

        btnVoiceInput.setOnClickListener(v -> {
            animateClick(btnVoiceInput);
            startVoiceInput();
        });

        ImageView btnSummarize = findViewById(R.id.btnSummarize);
        btnSummarize.setOnClickListener(v -> {
            animateClick(btnSummarize);

            if (isRewrittenFlag) {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Already Rewritten")
                        .setMessage("You've already rewritten this entry. Rewrite again?")
                        .setPositiveButton("Rewrite", (dialog, which) -> startRewrite())
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                startRewrite();
            }
        });

        journalBack = findViewById(R.id.journalBack);
        journalBack.setOnClickListener(v -> {
            animateClick(journalBack);
            onBackPressed();
            overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right);
        });

        btnSave.setOnClickListener(v -> {
            animateClick(btnSave);
            String title = journalTitle.getText().toString();
            String body = journalContent.getText().toString();
            String subtitle = journalTags.getText().toString();
            String dateTime = journalDateTime.getText().toString().trim();
            String original = originalBeforeRewrite != null ? originalBeforeRewrite : journalContent.getText().toString();

            if (title.isEmpty() || body.isEmpty()) {
                Toast.makeText(this, "Please fill in the title and content.", Toast.LENGTH_SHORT).show();
                return;
            }

            DiaryEntry entry = new DiaryEntry();
            entry.id = System.currentTimeMillis();
            entry.title = title;
            entry.subtitle = subtitle;
            entry.content = body;
            entry.originalContent = original;
            entry.isRewritten = isRewrittenFlag;

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
            overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right);
        });
    }

    private void startRewrite() {
        if (originalBeforeRewrite == null) {
            originalBeforeRewrite = journalContent.getText().toString();
        }

        new Thread(() -> {
            JSONObject resultJson = GeminiApiHelper.summarizeToJson(this, journalContent.getText().toString());

            runOnUiThread(() -> {
                if (resultJson != null) {
                    try {
                        String newTitle = resultJson.getString("title");
                        String newBody = resultJson.getString("body");
                        String newTags = resultJson.getString("tags");
                        journalTitle.setText(newTitle);
                        journalContent.setText(newBody);
                        journalTags.setText(newTags);
                        isRewrittenFlag = true;
                        Toast.makeText(this, "Rewritten with AI ✨", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to parse AI output", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "AI returned nothing", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void animateClick(ImageView view) {
        view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(50)).start();
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
