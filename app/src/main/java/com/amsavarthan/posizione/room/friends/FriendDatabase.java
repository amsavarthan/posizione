package com.amsavarthan.posizione.room.friends;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = FriendEntity.class,exportSchema = false,version = 4)
public abstract class FriendDatabase extends RoomDatabase {

    private static final String DB_NAME="friend_db";
    private static FriendDatabase instance;

    public static synchronized FriendDatabase getInstance(Context context){
        if(instance==null){
            instance= Room.databaseBuilder(context.getApplicationContext(), FriendDatabase.class,DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public abstract FriendDao friendDao();

}
