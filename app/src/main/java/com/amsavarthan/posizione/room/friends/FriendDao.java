package com.amsavarthan.posizione.room.friends;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FriendDao {

    @Query("Select * from friends")
    List<FriendEntity> getFriendsList();

    @Query("Select * from friends where unique_id = :id")
    FriendEntity getFriendByUniqueId(String id);

    @Query("Select * from friends where name = :name")
    FriendEntity getFriendByName(String name);

    @Insert
    void addUser(FriendEntity friend);

    @Update
    void updateUser(FriendEntity friend);

    @Delete
    void deleteUser(FriendEntity friend);



}
