package com.amsavarthan.posizione.room.fav;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FavDao {

    @Query("Select * from favourites")
    List<FavEntity> getFavList();

    @Query("Select * from favourites where unique_id = :id")
    FavEntity getFavByUniqueId(String id);

    @Insert
    void addUser(FavEntity favEntity);

    @Update
    void updateUser(FavEntity favEntity);

    @Delete
    void deleteUser(FavEntity favEntity);



}
