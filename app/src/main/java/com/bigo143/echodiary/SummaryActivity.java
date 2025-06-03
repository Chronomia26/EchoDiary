package com.bigo143.echodiary;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

public class SummaryActivity extends AppCompatActivity {

    TextView tvTitle, tvBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        tvTitle = findViewById(R.id.tvSummaryTitle);
        tvBody = findViewById(R.id.tvSummaryBody);

        long selectedDayMillis = getIntent().getLongExtra("summaryDayMillis", System.currentTimeMillis());

        new Thread(() -> {
            long start = getStartOfDayMillis(selectedDayMillis);
            long end = getEndOfDayMillis(selectedDayMillis);

            List<DiaryEntry> entries = DiaryDatabase.getInstance(this)
                    .diaryDao()
                    .getEntriesForDay(start, end);

            if (entries == null || entries.isEmpty()) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "No entries for selected day.", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

// ... rest of your code that processes entries ...

            StringBuilder combined = new StringBuilder();
            for (DiaryEntry entry : entries) {
                combined.append("- ").append(entry.content).append("\n\n");
            }

            JSONObject summary = GeminiApiHelper.summarizeToJson(this, combined.toString());

            if (summary != null) {
                runOnUiThread(() -> {
                    try {
                        tvTitle.setText(summary.getString("title"));
                        tvBody.setText(summary.getString("body"));
                    } catch (Exception e) {
                        tvTitle.setText("Summary failed");
                        tvBody.setText(e.getMessage());
                        Log.e("SummaryActivity", "Parsing error", e);
                    }
                });
            } else {
                runOnUiThread(() -> {
                    tvTitle.setText("AI Summary Failed");
                    tvBody.setText("Please try again later.");
                });
            }
        }).start();
    }

    private long getStartOfDayMillis(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long getEndOfDayMillis(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }
}
