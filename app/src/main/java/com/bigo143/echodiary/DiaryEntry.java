package com.bigo143.echodiary;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "diary_entries")
public class DiaryEntry {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String title;
    public String subtitle;
    public String content;
    public long timestamp;
}
