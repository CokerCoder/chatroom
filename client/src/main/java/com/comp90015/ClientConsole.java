package com.comp90015;

import com.comp90015.base.Packet;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


/*
 * This class is responsible for reading input from the client console
 * and wrap it with JSON and sent it to the server
 * This class is different to the ClientConn class which is responsible for reading server's message
 * */
public class ClientConsole extends Thread {

    // JSON Parser
    // No need to deserialize JSON string in this class, only serialize
    private final Gson gson = new Gson();

    private Client client;
    private Socket socket;

    // write to the socket
    private BufferedReader reader; // read from console
    private PrintWriter writer; // write to the server

    private boolean connectionAlive = false;

    public ClientConsole(Client client, Socket socket) throws IOException {
        this.client = client;
        this.socket = socket;
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void run() {
        System.out.println(this.client);
        connectionAlive = true;
        String consoleMsg;

        // wait until has been assigned an identity by the server (handled by the ClientConn class)
        while (true) {
            if (client.getIdentity()!=null) {
                System.out.format("Connected to %s as %s\n", client.getHost(), client.getIdentity());
                break;
            }
        }

        while (connectionAlive) {
            try {
//                synchronized (client) {
//                    // wait the client set up
//                    try {
//                        client.wait();
//                    } catch (Exception e) {
//                        System.out.println(e.getMessage());
//                    }
//                }
//                synchronized (client) {
                // TODO: Concurrency
                Thread.sleep(100);
                System.out.format("[%s] %s> ", client.getRoomid(), client.getIdentity());
//                }
                consoleMsg = reader.readLine();
                if (consoleMsg != null) {
                    parse(consoleMsg);
                } else {
                    connectionAlive = false;
                }
            } catch (IOException | InterruptedException e) {
                connectionAlive = false;
                System.out.println(e.getMessage());
                close();
            }
        }
        close();
    }

    /*
     * Parse the user input from the console
     * */
    public void parse(String consoleMessage) {
        if (consoleMessage.length()==0) return;
        if (consoleMessage.charAt(0) == '#') {
            parseCommand(consoleMessage.substring(1));
        } else {
            parseMessage(consoleMessage);
        }
    }

    public void parseMessage(String text) {
        Packet.ToSMessage clientMessage = new Packet.ToSMessage(text);
        System.out.println(gson.toJson(clientMessage));
        sendMessage(gson.toJson(clientMessage));
    }

    /*
     * If the input starts with a '#' then it's a command to the server
     * */
    public void parseCommand(String command) {

        String[] words = command.split(" ");

        Packet.ToServer toServerMessage = null;

        switch (words[0]) {
            case "identitychange":
                if (words.length > 1) {
                    toServerMessage = new Packet.IdentityChange(words[1]);
                } else {
                    System.out.println("Invalid command");
                }
                break;
            case "join":
                if (words.length > 1) {
                    toServerMessage = new Packet.Join(words[1]);
                } else {
                    System.out.println("Invalid command");
                }
                break;
            case "who":
                if (words.length > 1) {
                    toServerMessage = new Packet.Who(words[1]);
                } else {
                    System.out.println("Invalid command");
                }
                break;
            case "list":
                toServerMessage = new Packet.List();
                break;
            case "createroom":
                if (words.length > 1) {
                    toServerMessage = new Packet.CreateRoom(words[1]);
                } else {
                    System.out.println("Invalid command");
                }
                break;
            case "quit":
                toServerMessage = new Packet.Quit();
                sendMessage(gson.toJson(toServerMessage));
                connectionAlive = false;
                return;
        }
        sendMessage(gson.toJson(toServerMessage));
    }

    /*
     * Send the message to the server
     * */
    private void sendMessage(String message) {
        writer.println(message);
    }

    /*
     * Close the client connection thread
     * */
    private void close() {
        try {
            socket.close();
            reader.close();
            writer.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
