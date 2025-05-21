// MoodManager.java
package com.bigo143.echodiary;

import android.content.Context;

public class MoodManager {

    private final MoodNoteDBHelper dbHelper;

    public MoodManager(Context context) {
        dbHelper = new MoodNoteDBHelper(context);
    }

    // Save mood + note for a date
    public void saveMood(String date, String mood, String note) {
        dbHelper.upsertMoodNote(date, mood, note);
    }

    // Get mood + note for a date
    public MoodNoteDBHelper.MoodNote getMood(String date) {
        return dbHelper.getMoodNoteByDate(date);
    }

    public static class MoodNote {
        public String date;
        public String mood;
    }

    public void deleteMood(String date) {
        dbHelper.deleteMoodByDate(date);
    }


    // You can add more mood-related helper methods here
}
