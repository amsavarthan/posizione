package com.amsavarthan.posizione.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import java.util.Map;

public class Request{

    private String data;
    private Long timestamp;

    public Request() {
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
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
}
