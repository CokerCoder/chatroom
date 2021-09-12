package com.comp90015.base;

public class Packet {

    public static abstract class ToClient {
        protected String type;

        public ToClient(String type) {
            this.type = type;
        }
    }

    public static class NewIdentity extends ToClient {
        private final String former;
        private final String identity;

        public NewIdentity(String former, String identity) {
            super(Constant.NEW_IDENTITY);
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

    public static class RoomChange extends ToClient {
        private final String identity;
        private final String former;
        private final String roomid;

        public RoomChange(String identity, String former, String roomid) {
            super(Constant.ROOM_CHANGE);
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

    public static class RoomContents extends ToClient {
        private final String roomid;
        private final String identities;
        private final String owner;

        public RoomContents(String roomid, String identities, String owner) {
            super(Constant.ROOM_CONTENTS);
            this.roomid = roomid;
            this.identities = identities;
            this.owner = owner;
        }

        public String getRoomid() {
            return roomid;
        }

        public String getIdentities() {
            return identities;
        }

        public String getOwner() {
            return owner;
        }
    }

    public static class RoomList extends ToClient {
        private final String rooms;

        public RoomList(String rooms) {
            super(Constant.ROOM_LIST);
            this.rooms = rooms;
        }

        public String getRooms() {
            return rooms;
        }
    }

    public static class ToCMessage extends ToClient {
        private final String content;
        private final String identity;

        public ToCMessage(String content, String identity) {
            super(Constant.MESSAGE);
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



    public static abstract class ToServer {
        protected String type;

        public ToServer(String type) {
            this.type = type;
        }
    }

    public static class IdentityChange extends ToServer {
        private final String identity;

        public IdentityChange(String identity) {
            super(Constant.IDENTITY_CHANGE);
            this.identity = identity;
        }

        public String getIdentity() {
            return identity;
        }
    }

    public static class Join extends ToServer {
        private final String roomid;

        public Join(String roomid) {
            super(Constant.JOIN);
            this.roomid = roomid;
        }

        public String getRoomid() {
            return roomid;
        }
    }

    public static class Who extends ToServer {
        private final String roomid;

        public Who(String roomid) {
            super(Constant.WHO);
            this.roomid = roomid;
        }

        public String getRoomid() {
            return roomid;
        }
    }

    public static class List extends ToServer {
        public List() {
            super(Constant.LIST);
        }
    }

    public static class CreateRoom extends ToServer {
        private final String roomid;

        public CreateRoom(String roomid) {
            super(Constant.CREATE_ROOM);
            this.roomid = roomid;
        }

        public String getRoomid() {
            return roomid;
        }
    }

    public static class Delete extends ToServer {
        private final String roomid;

        public Delete(String roomid) {
            super(Constant.DELETE);
            this.roomid = roomid;
        }

        public String getRoomid() {
            return roomid;
        }
    }

    public static class Quit extends ToServer {
        public Quit() {
            super(Constant.QUIT);
        }
    }

    public static class ToSMessage extends ToServer {
        public String getContent() {
            return content;
        }

        private final String content;

        public ToSMessage(String content) {
            super(Constant.MESSAGE);
            this.content = content;
        }
    }

}
