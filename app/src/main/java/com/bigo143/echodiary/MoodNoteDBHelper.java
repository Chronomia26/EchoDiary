// MoodNoteDBHelper.java
package com.bigo143.echodiary;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MoodNoteDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mood_notes.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_MOOD = "moods";
    private static final String COL_ID = "id";
    private static final String COL_DATE = "date"; // Format: yyyy/MM/dd
    private static final String COL_MOOD = "mood"; // e.g., "happy", "sad", "neutral"
    private static final String COL_NOTE = "note";
    private static final String TABLE_TASKS = "tasks";
    private static final String COL_TASK_ID = "id";
    private static final String COL_TASK_DATE = "date";
    private static final String COL_TASK_TEXT = "task_text";
    private static final String COL_TASK_TIME = "task_time";
    private static final String COL_TASK_CHECKED = "is_checked";

    public MoodNoteDBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableMood = "CREATE TABLE " + TABLE_MOOD + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_DATE + " TEXT UNIQUE, " +  // one mood+note per date
                COL_MOOD + " TEXT, " +
                COL_NOTE + " TEXT" +
                ")";
        db.execSQL(createTableMood);

        String createTableTask = "CREATE TABLE " + TABLE_TASKS + " (" +
                COL_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TASK_DATE + " TEXT, " +
                COL_TASK_TEXT + " TEXT, " +
                COL_TASK_TIME + " TEXT, " +
                COL_TASK_CHECKED + " INTEGER DEFAULT 0" +
                ")";
        db.execSQL(createTableTask);


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MOOD);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        onCreate(db);
    }

    // Insert or update mood + note for a date
    public void upsertMoodNote(String date, String mood, String note) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_DATE, date);
        values.put(COL_MOOD, mood);
        values.put(COL_NOTE, note);

        int rows = db.update(TABLE_MOOD, values, COL_DATE + "=?", new String[]{date});
        if (rows == 0) {
            db.insert(TABLE_MOOD, null, values);
        }
        db.close();
    }

    // Get mood and note for a date
    public MoodNote getMoodNoteByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_MOOD,
                new String[]{COL_DATE, COL_MOOD, COL_NOTE},
                COL_DATE + "=?",
                new String[]{date},
                null, null, null);

        MoodNote moodNote = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String mood = cursor.getString(cursor.getColumnIndexOrThrow(COL_MOOD));
                String note = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE));
                moodNote = new MoodNote(date, mood, note);
            }
            cursor.close();
        }
        db.close();
        return moodNote;
    }

    public int deleteMoodByDate(String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_MOOD, COL_DATE + "=?", new String[]{date});
        db.close();
        return rows;
    }


    // MoodNote simple data class
    public static class MoodNote {
        public String date;
        public String mood;
        public String note;

        public MoodNote(String date, String mood, String note) {
            this.date = date;
            this.mood = mood;
            this.note = note;
        }
    }

    public static class Task {
        public int id;
        public String date;
        public String text;
        public String time;
        public boolean isChecked;

        public Task() {} // <-- Add this

        public Task(int id, String date, String text, String time, boolean isChecked) {
            this.id = id;
            this.date = date;
            this.text = text;
            this.time = time;
            this.isChecked = isChecked;
        }
        public int getId() {
            return id;
        }

        public String getDate() {
            return date;
        }

        public String getText() {
            return text;
        }

        public String getTime() {
            return time;
        }

        public boolean isChecked() {
            return isChecked;
        }

        // Setters
        public void setDate(String date) {
            this.date = date;
        }

        public void setText(String text) {
            this.text = text;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public void setChecked(boolean checked) {
            this.isChecked = isChecked;
        }
    }


    public void insertTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TASK_TEXT, task.text);
        values.put(COL_TASK_DATE, task.date);
        values.put(COL_TASK_CHECKED, task.isChecked ? 1 : 0);
        values.put(COL_TASK_TIME, task.time);

        db.insert(TABLE_TASKS, null, values);
        db.close();
    }




    public List<Task> getTasksByDate(String date) {
        List<Task> tasks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_TASKS,
                null,
                COL_TASK_DATE + "=?",
                new String[]{date},
                null, null,
                COL_TASK_TIME + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TASK_ID));
                String taskText = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_TEXT));
                String taskTime = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_TIME));
                boolean isChecked = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TASK_CHECKED)) == 1;

                tasks.add(new Task(id, date, taskText, taskTime, isChecked));
            }
            cursor.close();
        }
        db.close();
        return tasks;
    }

    public void updateTaskById(int id, String newText, String newTime, boolean isChecked) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TASK_TEXT, newText);
        values.put(COL_TASK_TIME, newTime);
        values.put(COL_TASK_CHECKED, isChecked ? 1 : 0);
        db.update(TABLE_TASKS, values, COL_TASK_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public int deleteTaskById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_TASKS, COL_TASK_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }




}
