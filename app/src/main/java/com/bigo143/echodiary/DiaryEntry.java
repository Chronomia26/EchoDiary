package com.bigo143.echodiary;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "diary_entries")
public class DiaryEntry {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public String subtitle;
    public String content;
    public long timestamp;

    @ColumnInfo(name = "is_rewritten")
    public boolean isRewritten = false; // ✅ New column to track AI rewrites

    public String originalContent;
}
