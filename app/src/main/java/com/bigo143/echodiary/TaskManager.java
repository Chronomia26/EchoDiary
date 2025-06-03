package com.bigo143.echodiary;

import android.content.Context;
import java.util.List;

public class TaskManager {
    private MoodNoteDBHelper dbHelper;

    public TaskManager(Context context) {
        dbHelper = new MoodNoteDBHelper(context);
    }

    public void addTask(MoodNoteDBHelper.Task task) {
        dbHelper.insertTask(task);
    }

    public List<MoodNoteDBHelper.Task> getTasks(String date) {
        return dbHelper.getTasksByDate(date);
    }

    public void insertTask(String date, String text, String time, boolean isChecked) {
        MoodNoteDBHelper.Task task = new MoodNoteDBHelper.Task(0, date, text, time, isChecked);
        dbHelper.insertTask(task);
    }

    public void updateTaskById(int id, String newText, String newTime, boolean isChecked) {
        dbHelper.updateTaskById(id, newText, newTime, isChecked);
    }

    public void deleteTaskById(int id) {
        dbHelper.deleteTaskById(id);
    }

    public List<MoodNoteDBHelper.Task> getTasksForMonth(int year, int month) {
        return dbHelper.getTasksForMonth(year, month);
    }

}
