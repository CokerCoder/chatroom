package com.comp90015;

import com.comp90015.base.ChatRoom;
import com.comp90015.base.Packet;
import com.comp90015.base.RuntimeTypeAdapterFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;


/*
 * This class is responsible for reading input from the server socket
 * */
public class ClientConn extends Thread {

    RuntimeTypeAdapterFactory<Packet.ToClient> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
            .of(Packet.ToClient.class, "type")
            .registerSubtype(Packet.NewIdentity.class, "newidentity")
            .registerSubtype(Packet.RoomChange.class, "roomchange")
            .registerSubtype(Packet.RoomContents.class, "roomcontents")
            .registerSubtype(Packet.RoomList.class, "roomlist")
            .registerSubtype(Packet.ToCMessage.class, "message");

    Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(runtimeTypeAdapterFactory)
            .create();

    private Client client;
    private Socket socket;

    private BufferedReader reader; // read from server

    private boolean connectionAlive = false;

    public ClientConn(Client client, Socket socket) throws IOException {
        this.client = client;
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run() {
        connectionAlive = true;
        String serverMsg;
        while (connectionAlive) {
            try {
                serverMsg  = reader.readLine();
                if (serverMsg != null) {
                    parseJSON(serverMsg);
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


    public void parseJSON(String serverMsg) {

        Packet.ToClient serverMessage = gson.fromJson(serverMsg, Packet.ToClient.class);

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
                client.setRoomid(roomChangeMessage.getRoomid());
                if (roomChangeMessage.getFormer().length() > 0) {
                    System.out.format("%s moved from %s to %s\n",
                            client.getIdentity(),
                            roomChangeMessage.getFormer(),
                            roomChangeMessage.getRoomid());
                }
            } else {
                System.out.println("The requested room is invalid or non existent.");
            }
        }

        if (serverMessage instanceof Packet.RoomContents) {
            System.out.println("received roomcontent message");
            Packet.RoomContents roomContentsMessage = (Packet.RoomContents) serverMessage;
            String[] data = gson.fromJson(roomContentsMessage.getIdentities(), String[].class);

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
            for (ChatRoom chatRoom : data) {
                System.out.format("%s: %d guests\n", chatRoom.getRoomid(), chatRoom.getCount());
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
            System.out.println(e.getMessage());
        }
    }

}
