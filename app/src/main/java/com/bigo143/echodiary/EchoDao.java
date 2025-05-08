package com.bigo143.echodiary;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EchoDao {
    @Insert
    void insert(EchoEntry entry);

    @Query("SELECT * FROM echo_entries ORDER BY timestamp DESC")
    List<EchoEntry> getAllEntries();
}
