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
    
    public int getX() {
        return player.getX();
    }
    
    public int getY() {
        return player.getY();
    }
    
    public void takeDamage(int damage) {
        player.takeDamage(damage);
    }
}
