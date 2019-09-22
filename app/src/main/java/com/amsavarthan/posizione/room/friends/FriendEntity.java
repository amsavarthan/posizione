package com.amsavarthan.posizione.room.friends;


import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "friends")
public class FriendEntity implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "pic")
    private String pic;
    @ColumnInfo(name = "unique_id")
    private String unique_id;
    @ColumnInfo(name = "location")
    private String location;
    @ColumnInfo(name = "device")
    private String device;
    @ColumnInfo(name = "phone")
    private String phone;
    @ColumnInfo(name = "who_can_track")
    private String who_can_track;
    @Ignore
    private boolean tracking_enabled;

    public FriendEntity() {
    }

    public FriendEntity(int id, String name, String pic, String unique_id, String location, String device, String phone, String who_can_track) {
        this.id = id;
        this.name = name;
        this.pic = pic;
        this.unique_id = unique_id;
        this.location = location;
        this.device = device;
        this.phone = phone;
        this.who_can_track = who_can_track;
    }

    @Ignore
    public FriendEntity(String name, String pic, String unique_id, String location, String device, String phone, String who_can_track,boolean tracking_enabled) {
        this.name = name;
        this.pic = pic;
        this.unique_id = unique_id;
        this.location = location;
        this.device = device;
        this.phone = phone;
        this.who_can_track = who_can_track;
        this.tracking_enabled=tracking_enabled;
    }

    protected FriendEntity(Parcel in) {
        id = in.readInt();
        name = in.readString();
        pic = in.readString();
        unique_id = in.readString();
        location = in.readString();
        device=in.readString();
        phone=in.readString();
        who_can_track=in.readString();
    }

    public static final Creator<FriendEntity> CREATOR = new Creator<FriendEntity>() {
        @Override
        public FriendEntity createFromParcel(Parcel in) {
            return new FriendEntity(in);
        }

        @Override
        public FriendEntity[] newArray(int size) {
            return new FriendEntity[size];
        }
    };

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

    public String getUnique_id() {
        return unique_id;
    }

    public void setUnique_id(String unique_id) {
        this.unique_id = unique_id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(name);
        parcel.writeString(pic);
        parcel.writeString(unique_id);
        parcel.writeString(location);
        parcel.writeString(device);
        parcel.writeString(phone);
        parcel.writeString(who_can_track);
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
