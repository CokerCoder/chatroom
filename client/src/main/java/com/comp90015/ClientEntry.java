package com.comp90015;

public class ClientEntry {

    public static final String HOST = "localhost";
    public static final int PORT = 4444;

    public static void main(String args[]) {
        Client client = new Client(HOST, PORT);
        client.connect();
    }

}
