package com.comp90015;

import com.comp90015.base.Constant;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;


public class ServerEntry {

    static int PORT = Constant.PORT;

    public static void main(String[] args) {

//        new CommandLine(new ServerCommandLineParser()).execute(args);

        Server server = new Server(PORT);
        server.handle();

    }

//    @Command(name = "ServerCommandLineParser", mixinStandardHelpOptions = true)
//    static class ServerCommandLineParser implements Runnable {
//
//        @Option(names = { "-p", "--port" }, description = "Port Number")
//        int port = Constant.PORT;
//
//        @Override
//        public void run() {
//            PORT = port;
//        }
//    }
}
