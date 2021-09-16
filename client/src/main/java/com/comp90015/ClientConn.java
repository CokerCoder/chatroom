package com.comp90015;

import com.comp90015.base.ChatRoom;
import com.comp90015.base.Constant;
import com.comp90015.base.Packet;
import com.comp90015.base.RuntimeTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


/*
 * This class is responsible for reading input from the server socket
 * */
public class ClientConn extends Thread {

    RuntimeTypeAdapterFactory<Packet.ToClient> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
            .of(Packet.ToClient.class, Constant.TYPE)
            .registerSubtype(Packet.NewIdentity.class, Constant.NEW_IDENTITY)
            .registerSubtype(Packet.RoomChange.class, Constant.ROOM_CHANGE)
            .registerSubtype(Packet.RoomContents.class, Constant.ROOM_CONTENTS)
            .registerSubtype(Packet.RoomList.class, Constant.ROOM_LIST)
            .registerSubtype(Packet.ToCMessage.class, Constant.MESSAGE);

    Gson gson = new GsonBuilder().registerTypeAdapterFactory(runtimeTypeAdapterFactory).create();

    private final Client client;
    private final Socket socket;

    private final BufferedReader reader; // read from server

    public ClientConn(Client client, Socket socket) throws IOException {
        this.client = client;
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        boolean connectionAlive = true;
        String serverMessage;
        while (connectionAlive) {
            try {
                if (client.isQuitting() || socket.isClosed()) return;
                serverMessage  = reader.readLine();
                if (serverMessage != null) {
                    parseJSON(serverMessage);
                } else {
                    connectionAlive = false;
                }
            } catch (IOException e) {
                connectionAlive = false;
                System.err.println(e.getMessage());
                close();
            }
        }
        close();
    }


    public void parseJSON(String jsonText) {

        Packet.ToClient serverMessage = gson.fromJson(jsonText, Packet.ToClient.class);

        if (serverMessage instanceof Packet.NewIdentity) {
            Packet.NewIdentity newIdentityMessage = (Packet.NewIdentity) serverMessage;
            String former = newIdentityMessage.getFormer();
            String identity = newIdentityMessage.getIdentity();
            if (former.length() == 0) {
                client.setIdentity(identity);
            } else if (!(former.equals(identity))) {
                // Assigned to self
                if (former.equals(client.getIdentity())) {
                    client.setIdentity(identity);
                }
                System.out.format("%s is now %s\n", former, identity);
            } else
                System.out.println("Requested identity invalid or in use");
        }

        if (serverMessage instanceof Packet.ToCMessage) {
            Packet.ToCMessage toCMessage = (Packet.ToCMessage) serverMessage;
            System.out.format("%s: %s\n", toCMessage.getIdentity(), toCMessage.getContent());
        }

        if (serverMessage instanceof Packet.RoomChange) {
            Packet.RoomChange roomChangeMessage = ((Packet.RoomChange) serverMessage);
            if (!roomChangeMessage.getFormer().equals(roomChangeMessage.getRoomid())) {
                if (roomChangeMessage.getIdentity().equals(client.getIdentity())) {
                    client.setRoomid(roomChangeMessage.getRoomid());
                }
                if (roomChangeMessage.getFormer().length() > 0) {
                    if (roomChangeMessage.getRoomid().equals("")) {
                        System.out.format("%s leaves %s\n",
                                client.getIdentity(),
                                roomChangeMessage.getFormer());
                    } else {
                        System.out.format("%s moved from %s to %s\n",
                                roomChangeMessage.getIdentity(),
                                roomChangeMessage.getFormer(),
                                roomChangeMessage.getRoomid());
                    }
                }
            } else {
                System.out.println("The requested room is invalid or non existent.");
            }
        }

        if (serverMessage instanceof Packet.RoomContents) {
            Packet.RoomContents roomContentsMessage = (Packet.RoomContents) serverMessage;
            String[] data = gson.fromJson(roomContentsMessage.getIdentities(), String[].class);

            if (data.length == 0) {
                System.out.format("%s is empty.\n", roomContentsMessage.getRoomid());
                return;
            }

            System.out.format("%s contains", roomContentsMessage.getRoomid());
            for (String guest : data) {
                System.out.print(" " + guest);
                if (guest.equals(roomContentsMessage.getOwner())) {
                    System.out.print("*");
                }
            }
            System.out.print("\n");
        }

        if (serverMessage instanceof Packet.RoomList) {

            Packet.RoomList roomListMessage = (Packet.RoomList) serverMessage;
            ChatRoom[] data = gson.fromJson(roomListMessage.getRooms(), ChatRoom[].class);

            if (client.getCreatingRoom()!=null) {
                // the client requested a room creation
                for (ChatRoom chatRoom : data) {
                    if (chatRoom.getRoomid().equals(client.getCreatingRoom())) {
                        System.out.format("Room %s created.\n", client.getCreatingRoom());
                        client.setCreatingRoom(null);
                        return;
                    }
                }
                System.out.format("Room %s is invalid or already in use.\n", client.getCreatingRoom());
                client.setCreatingRoom(null);
            } else {
                // display the relevant information only when the client isn't requesting creating room
                for (ChatRoom chatRoom : data) {
                    System.out.format("%s: %d guests\n", chatRoom.getRoomid(), chatRoom.getCount());
                }
            }
        }
    }

    /*
     * Close the client connection thread
     * */
    private void close() {
        try {
            socket.close();
            reader.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

}
