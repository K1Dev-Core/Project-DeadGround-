package shared;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public static final int PLAYER_JOIN = 1;
    public static final int PLAYER_LEAVE = 2;
    public static final int PLAYER_UPDATE = 3;
    public static final int BULLET_SPAWN = 4;
    public static final int BOT_UPDATE = 5;
    public static final int BOT_SPAWN = 6;
    public static final int BOT_HIT = 7;
    public static final int PLAYER_HIT = 8;
    public static final int GAME_STATE = 9;
    public static final int PING = 10;
    public static final int PONG = 11;
    public static final int CHICKEN_UPDATE = 12;

    public int type;
    public String playerId;
    public Object data;
    public long timestamp;
    public int sequence;

    public NetworkMessage() {
        this.type = 0;
        this.playerId = "";
        this.data = null;
        this.timestamp = System.currentTimeMillis();
        this.sequence = 0;
    }

    public NetworkMessage(int type, String playerId, Object data) {
        this.type = type;
        this.playerId = playerId;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.sequence = 0;
    }

    public NetworkMessage(int type, String playerId, Object data, int sequence) {
        this.type = type;
        this.playerId = playerId;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.sequence = sequence;
    }
}
