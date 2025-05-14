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
        detailBack.setOnClickListener(v -> onBackPressed());

        ImageView detailSave = findViewById(R.id.detailSave);
        detailSave.setOnClickListener(v -> saveChanges());

        ImageView detailDelete = findViewById(R.id.detailDelete);
        detailDelete.setOnClickListener(v -> deleteEntry());
    }

    private void saveChanges() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("entryId", entryId);
        resultIntent.putExtra("title", title.getText().toString());
        resultIntent.putExtra("subtitle", subtitle.getText().toString());
        resultIntent.putExtra("content", content.getText().toString());
        setResult(RESULT_OK, resultIntent);
        Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void deleteEntry() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("entryId", entryId);
        resultIntent.putExtra("delete", true);
        setResult(RESULT_OK, resultIntent);
        Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
        finish();
    }
}
