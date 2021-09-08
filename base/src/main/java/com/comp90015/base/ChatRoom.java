package com.comp90015.base;

public class ChatRoom {

    private String roomid;
    private int count;

    private String owner;

    public ChatRoom(String roomid, int count) {
        this.roomid = roomid;
        this.count = count;
//        this.owner = owner;
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
}
