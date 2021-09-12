package com.comp90015.base;

public class Constant {

    public static final String HOST = "localhost";
    public static final int PORT = 4444;

    public static final String MAINHALL = "MainHall";
    public static final String GUEST = "guest";

    public static final Integer MIN_NAME_LENGTH = 3;
    public static final Integer MAX_IDENTITY_LENGTH = 16;
    public static final Integer MAX_ROOM_LENGTH = 32;

    public static final String TYPE = "type";
    public static final String MESSAGE = "message";

    // C2S
    public static final String IDENTITY_CHANGE = "identitychange";
    public static final String JOIN = "join";
    public static final String WHO = "who";
    public static final String LIST = "list";
    public static final String CREATE_ROOM = "createroom";
    public static final String DELETE = "delete";
    public static final String QUIT = "quit";

    // S2C
    public static final String NEW_IDENTITY = "newidentity";
    public static final String ROOM_CHANGE = "roomchange";
    public static final String ROOM_CONTENTS = "roomcontents";
    public static final String ROOM_LIST = "roomlist";

}
