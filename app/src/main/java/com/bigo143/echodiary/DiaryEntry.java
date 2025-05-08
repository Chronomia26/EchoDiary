package com.bigo143.echodiary;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "diary_entries")
public class DiaryEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String content;
    public long timestamp;
}
