package com.comp90015;

import com.comp90015.base.Constant;

public class ServerEntry {

    public static void main(String[] args) {

        // TODO: Parse command line argument

        Server server = new Server(Constant.PORT);
        server.handle();
    }

}
