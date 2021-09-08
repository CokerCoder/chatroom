package com.comp90015.base;

public class Packet {

    public static abstract class ToClient {
        protected String type;

        public ToClient(String type) {
            this.type = type;
        }
    }

    public static class NewIdentity extends ToClient {
        private String former;
        private String identity;

        public NewIdentity(String former, String identity) {
            super("newidentity");
            this.former = former;
            this.identity = identity;
        }

        public String getFormer() {
            return former;
        }

        public String getIdentity() {
            return identity;
        }
    }

    public static class ToCMessage extends ToClient {
        private String content;
        private String identity;

        public ToCMessage(String content, String identity) {
            super("message");
            this.content = content;
            this.identity = identity;
        }

        public String getContent() {
            return content;
        }

        public String getIdentity() {
            return identity;
        }
    }

    public static class RoomChange extends ToClient {
        private String identity;
        private String former;
        private String roomid;

        public RoomChange(String identity, String former, String roomid) {
            super("roomchange");
            this.identity = identity;
            this.former = former;
            this.roomid = roomid;
        }

        public String getIdentity() {
            return identity;
        }

        public String getFormer() {
            return former;
        }

        public String getRoomid() {
            return roomid;
        }
    }

    public static class RoomList extends ToClient {
        private String rooms;

        public RoomList(String rooms) {
            super("roomlist");
            this.rooms = rooms;
        }

        public String getRooms() {
            return rooms;
        }
    }

    public static abstract class ToServer {
        protected String type;

        public ToServer(String type) {
            this.type = type;
        }
    }

    public static class IdentityChange extends ToServer {
        private String identity;

        public IdentityChange(String identity) {
            super("identitychange");
            this.identity = identity;
        }
    }

    public static class Join extends ToServer {
        private String roomid;

        public Join(String roomid) {
            super("join");
            this.roomid = roomid;
        }

        public String getRoomid() {
            return roomid;
        }
    }

    public static class List extends ToServer {
        public List() {
            super("list");
        }
    }

    public static class CreateRoom extends ToServer {
        private String roomid;

        public CreateRoom(String roomid) {
            super("createroom");
            this.roomid = roomid;
        }

        public String getRoomid() {
            return roomid;
        }
    }

    public static class Quit extends ToServer {
        public Quit() {
            super("quit");
        }
    }

    public static class ToSMessage extends ToServer {
        private String content;

        public ToSMessage(String content) {
            super("message");
            this.content = content;
        }
    }

}
