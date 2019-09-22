package com.amsavarthan.posizione.room.fav;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = FavEntity.class,exportSchema = false,version = 1)
public abstract class FavDatabase extends RoomDatabase {

    private static final String DB_NAME="fav_db";
    private static FavDatabase instance;

    public static synchronized FavDatabase getInstance(Context context){
        if(instance==null){
            instance= Room.databaseBuilder(context.getApplicationContext(), FavDatabase.class,DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public abstract FavDao favDao();

}
