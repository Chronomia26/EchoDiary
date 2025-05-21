package com.bigo143.echodiary;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "diary_entries")
public class DiaryEntry {
    @PrimaryKey
    public long id;  // Manually assigned, not auto-generated

    public String title;
    public String subtitle;
    public String content;
    public long timestamp;
}
