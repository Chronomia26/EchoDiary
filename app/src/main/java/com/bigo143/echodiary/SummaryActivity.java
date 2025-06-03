package com.bigo143.echodiary;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SummaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long targetDayMillis = getIntent().getLongExtra("summaryDayMillis", System.currentTimeMillis());
        summarizeSelectedDay(targetDayMillis);
    }

    private void summarizeSelectedDay(long dayMillis) {
        new Thread(() -> {
            long start = getStartOfDayMillis(dayMillis);
            long end = getEndOfDayMillis(dayMillis);

            List<DiaryEntry> entries = DiaryDatabase.getInstance(this)
                    .diaryDao()
                    .getEntriesForDay(start, end);

            if (entries == null || entries.isEmpty()) {
                runOnUiThread(() ->
                        Toast.makeText(this, "No entries for this date.", Toast.LENGTH_SHORT).show()
                );
                finish();
                return;
            }

            StringBuilder combined = new StringBuilder();
            for (DiaryEntry entry : entries) {
                combined.append("- ").append(entry.content).append("\n\n");
            }

            String prompt = "Summarize my day based on these journal entries. " +
                    "Write in first-person diary tone and keep it concise:\n" + combined;

            String result = GeminiApiHelper.summarizeText(this, prompt);

            runOnUiThread(() -> {
                new AlertDialog.Builder(this)
                        .setTitle("✨ AI Summary")
                        .setMessage(result)
                        .setPositiveButton("OK", (dialog, which) -> finish())
                        .show();
            });

        }).start();
    }

    private long getStartOfDayMillis(long dayMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dayMillis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long getEndOfDayMillis(long dayMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dayMillis);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }
}
