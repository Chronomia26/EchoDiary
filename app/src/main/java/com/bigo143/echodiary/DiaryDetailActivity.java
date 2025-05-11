package com.bigo143.echodiary;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;

public class DiaryDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_detail);

        TextView title = findViewById(R.id.detailTitle);
        TextView content = findViewById(R.id.detailContent);
        TextView timestamp = findViewById(R.id.detailTimestamp);

        Intent intent = getIntent();
        title.setText(intent.getStringExtra("title"));
        content.setText(intent.getStringExtra("content"));

        long time = intent.getLongExtra("timestamp", 0);
        timestamp.setText(DateFormat.getDateTimeInstance().format(time));
    }
}
