package com.comp90015;

import com.comp90015.base.Packet;
import com.google.gson.Gson;
//import message.ClientMessage;
//import message.ServerMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

public class ServerConn extends Thread {

    private Server server;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connectionAlive = false;

    private String identity;
    private String roomid;

    // JSON Parser
    private final Gson gson = new Gson();

    public ServerConn(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    /*
     * Run method keeps listening on the reader of the socket for the message sent from the client
     * */
    public void run() {

        // a new guest is connected
        // make a new thread and assign a new identity
        String newId = UUID.randomUUID().toString();
        this.identity = newId;

        // the server sends a NewIdentity message to the new client
//        ServerMessage serverMessage = new ServerMessage.ServerMessageBuilder("newidentity")
//                .former("")
//                .identity(newId)
//                .build();
//        sendMessage(gson.toJson(serverMessage));

        Packet.NewIdentity serverMessage = new Packet.NewIdentity("", newId);

//        ServerMessage serverMessage1 = new ServerMessage.ServerMessageBuilder("roomchange")
//                .identity(newId)
//                .former("")
//                .roomid("MainHall")
//                .build();
//        sendMessage(gson.toJson(serverMessage1));
//
//        String roomList = server.listRooms();
//        serverMessage = new ServerMessage.ServerMessageBuilder("roomlist")
//                .rooms(roomList)
//                .build();
        sendMessage(gson.toJson(serverMessage));

        connectionAlive = true;
        String in;
        while (connectionAlive) {
            try {
                in  = reader.readLine();
                if (in != null) {
                    // TODO: use thread pool to handle parsing
//                    parseJSON(in);
                } else {
                    connectionAlive = false;
                }
            } catch (IOException e) {
                connectionAlive = false;
                System.out.println(e.getMessage());
                close();
            }
        }
        close();
    }

    /*
     * Close the server connection thread
     * */
    public void close() {
        try {
            socket.close();
            reader.close();
            writer.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * Send the message to the client
     * */
    public void sendMessage(String message) {
        writer.println(message);
    }

//    public void parseJSON(String jsonText) {
//        ClientMessage clientMessage = gson.fromJson(jsonText, ClientMessage.class);
//
//        String messageType = clientMessage.getType();
//
//        ServerMessage serverMessage = null;
//
//        switch (messageType) {
//            case "identitychange":
//                String newIdentity = clientMessage.getIdentity();
//                if (server.isValidIdentity(newIdentity)) {
//                    serverMessage = new ServerMessage.ServerMessageBuilder("newidentity")
//                            .former(identity)
//                            .identity(newIdentity)
//                            .build();
//                    server.broadcast(serverMessage, roomid, null);
//                    this.identity = clientMessage.getIdentity();
//                } else {
//                    serverMessage = new ServerMessage.ServerMessageBuilder("newidentity")
//                            .former(identity)
//                            .identity(identity)
//                            .build();
//                    sendMessage(gson.toJson(serverMessage));
//                }
//                break;
//            case "join":
//                String former = this.roomid;
//                boolean success = server.joinRoom(this, clientMessage.getRoomid());
//                if (success) {
//                    this.roomid = clientMessage.getRoomid();
//                }
//                serverMessage = new ServerMessage.ServerMessageBuilder("roomchange")
//                        .identity(clientMessage.getIdentity())
//                        .former(former)
//                        .roomid(this.getRoomid())
//                        .build();
//                sendMessage(gson.toJson(serverMessage));
//                break;
//            case "message":
//                serverMessage = new ServerMessage.ServerMessageBuilder("message")
//                        .identity(identity)
//                        .content(clientMessage.getContent())
//                        .build();
//                server.broadcast(serverMessage, roomid, this);
//                break;
//            case "list":
//                // query server for room list
//                String roomList = server.listRooms();
//                serverMessage = new ServerMessage.ServerMessageBuilder("roomlist")
//                        .rooms(roomList)
//                        .build();
//                sendMessage(gson.toJson(serverMessage));
//                break;
//            case "createroom":
//                String roomid = clientMessage.getRoomid();
//                if (server.isValidRoomid(roomid)) {
//                    server.createRoom(roomid, this.identity);
//                }
//                serverMessage = new ServerMessage.ServerMessageBuilder("roomlist")
//                        .rooms(server.listRooms())
//                        .build();
//                sendMessage(gson.toJson(serverMessage));
//                break;
//        }


//    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getRoomid() {
        return roomid;
    }

    public void setRoomid(String roomid) {
        this.roomid = roomid;
    }
}
