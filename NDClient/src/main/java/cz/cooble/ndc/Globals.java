package cz.cooble.ndc;

public class Globals {

    public static final int MAX_PLAYER_NAME_LENGTH = 11;
    public static final int MIN_PLAYER_NAME_LENGTH = 3;
    public static final int LOBBY_PORT = 1234;

    public static final boolean DISABLE_MOVE = false;
    public static final long SERVER_TIMEOUT = DISABLE_MOVE ? 10000000 : 5000;
    public static final long CONNECT_TIMEOUT = 5000;
}
