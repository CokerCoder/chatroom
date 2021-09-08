package com.comp90015;

import com.comp90015.base.Packet;
import com.comp90015.base.RuntimeTypeAdapterFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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

    RuntimeTypeAdapterFactory<Packet.ToServer> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
            .of(Packet.ToServer.class, "type")
            .registerSubtype(Packet.IdentityChange.class, "identitychange")
            .registerSubtype(Packet.Join.class, "join")
            .registerSubtype(Packet.Who.class, "who")
            .registerSubtype(Packet.List.class, "list")
            .registerSubtype(Packet.CreateRoom.class, "createroom")
            .registerSubtype(Packet.Delete.class, "delete")
            .registerSubtype(Packet.Quit.class, "quit")
            .registerSubtype(Packet.ToSMessage.class, "message");

    Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(runtimeTypeAdapterFactory)
            .create();

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

        // the server sends some initial messages to the client
        Packet.ToClient serverMessage;
        serverMessage = new Packet.NewIdentity("", newId);
        sendMessage(gson.toJson(serverMessage));

        serverMessage = new Packet.RoomChange(newId, "", "MainHall");
        sendMessage(gson.toJson(serverMessage));

        serverMessage = new Packet.RoomList(server.listRooms());
        sendMessage(gson.toJson(serverMessage));

        connectionAlive = true;
        String in;
        while (connectionAlive) {
            try {
                in  = reader.readLine();
                if (in != null) {
                    parseJSON(in);
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

    public void parseJSON(String jsonText) {

        Packet.ToServer clientMessage = gson.fromJson(jsonText, Packet.ToServer.class);

        Packet.ToClient serverMessage = null;

        if (clientMessage instanceof Packet.IdentityChange) {
            Packet.IdentityChange identityChangeMessage = (Packet.IdentityChange) clientMessage;
            String newIdentity = identityChangeMessage.getIdentity();
            if (server.isValidIdentity(newIdentity)) {
                serverMessage = new Packet.NewIdentity(identity, newIdentity);
                server.broadcast(serverMessage, roomid, null);
                this.identity = identityChangeMessage.getIdentity();
            } else {
                serverMessage = new Packet.NewIdentity(identity, identity);
                sendMessage(gson.toJson(serverMessage));
            }
        }

        if (clientMessage instanceof Packet.Join) {
            Packet.Join joinMessage = (Packet.Join) clientMessage;
            String former = this.roomid;
            boolean success = server.joinRoom(this, joinMessage.getRoomid());
            if (success) {
                this.roomid = joinMessage.getRoomid();
            }
            serverMessage = new Packet.RoomChange(identity, former, roomid);
            sendMessage(gson.toJson(serverMessage));
        }

        if (clientMessage instanceof Packet.Who) {
            Packet.Who whoMessage = (Packet.Who) clientMessage;
            serverMessage = new Packet.RoomContents(whoMessage.getRoomid(),
                    server.listGuests(whoMessage.getRoomid()), "");
            sendMessage(gson.toJson(serverMessage));
        }

        if (clientMessage instanceof Packet.ToSMessage) {
            Packet.ToSMessage toSMessage = (Packet.ToSMessage) clientMessage;
            serverMessage = new Packet.ToCMessage(toSMessage.getContent(), identity);
            server.broadcast(serverMessage, roomid, this);
        }

        if (clientMessage instanceof Packet.List) {
            // query server for room list
            String roomList = server.listRooms();
            serverMessage = new Packet.RoomList(roomList);
            sendMessage(gson.toJson(serverMessage));
        }

        if (clientMessage instanceof Packet.CreateRoom) {
            Packet.CreateRoom createRoomMessage = (Packet.CreateRoom) clientMessage;
            String roomid = createRoomMessage.getRoomid();
            if (server.isValidRoomid(roomid)) {
                server.createRoom(roomid, this.identity);
            }
            serverMessage = new Packet.RoomList(server.listRooms());
            sendMessage(gson.toJson(serverMessage));
        }
    }

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
