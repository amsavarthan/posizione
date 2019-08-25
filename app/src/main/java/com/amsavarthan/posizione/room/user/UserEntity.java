package com.amsavarthan.posizione.room.user;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user")
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "image")
    private String image;
    @ColumnInfo(name = "unique_id")
    private String unique_id;
    @ColumnInfo(name = "location")
    private String location;
    @ColumnInfo(name = "token")
    private String token;
    @ColumnInfo(name = "device")
    private String device;
    @ColumnInfo(name = "phone")
    private String phone;
    @ColumnInfo(name = "who_can_track")
    private String who_can_track;

    public UserEntity() {
    }

    public UserEntity(int id, String name, String image, String unique_id, String location, String token, String device, String phone, String who_can_track) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.unique_id = unique_id;
        this.location = location;
        this.token = token;
        this.device = device;
        this.phone = phone;
        this.who_can_track = who_can_track;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUnique_id() {
        return unique_id;
    }

    public void setUnique_id(String unique_id) {
        this.unique_id = unique_id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWho_can_track() {
        return who_can_track;
    }

    public void setWho_can_track(String who_can_track) {
        this.who_can_track = who_can_track;
    }
}
