package com.bigo143.echodiary;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {EchoEntry.class}, version = 1)
public abstract class EchoDatabase extends RoomDatabase {
    public abstract EchoDao echoDao();

    private static volatile EchoDatabase INSTANCE;

    public static EchoDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (EchoDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    EchoDatabase.class, "echo_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
