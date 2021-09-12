package com.comp90015;

import com.comp90015.base.Constant;

public class ClientEntry {

    public static void main(String[] args) {

        // TODO: Parse command line argument

        Client client = new Client(Constant.HOST, Constant.PORT);
        client.connect();
    }

}
