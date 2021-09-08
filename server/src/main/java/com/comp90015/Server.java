package com.comp90015;

import com.comp90015.base.ChatRoom;

import com.comp90015.base.Packet;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class Server {

    private final int port;

    private boolean alive;
    private ServerSocket serverSocket;

    // JSON Parser
    private final Gson gson = new Gson();

    private List<ChatRoom> chatRooms = new ArrayList<>();

    // room list with room id with key and initialize with a empty "Main Hall"
    private Map<String, List<ServerConn>> rooms = new HashMap<String, List<ServerConn>>() {{
        put("MainHall", new ArrayList<ServerConn>());
    }};

    // room owner list with room id with key and initialize with a empty "Main Hall"
    private Map<String, String> owners = new HashMap<>() {{
        put("MainHall", "");
    }};

    // TODO: number list keep track of the least unused number


    public Server(int port) {
        this.port = port;
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
            System.out.println("com.comp90015.Server has started, listening on port " + port + "...");
            while (alive) {

                Socket socket = serverSocket.accept();

                ServerConn serverConn = new ServerConn(this, socket);
                serverConn.start();

                // let the guest join Main Hall by default
                joinRoom(serverConn, "MainHall");
                System.out.println("all rooms: " + listRooms());

            }
        } catch (IOException e) {
            System.out.println("Error creating socket...");
            e.printStackTrace();
        } finally {
            System.out.println("com.comp90015.Server has stopped...");
            close();
        }
    }

    public void broadcast(Packet.ToClient toClientMessage, String roomid, ServerConn ignored) {
        synchronized (rooms) {
            for (com.comp90015.ServerConn conn : rooms.get(roomid)) {
                if (ignored == null || !ignored.equals(conn))
                    conn.sendMessage(gson.toJson(toClientMessage));
            }
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
     * Let the given guest join the given room, synchronize operation
     * */
    public synchronized boolean joinRoom(ServerConn guest, String roomid) {
        boolean exist = false;
        for (var entry : rooms.entrySet()) {
            if (roomid.equals(entry.getKey())) {
                exist = true;
            }
        }
        if (!exist) return false;

        if (guest.getRoomid()!=null) {
            // remove guest from the previous room
            List<ServerConn> former = rooms.get(guest.getRoomid());
            former.remove(guest);
        }

        List<ServerConn> guests = rooms.get(roomid);
        guests.add(guest);

        rooms.put(roomid, guests);
        guest.setRoomid(roomid);

        // if moving to the MainHall, send room content
        if (roomid == "MainHall") {
            Packet.ToClient serverMessage = new Packet.RoomContents(roomid,
                    listGuests(roomid), "");
            guest.sendMessage(gson.toJson(serverMessage));
        }

        System.out.println("User-" + guest.getIdentity() + " joined chat room: " + roomid);
        return true;
    }

    public synchronized boolean isValidIdentity(String newIdentity) {
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

    public synchronized boolean isValidRoomid(String roomid) {
        if (!isValidName(roomid)) {
            return false;
        }
        for (var entry : rooms.entrySet()) {
            if (roomid == entry.getKey()) {
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
        rooms.put(roomid, new ArrayList<ServerConn>());
    }
}
