package com.bigo143.echodiary;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText editTextTitle, editTextSubtitle, editTextBody;
    private TextView textViewDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        // Initialize views
        editTextTitle = findViewById(R.id.edittext_Title);
        editTextSubtitle = findViewById(R.id.edittext_Subtitle);
        editTextBody = findViewById(R.id.edittext_Body);
        textViewDateTime = findViewById(R.id.textview_DateTime);

        // Set current date/time
        String currentDateTime = new SimpleDateFormat("EEE, MMM d yyyy HH:mm a", Locale.getDefault()).format(new Date());
        textViewDateTime.setText(currentDateTime);

        // Back button
        ImageView imageBack = findViewById(R.id.image_Back);
        imageBack.setOnClickListener(v -> onBackPressed());

        // Save button
        ImageView imageSave = findViewById(R.id.image_Save);
        imageSave.setOnClickListener(v -> saveNote());
    }


    private void saveNote() {
        String title = editTextTitle.getText().toString().trim();
        String subtitle = editTextSubtitle.getText().toString().trim();
        String body = editTextBody.getText().toString().trim();
        String dateTime = textViewDateTime.getText().toString().trim();

        if (title.isEmpty() && subtitle.isEmpty() && body.isEmpty()) {
            Toast.makeText(this, "Cannot save empty note", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create DiaryEntry object
        DiaryEntry entry = new DiaryEntry();
        entry.title = title;
        entry.subtitle = subtitle;
        entry.content = body;
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd yyyy hh:mm a", Locale.ENGLISH);
        try {
            Date date = sdf.parse(dateTime);
            entry.timestamp = date.getTime(); // This is the correct long value
        } catch (ParseException e) {
            e.printStackTrace();
            // Optional: Show a Toast or default to current time if parsing fails
            entry.timestamp = System.currentTimeMillis();
        }


        // Insert into database (in background thread)
        Executors.newSingleThreadExecutor().execute(() -> {
            DiaryDatabase.getInstance(getApplicationContext())
                    .diaryDao()
                    .insert(entry);
            runOnUiThread(() -> {
                Toast.makeText(CreateNoteActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                finish(); // Go back to DiaryFragment
            });
        });
    }
}
