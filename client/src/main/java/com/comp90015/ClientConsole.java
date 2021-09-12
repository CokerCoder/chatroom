package com.comp90015;

import com.comp90015.base.Constant;
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

    private final Client client;
    private final Socket socket;

    // write to the socket
    private final BufferedReader reader; // read from console
    private final PrintWriter writer; // write to the server

    public ClientConsole(Client client, Socket socket) throws IOException {
        this.client = client;
        this.socket = socket;
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void run() {
        System.out.println(this.client);
        boolean connectionAlive = true;
        String consoleMessage;

        // wait until has been assigned an identity by the server (handled by the ClientConn class)
        while (true) {
            if (client.getIdentity()!=null) {
                System.out.format("Connected to %s as %s\n", client.getHost(), client.getIdentity());
                break;
            }
        }

        while (connectionAlive) {
            try {
                if (client.isQuitting()) {
                    return;
                }
                // wait for any update on the "[roomid] identity" update by the ClientConn
                Thread.sleep(10);
                System.out.format("[%s] %s> ", client.getRoomid(), client.getIdentity());
                consoleMessage = reader.readLine();
                if (consoleMessage != null) {
                    parse(consoleMessage);
                } else {
                    connectionAlive = false;
                }
            } catch (IOException e) {
                connectionAlive = false;
                System.err.println(e.getMessage());
                close();
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
                Thread.currentThread().interrupt();
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
        sendMessage(gson.toJson(clientMessage));
    }

    /*
     * If the input starts with a '#' then it's a command to the server
     * */
    public void parseCommand(String command) {

        String[] words = command.split(" ");

        Packet.ToServer toServerMessage = null;

        switch (words[0]) {
            case Constant.IDENTITY_CHANGE:
                if (words.length > 1) {
                    toServerMessage = new Packet.IdentityChange(words[1]);
                } else {
                    System.out.println("Invalid command");
                }
                break;
            case Constant.JOIN:
                if (words.length > 1) {
                    toServerMessage = new Packet.Join(words[1]);
                } else {
                    System.out.println("Invalid command");
                }
                break;
            case Constant.WHO:
                if (words.length > 1) {
                    toServerMessage = new Packet.Who(words[1]);
                } else {
                    System.out.println("Invalid command");
                }
                break;
            case Constant.LIST:
                toServerMessage = new Packet.List();
                break;
            case Constant.CREATE_ROOM:
                if (words.length > 1) {
                    toServerMessage = new Packet.CreateRoom(words[1]);
                    client.setCreatingRoom(words[1]);
                } else {
                    System.out.println("Invalid command");
                }
                break;
            case Constant.QUIT:
                client.setQuitting(true);
                toServerMessage = new Packet.Quit();
                break;
            case Constant.DELETE:
                if (words.length > 1) {
                    toServerMessage = new Packet.Delete(words[1]);
                } else {
                    System.out.println("Invalid command");
                }
                break;
            default:
                System.out.println("Invalid command");
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
            System.err.println(e.getMessage());
        }
    }
}
