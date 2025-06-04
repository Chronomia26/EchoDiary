package com.bigo143.echodiary;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.concurrent.Executors;

public class DiaryDetailActivity extends AppCompatActivity {

    EditText title, subtitle, content;
    private boolean isRewritten = false;

    TextView timestamp;
    long entryId; // Unique ID or position to identify the entry

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_detail);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.pale_mocha));

        title = findViewById(R.id.detailTitle);
        subtitle = findViewById(R.id.detailSubtitle);
        content = findViewById(R.id.detailContent);
        timestamp = findViewById(R.id.detailTimestamp);

        Intent intent = getIntent();

        entryId = intent.getLongExtra("entryId", -1); // or use intent.getIntExtra("position", -1)
        isRewritten = intent.getBooleanExtra("isRewritten", false); // 👈 receive the flag


        title.setText(intent.getStringExtra("title"));
        subtitle.setText(intent.getStringExtra("subtitle"));
        content.setText(intent.getStringExtra("content"));

        long time = intent.getLongExtra("timestamp", 0);
        timestamp.setText(DateFormat.getDateTimeInstance().format(time));

        ImageView detailBack = findViewById(R.id.detailBack);
        detailBack.setOnClickListener(v -> {
            animateClick(detailBack);
            onBackPressed();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        ImageView detailSave = findViewById(R.id.detailSave);
        detailSave.setOnClickListener(v -> {
            animateClick(detailSave);
            saveChanges();
        });

        ImageView detailDelete = findViewById(R.id.detailDelete);
        detailDelete.setOnClickListener(v -> {
            animateClick(detailDelete);
            deleteEntry();
        });

        ImageView btnSummarize = findViewById(R.id.btnSummarize);
        btnSummarize.setOnClickListener(v -> {
            animateClick(btnSummarize);

            if (isRewritten) {
                runOnUiThread(() -> new android.app.AlertDialog.Builder(this)
                        .setTitle("Already Rewritten")
                        .setMessage("This entry has already been rewritten by AI. Rewrite again?")
                        .setPositiveButton("Rewrite", (dialog, which) -> startRewrite())
                        .setNegativeButton("Cancel", null)
                        .show());
            } else {
                startRewrite();
            }
        });
    }
    private void startRewrite() {
        new Thread(() -> {
            JSONObject resultJson = GeminiApiHelper.summarizeToJson(this, content.getText().toString());

            runOnUiThread(() -> {
                if (resultJson != null) {
                    try {
                        String newTitle = resultJson.getString("title");
                        String newBody = resultJson.getString("body");
                        String newTags = resultJson.getString("tags");

                        title.setText(newTitle);
                        content.setText(newBody);
                        subtitle.setText(newTags);

                        isRewritten = true; // ✅ mark as rewritten
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

    private void saveChanges() {
        Executors.newSingleThreadExecutor().execute(() -> {
            DiaryEntry updated = new DiaryEntry();
            updated.id = entryId;
            updated.title = title.getText().toString();
            updated.subtitle = subtitle.getText().toString();
            updated.content = content.getText().toString();
            updated.timestamp = getIntent().getLongExtra("timestamp", System.currentTimeMillis()); // ✅ Use original
            updated.isRewritten = isRewritten; // ✅ Save the updated rewritten flag



            DiaryDatabase.getInstance(getApplicationContext()).diaryDao().update(updated);

            runOnUiThread(() -> {
                Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show();
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        });
    }

    private void deleteEntry() {
        Executors.newSingleThreadExecutor().execute(() -> {
            DiaryDatabase.getInstance(getApplicationContext()).diaryDao().deleteById(entryId); // ✅ remove (int) cast


            runOnUiThread(() -> {
                Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        });
    }
}
