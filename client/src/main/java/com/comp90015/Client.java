package com.comp90015;

import java.io.IOException;
import java.net.Socket;


public class Client {

    private final String host;
    private final int port;

    // make sure the ClientConsole can read any update made by ClientConn
    private volatile String roomid;
    private volatile String identity;

    // this field is to check if a room is successfully created by the server
    private volatile String creatingRoom = null;

    // this field is to check if the client is asking for leaving the server
    private volatile boolean quitting = false;


    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() {
        try {
            Socket socket = new Socket(host, port);
            /*
             * Make two thread for each connected socket,
             * one is listing message from the server
             * and one is listening console input from the end user
             * */
            Thread clientConn = new ClientConn(this, socket);
            Thread clientConsole = new ClientConsole(this, socket);
            clientConsole.start();
            clientConn.start();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public String getHost() {
        return host;
    }

    public String getRoomid() {
        return roomid;
    }

    public void setRoomid(String roomid) {
        this.roomid = roomid;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getCreatingRoom() {
        return creatingRoom;
    }

    public void setCreatingRoom(String creatingRoom) {
        this.creatingRoom = creatingRoom;
    }

    public boolean isQuitting() {
        return quitting;
    }

    public void setQuitting(boolean quitting) {
        this.quitting = quitting;
    }
}
