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
    // formerly void, 2 E

    @Query("SELECT * FROM diary_entries ORDER BY timestamp DESC")
    LiveData<List<DiaryEntry>> getAllEntries();

    // 4
    @Update
    void update(DiaryEntry entry);
    @Query("DELETE FROM diary_entries WHERE id = :id")
    void deleteById(int id);

}
