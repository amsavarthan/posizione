package com.amsavarthan.posizione.room.user;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = UserEntity.class,exportSchema = false,version = 4)
public abstract class UserDatabase extends RoomDatabase {

    private static final String DB_NAME="user_db";
    private static UserDatabase instance;

    public static synchronized UserDatabase getInstance(Context context){
        if(instance==null){
            instance= Room.databaseBuilder(context.getApplicationContext(),UserDatabase.class,DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public abstract UserDao userDao();

}
