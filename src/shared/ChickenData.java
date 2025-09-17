package shared;

import java.io.Serializable;

public class ChickenData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int id;
    public int x, y;
    public int hp;
    public double angle;
    public boolean isMoving;
    public boolean isHit;
    public boolean isIdle;
    public int currentFrame;
    public int respawnTimer;
    
    public ChickenData() {
        this.id = 0;
        this.x = 0;
        this.y = 0;
        this.hp = Config.CHICKEN_HP;
        this.angle = 0;
        this.isMoving = false;
        this.isHit = false;
        this.isIdle = true;
        this.currentFrame = 0;
        this.respawnTimer = Config.CHICKEN_RESPAWN_TIME * 60;
    }
    
    public ChickenData(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.hp = Config.CHICKEN_HP;
        this.angle = Math.random() * Math.PI * 2;
        this.isMoving = false;
        this.isHit = false;
        this.isIdle = true;
        this.currentFrame = 0;
        this.respawnTimer = Config.CHICKEN_RESPAWN_TIME * 60;
    }
}
