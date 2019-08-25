package com.amsavarthan.posizione.models;

public class User {

    private String name,image,unique_id,token,location,device,phone,who_can_track;

    public User() {}

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

    public void setUnique_id(String unique_id) { this.unique_id = unique_id; }

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
