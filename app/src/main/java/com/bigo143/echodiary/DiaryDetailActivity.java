package com.bigo143.echodiary;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;
import java.util.concurrent.Executors;

public class DiaryDetailActivity extends AppCompatActivity {

    EditText title, subtitle, content;
    TextView timestamp;
    long entryId; // Unique ID or position to identify the entry

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_detail);

        title = findViewById(R.id.detailTitle);
        subtitle = findViewById(R.id.detailSubtitle);
        content = findViewById(R.id.detailContent);
        timestamp = findViewById(R.id.detailTimestamp);

        Intent intent = getIntent();

        entryId = intent.getLongExtra("entryId", -1); // or use intent.getIntExtra("position", -1)

        title.setText(intent.getStringExtra("title"));
        subtitle.setText(intent.getStringExtra("subtitle"));
        content.setText(intent.getStringExtra("content"));

        long time = intent.getLongExtra("timestamp", 0);
        timestamp.setText(DateFormat.getDateTimeInstance().format(time));

        ImageView detailBack = findViewById(R.id.detailBack);
        detailBack.setOnClickListener(v -> {
            animateClick(detailBack);
            onBackPressed();
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


            DiaryDatabase.getInstance(getApplicationContext()).diaryDao().update(updated);

            runOnUiThread(() -> {
                Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void deleteEntry() {
        Executors.newSingleThreadExecutor().execute(() -> {
            DiaryDatabase.getInstance(getApplicationContext()).diaryDao().deleteById(entryId); // ✅ remove (int) cast


            runOnUiThread(() -> {
                Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}
