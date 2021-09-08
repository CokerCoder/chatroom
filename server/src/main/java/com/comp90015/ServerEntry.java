package com.comp90015;

public class ServerEntry {

    public static final int PORT = 4444;

    public static void main(String args[]) {
        // TODO: Parse command line argument
        Server server = new Server(PORT);
        server.handle();
    }

}
