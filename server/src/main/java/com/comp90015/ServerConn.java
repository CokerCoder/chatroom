package com.comp90015;

import com.comp90015.base.ChatRoom;
import com.comp90015.base.Constant;
import com.comp90015.base.Packet;
import com.comp90015.base.RuntimeTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerConn extends Thread {

    private final Server server;
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;

    private String identity;
    private String roomid;

    RuntimeTypeAdapterFactory<Packet.ToServer> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
            .of(Packet.ToServer.class, Constant.TYPE)
            .registerSubtype(Packet.IdentityChange.class, Constant.IDENTITY_CHANGE)
            .registerSubtype(Packet.Join.class, Constant.JOIN)
            .registerSubtype(Packet.Who.class, Constant.WHO)
            .registerSubtype(Packet.List.class, Constant.LIST)
            .registerSubtype(Packet.CreateRoom.class, Constant.CREATE_ROOM)
            .registerSubtype(Packet.Delete.class, Constant.DELETE)
            .registerSubtype(Packet.Quit.class, Constant.QUIT)
            .registerSubtype(Packet.ToSMessage.class, Constant.MESSAGE);

    Gson gson = new GsonBuilder().registerTypeAdapterFactory(runtimeTypeAdapterFactory).create();

    public ServerConn(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new PrintWriter(new OutputStreamWriter
                (socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    @Override
    /*
     * Run method keeps listening on the reader of the socket for the message sent
     * from the client
     */
    public void run() {
        boolean connectionAlive = true;
        String clientMessage;
        while (connectionAlive) {
            try {
                clientMessage = reader.readLine();
                // detect if the client disconnected without sending the quit command
                if (clientMessage == null) {
                    closeConnection();
                }
                parseJSON(clientMessage);
            } catch (IOException e) {
                connectionAlive = false;
                System.err.println(e.getMessage());
                close();
            }
        }
        close();
    }

    /*
     * Close the server connection thread
     */
    public void close() {
        try {
            socket.close();
            reader.close();
            writer.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /*
     * Send the message to the client
     */
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

                // check if there is any room owned by this client
                // if so change the corresponding owner name to the new one
                for (Map.Entry<String, String> entry : server.getOwners().entrySet()) {
                    if (entry.getValue().equals(identity)) {
                        entry.setValue(newIdentity);
                    }
                }

                // if former identity is default ones, remove from the set
                handleGuestId();
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
                // send room change message to all the connected clients in the room
                for (ServerConn serverConn : server.getRooms().get(roomid)) {
                    serverMessage = new Packet.RoomChange(identity, former, roomid);
                    serverConn.sendMessage(gson.toJson(serverMessage));
                }
            } else {
                serverMessage = new Packet.RoomChange(identity, former, roomid);
                sendMessage(gson.toJson(serverMessage));
            }
            // if changing to main hall, send additional information
            if (roomid.equals(Constant.MAINHALL) && !former.equals(roomid)) {
                serverMessage = new Packet.RoomContents(Constant.MAINHALL,
                        server.listGuests(Constant.MAINHALL),
                        "");
                sendMessage(gson.toJson(serverMessage));

                serverMessage = new Packet.RoomList(server.listRooms());
                sendMessage(gson.toJson(serverMessage));
            }
        }

        if (clientMessage instanceof Packet.Who) {
            Packet.Who whoMessage = (Packet.Who) clientMessage;
            String guests = server.listGuests(whoMessage.getRoomid());
            if (guests.length() == 0)
                return;
            serverMessage = new Packet.RoomContents(whoMessage.getRoomid(), guests,
                    server.getOwners().get(whoMessage.getRoomid()));
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
                for (Map.Entry<String, List<ServerConn>> entry : server.getRooms().entrySet()) {
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
            closeConnection();
        }

        if (clientMessage instanceof Packet.Delete) {
            Packet.Delete deleteMessage = (Packet.Delete) clientMessage;
            // only delete the room if the request client is the owner
            String toDelete = deleteMessage.getRoomid();
            if (!server.getOwners().get(toDelete).equals(identity)) {
                return;
            }
            // join each guest in the given room to MainHall
            for (ServerConn guest : server.getRooms().get(toDelete)) {
                guest.setRoomid(Constant.MAINHALL);
                serverMessage = new Packet.RoomChange(guest.getIdentity(), toDelete, Constant.MAINHALL);
                guest.sendMessage(gson.toJson(serverMessage));
                server.joinRoom(guest, Constant.MAINHALL);
            }

            // remove the room
            server.getRooms().remove(toDelete);

            serverMessage = new Packet.RoomList(server.listRooms());
            sendMessage(gson.toJson(serverMessage));
        }
    }

    private void handleGuestId() {
        if (identity.startsWith(Constant.GUEST)) {
            if (identity.length() > Constant.GUEST.length()) {
                String tail = identity.substring(Constant.GUEST.length());
                try {
                    int identityInt = Integer.parseInt(tail);
                    server.getIds().remove(identityInt);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    private void closeConnection() {
        Packet.ToClient serverMessage;
        serverMessage = new Packet.RoomChange(identity, roomid, "");
        server.broadcast(serverMessage, roomid, null);
        server.quit(roomid, this);
        // check if there is any room owned by this client
        // if so change the corresponding owner name to empty string
        for (Map.Entry<String, String> entry : server.getOwners().entrySet()) {
            if (entry.getValue().equals(identity)) {
                entry.setValue("");
            }
        }
        // if former identity is default ones, remove from the set
        handleGuestId();
        close();
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
