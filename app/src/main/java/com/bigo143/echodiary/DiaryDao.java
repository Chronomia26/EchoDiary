package com.bigo143.echodiary;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DiaryDao {
    @Insert
    long insert(DiaryEntry entry);

    @Query("SELECT * FROM diary_entries ORDER BY timestamp DESC")
    LiveData<List<DiaryEntry>> getAllEntries();

    //
    @Update
    void update(DiaryEntry entry);
    @Query("DELETE FROM diary_entries WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM diary_entries WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    List<DiaryEntry> getEntriesForDay(long start, long end);
}
