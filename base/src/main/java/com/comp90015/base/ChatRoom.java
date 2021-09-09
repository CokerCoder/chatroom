package com.comp90015.base;

import java.util.ArrayList;
import java.util.List;

public class ChatRoom {

    private String roomid;
//    private String owner;
    private int count;
//    private List<String> guests;

    public ChatRoom(String roomid, int count) {
        this.roomid = roomid;
//        this.owner = owner;
        this.count = count;
//        this.guests = new ArrayList<>();
    }

    public String getRoomid() {
        return roomid;
    }

    public void setRoomid(String roomid) {
        this.roomid = roomid;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

//    public String getOwner() {
//        return owner;
//    }
//
//    public void setOwner(String owner) {
//        this.owner = owner;
//    }
//
//    public List<String> getGuests() {
//        return guests;
//    }
//
//    public void setGuests(List<String> guests) {
//        this.guests = guests;
//    }
}
