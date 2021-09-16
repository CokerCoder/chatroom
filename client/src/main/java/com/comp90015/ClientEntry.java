package com.comp90015;

import com.comp90015.base.Constant;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

public class ClientEntry {

    static String HOST = Constant.HOST;
    static int PORT = Constant.PORT;

    public static void main(String[] args) {

        new CommandLine(new ServerCommandLineParser()).execute(args);

        Client client = new Client(HOST, PORT);
        client.connect();

    }

    @Command(name = "ServerCommandLineParser", mixinStandardHelpOptions = true)
    static class ServerCommandLineParser implements Runnable {

        @Option(names = { "-p", "--port" }, description = "Port Number")
        private int port = Constant.PORT;

        @Parameters(paramLabel = "hostname", description = "Host IP Address", defaultValue = Constant.HOST)
        private String host = Constant.HOST;

        @Override
        public void run() {
            PORT = port;
            HOST = host;
        }
    }

}
