package com.comp90015;

import com.comp90015.base.ChatRoom;
import com.comp90015.base.Constant;
import com.comp90015.base.Packet;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;


/**
 * The Server side for the client-server architecture chatroom
 * @author Yunfei Jing
 * @version 1.0.0
 */
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

    public Set<Integer> getIds() {
        return ids;
    }

    // Concurrent hashset to store the ids
    private final Set<Integer> ids = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public Server(int port) {
        this.port = port;
        // initialize MainHall
        rooms.put(Constant.MAINHALL, new ArrayList<>());
        owners.put(Constant.MAINHALL, "");
    }

    ExecutorService clientThreadPool = new ThreadPoolExecutor
            (5, Integer.MAX_VALUE, 5,
                    TimeUnit.MINUTES,
                    new SynchronousQueue<>(),
                    new ThreadPoolExecutor.AbortPolicy()
            );

    /**
     * Handle method for listening for connections made by the client,
     * for each connected client make a new thread to handle its request.
     */
    public void handle() {
        alive = true;
        try {
            serverSocket = new ServerSocket(port);
            while (alive) {

                // received new connection and make a new thread for it
                Socket socket = serverSocket.accept();

                ServerConn serverConn = new ServerConn(this, socket);
                clientThreadPool.execute(serverConn);

                // a new guest is connected
                // make a new thread and assign a new identity
                String newId;
                for (int i=1;;i++) {
                    if (!ids.contains(i)) {
                        newId = Constant.GUEST + i;
                        serverConn.setIdentity(newId);
                        ids.add(i);
                        break;
                    }
                }

                // the server sends some initial messages to the client
                Packet.ToClient serverMessage;
                serverMessage = new Packet.NewIdentity("", newId);
                serverConn.sendMessage(gson.toJson(serverMessage));

                // let the guest join Main Hall by default
                joinRoom(serverConn, Constant.MAINHALL);

                // the server send some initial messages to the client
                serverMessage = new Packet.RoomChange(newId, "", Constant.MAINHALL);
                serverConn.sendMessage(gson.toJson(serverMessage));

                serverMessage = new Packet.RoomContents(Constant.MAINHALL, listGuests(Constant.MAINHALL), "");
                serverConn.sendMessage(gson.toJson(serverMessage));

                serverMessage = new Packet.RoomList(listRooms());
                serverConn.sendMessage(gson.toJson(serverMessage));

            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            close();
        }
    }

    /**
     * Close the current server socket.
     */
    private void close() {
        try {
            alive = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            clientThreadPool.shutdown();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Broadcast the message to all the clients in the current room
     * @param toClientMessage message to be broadcast
     * @param roomid room id to be broadcast in
     * @param ignored the client who send the message, should be ignored when broadcast
     */
    public void broadcast(Packet.ToClient toClientMessage, String roomid, ServerConn ignored) {
        for (ServerConn conn : rooms.get(roomid)) {
            if (ignored == null || !ignored.equals(conn))
                conn.sendMessage(gson.toJson(toClientMessage));
        }
    }

    /**
     * List all the current active chat rooms.
     * @return all the rooms with size in JSON format string
     */
    public String listRooms() {
        List<ChatRoom> data = new ArrayList<>();
        for (Map.Entry<String, List<ServerConn>> entry: rooms.entrySet()) {
            ChatRoom room = new ChatRoom(entry.getKey(), entry.getValue().size());
            data.add(room);
        }
        return gson.toJson(data);
    }

    /**
     * List all the guests connected in the given room.
     * @param roomid room id to be look up
     * @return all the guests in the given room in JSON format string
     */
    public String listGuests(String roomid) {
        if (!rooms.containsKey(roomid)) return "";
        List<String> data = new ArrayList<>();
        for (ServerConn serverConn: rooms.get(roomid)) {
            data.add(serverConn.getIdentity());
        }
        return gson.toJson(data);
    }

    /**
     * Let the given guest join the given room.
     * @param guest guest to be joined
     * @param roomid room id to be joined
     * @return whether the guest joined the room successfully
     */
    public boolean joinRoom(ServerConn guest, String roomid) {

        if (!rooms.containsKey(roomid)) return false;

        if (guest.getRoomid() != null) {
            // remove guest from the previous room
            List<ServerConn> former = rooms.get(guest.getRoomid());
            former.remove(guest);
        }

        List<ServerConn> guests = rooms.get(roomid);
        guests.add(guest);

        rooms.put(roomid, guests);
        guest.setRoomid(roomid);

        return true;
    }

    /**
     * Validate if a string is valid for an identity.
     * @param newIdentity given identity for checking
     * @return valid or not
     */
    public boolean isValidIdentity(String newIdentity) {

        Pattern p = Pattern.compile("[^a-zA-Z0-9]");
        boolean hasSpecialChar = p.matcher(newIdentity).find();
        if (hasSpecialChar ||
                newIdentity.length() > Constant.MAX_IDENTITY_LENGTH ||
                newIdentity.length() < Constant.MIN_NAME_LENGTH)
            return false;
        for (var entry : rooms.entrySet()) {
            for (ServerConn serverConn : entry.getValue()) {
                if (serverConn.getIdentity().equals(newIdentity)) {
                    return false;
                }
            }
        }

        // make sure new identity cannot be the same as "guestx"
        if (newIdentity.startsWith(Constant.GUEST)) {
            if (newIdentity.length() > Constant.GUEST.length()) {
                String tail = newIdentity.substring(Constant.GUEST.length());
                try {
                    int newIdentityInt = Integer.parseInt(tail);
                    if (ids.contains(newIdentityInt)) {
                        return false;
                    } else {
                        // if changing to guestx
                        ids.add(newIdentityInt);
                    }
                } catch (NumberFormatException ignored) { }
            }
        }
        return true;
    }

    /**
     * Validate if a string is valid for a room id.
     * @param newRoomid given room id for checking
     * @return valid or not
     */
    public boolean isValidRoomid(String newRoomid) {
        Pattern p = Pattern.compile("[^a-zA-Z0-9]");
        boolean hasSpecialChar = p.matcher(newRoomid).find();
        if (hasSpecialChar ||
                newRoomid.length() > Constant.MAX_ROOM_LENGTH ||
                newRoomid.length() < Constant.MIN_NAME_LENGTH)
            return false;
        for (var entry : rooms.entrySet()) {
            if (newRoomid.equals(entry.getKey())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create a room
     * @param roomid room id to be created
     * @param owner the name of the creator
     */
    public void createRoom(String roomid, String owner) {
        rooms.put(roomid, new ArrayList<>());
        owners.put(roomid, owner);
    }

    /**
     * Disconnect a guest from the server.
     * If the room is empty when the owner disconnects, remove the room.
     * @param roomid current room the guest is in
     * @param guest guest to be removed
     */
    public void quit(String roomid, ServerConn guest) {
        rooms.get(roomid).remove(guest);
        for (Map.Entry<String, String> entry: owners.entrySet()) {
            if (entry.getValue().equals(guest.getIdentity())) {
                entry.setValue("");
                if (rooms.get(entry.getKey()).size() == 0) {
                    rooms.remove(entry.getKey());
                    owners.remove(entry.getKey());
                }
            }
        }
    }
}
