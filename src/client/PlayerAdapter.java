package client;

import shared.*;

public class PlayerAdapter implements IPlayer {
    private ClientPlayer player;

    public PlayerAdapter(ClientPlayer player) {
        this.player = player;
    }

    public int getCenterX() {
        return player.getCenterX();
    }

    public int getCenterY() {
        return player.getCenterY();
    }
}
