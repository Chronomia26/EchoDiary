package com.bigo143.echodiary;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "echo_entries")
public class EchoEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String content;
    public String emotion; // optional
    public String category; // optional
    public long timestamp;
}
