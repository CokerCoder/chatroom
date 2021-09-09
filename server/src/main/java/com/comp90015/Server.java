package com.comp90015;

import com.comp90015.base.ChatRoom;
import com.comp90015.base.Packet;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


public class Server {

    private final int port;

    private boolean alive;
    private ServerSocket serverSocket;

    // JSON Parser
    private final Gson gson = new Gson();

    public Map<String, List<ServerConn>> getRooms() {
        return rooms;
    }

    private final Map<String, List<ServerConn>> rooms = new ConcurrentHashMap<>();

    public Map<String, String> getOwners() {
        return owners;
    }

    private final Map<String, String> owners = new ConcurrentHashMap<>();


    // TODO: number list keep track of the least unused number


    public Server(int port) {
        this.port = port;
        // initialize MainHall
        rooms.put("MainHall", new ArrayList<>());
        owners.put("MainHall", "");
    }

    /*
     * Handle (listen) for connection
     * For each connected client make a new thread to handle its request
     * TODO: change to thread pool
     * */
    public void handle() {
        alive = true;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server has started, listening on port " + port + "...");
            while (alive) {

                // received new connection and make a new thread for it
                Socket socket = serverSocket.accept();

                ServerConn serverConn = new ServerConn(this, socket);
                serverConn.start();

                // a new guest is connected
                // make a new thread and assign a new identity
                String newId = UUID.randomUUID().toString();
                serverConn.setIdentity(newId);

                // the server sends some initial messages to the client
                Packet.ToClient serverMessage;
                serverMessage = new Packet.NewIdentity("", newId);
                serverConn.sendMessage(gson.toJson(serverMessage));

                // let the guest join Main Hall by default
                joinRoom(serverConn, "MainHall");
                System.out.println("all rooms: " + listRooms());

                // the server send some initial messages to the client
                serverMessage = new Packet.RoomChange(newId, "", "MainHall");
                serverConn.sendMessage(gson.toJson(serverMessage));

                serverMessage = new Packet.RoomContents("MainHall", listGuests("MainHall"), "");
                serverConn.sendMessage(gson.toJson(serverMessage));

                serverMessage = new Packet.RoomList(listRooms());
                serverConn.sendMessage(gson.toJson(serverMessage));

            }
        } catch (IOException e) {
            System.out.println("Error creating socket...");
            e.printStackTrace();
        } finally {
            System.out.println("Server has stopped...");
            close();
        }
    }

    /*
     * Close the current server socket
     * */
    private void close() {
        try {
            alive = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void broadcast(Packet.ToClient toClientMessage, String roomid, ServerConn ignored) {
        for (ServerConn conn : rooms.get(roomid)) {
            if (ignored == null || !ignored.equals(conn))
                conn.sendMessage(gson.toJson(toClientMessage));
        }
    }

    /*
     * List all the current active chat rooms
     * */
    public String listRooms() {
        List<ChatRoom> data = new ArrayList<>();
        for (Map.Entry<String, List<ServerConn>> entry: rooms.entrySet()) {
            ChatRoom room = new ChatRoom(entry.getKey(), entry.getValue().size());
            data.add(room);
        }
        return gson.toJson(data);
    }

    /*
    * List all the guests connected in the given room
    * */
    public String listGuests(String roomid) {
        if (!rooms.containsKey(roomid)) return "";
        List<String> data = new ArrayList<>();
        for (ServerConn serverConn: rooms.get(roomid)) {
            data.add(serverConn.getIdentity());
        }
        return gson.toJson(data);
    }

    /*
     * Let the given guest join the given room
     * */
    public boolean joinRoom(ServerConn guest, String roomid) {

        if (!rooms.containsKey(roomid)) return false;

        if (guest.getRoomid()!=null) {
            // remove guest from the previous room
            List<ServerConn> former = rooms.get(guest.getRoomid());
            former.remove(guest);
        }

        List<ServerConn> guests = rooms.get(roomid);
        guests.add(guest);

        rooms.put(roomid, guests);
        guest.setRoomid(roomid);

        System.out.println("User-" + guest.getIdentity() + " joined chat room: " + roomid);
        return true;
    }

    public boolean isValidIdentity(String newIdentity) {
        if (!isValidName(newIdentity)) {
            return false;
        }
        for (var entry : rooms.entrySet()) {
            for (ServerConn serverConn : entry.getValue()) {
                if (serverConn.getIdentity().equals(newIdentity)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isValidRoomid(String roomid) {
        if (!isValidName(roomid)) {
            return false;
        }
        for (var entry : rooms.entrySet()) {
            if (roomid.equals(entry.getKey())) {
                return false;
            }
        }
        return true;
    }

    public boolean isValidName(String name) {
        Pattern p = Pattern.compile("[^a-zA-Z0-9]");
        boolean hasSpecialChar = p.matcher(name).find();
        return (!hasSpecialChar && name.length() <= 16 && name.length() >= 3);
    }

    public void createRoom(String roomid, String owner) {
        rooms.put(roomid, new ArrayList<>());
        owners.put(roomid, owner);
    }

    public void quit(String roomid, ServerConn guest) {
        rooms.get(roomid).remove(guest);
        for (Map.Entry<String, String> entry: owners.entrySet()) {
            if (entry.getValue().equals(guest.getIdentity())) {
                entry.setValue("");
            }
        }
    }
}
