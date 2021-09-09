package com.comp90015;

import com.comp90015.base.ChatRoom;
import com.comp90015.base.Packet;
import com.comp90015.base.RuntimeTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ServerConn extends Thread {

    private final Server server;
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
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
        connectionAlive = true;
        String clientMessage;
        while (connectionAlive) {
            try {
                clientMessage  = reader.readLine();
                if (clientMessage != null) {
                    parseJSON(clientMessage);
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

        Packet.ToClient serverMessage;

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
            if (roomid.equals("MainHall")) {
                serverMessage = new Packet.RoomContents("MainHall",
                        server.listGuests("MainHall"),
                        "");
                sendMessage(gson.toJson(serverMessage));
            }
        }

        if (clientMessage instanceof Packet.Who) {
            Packet.Who whoMessage = (Packet.Who) clientMessage;
            String guests = server.listGuests(whoMessage.getRoomid());
            if (guests.length()==0) return;
            serverMessage = new Packet.RoomContents(whoMessage.getRoomid(),
                    guests, server.getOwners().get(whoMessage.getRoomid()));
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
                serverMessage = new Packet.RoomList(server.listRooms());
            } else {
                // if the room is not valid (especially already in use)
                // adopt Luke's suggestion to firstly remove it from the list and then send
                List<ChatRoom> data = new ArrayList<>();
                for (Map.Entry<String, List<ServerConn>> entry: server.getRooms().entrySet()) {
                    if (!entry.getKey().equals(roomid)) {
                        ChatRoom room = new ChatRoom(entry.getKey(), entry.getValue().size());
                        data.add(room);
                    }
                }
                serverMessage = new Packet.RoomList(gson.toJson(data));
            }
            sendMessage(gson.toJson(serverMessage));
        }

        if (clientMessage instanceof Packet.Quit) {
            serverMessage = new Packet.RoomChange(identity, roomid, "");
            server.broadcast(serverMessage, roomid, null);
            server.quit(roomid, this);
            close();
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
