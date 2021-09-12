package com.comp90015.base;

public class ChatRoom {

    private final String roomid;
    private final int count;

    public ChatRoom(String roomid, int count) {
        this.roomid = roomid;
        this.count = count;
    }

    public String getRoomid() {
        return roomid;
    }

    public int getCount() {
        return count;
    }

}
