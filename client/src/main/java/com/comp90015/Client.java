package com.comp90015;

import java.io.IOException;
import java.net.Socket;

public class Client {

    private String host;
    private int port;

    // make sure the ClientConsole can read any update made by ClientConn
    private volatile String roomid;
    private volatile String identity;

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
            System.out.println(e.getMessage());
        }

    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
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
}
