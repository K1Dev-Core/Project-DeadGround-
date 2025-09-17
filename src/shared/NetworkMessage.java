package shared;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    public static final int PLAYER_JOIN = 1;
    public static final int PLAYER_LEAVE = 2;
    public static final int PLAYER_UPDATE = 3;
    public static final int BULLET_SPAWN = 4;
    public static final int BOT_UPDATE = 5;
    public static final int BOT_SPAWN = 6;
    public static final int BOT_HIT = 7;
    public static final int GAME_STATE = 8;

    public int type;
    public String playerId;
    public Object data;
    public long timestamp;

    public NetworkMessage(int type, String playerId, Object data) {
        this.type = type;
        this.playerId = playerId;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
}
