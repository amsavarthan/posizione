package com.amsavarthan.posizione.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import java.util.Map;

public class Tracker {

    private String uid;
    private Long timestamp;

    public Tracker() {
    }

    public Map<String,String> getTimestamp(){
        return ServerValue.TIMESTAMP;
    }

    @Exclude
    public Long getTimestampAsLong() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
