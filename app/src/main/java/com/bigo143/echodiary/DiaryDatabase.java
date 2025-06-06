package com.bigo143.echodiary;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {DiaryEntry.class}, version = 4)
// Version 2 and 3 has the ones without Id
public abstract class DiaryDatabase extends RoomDatabase {
    private static DiaryDatabase instance;

    public abstract DiaryDao diaryDao();

    public static synchronized DiaryDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            DiaryDatabase.class, "diary_database")
                    .build();
        }
        return instance;
    }
}
