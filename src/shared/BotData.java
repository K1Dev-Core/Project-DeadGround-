package shared;

import java.io.Serializable;

public class BotData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public String id;
    public double x, y;
    public double angle;
    public int hp;
    public long lastUpdate;

    public BotData() {
        this.id = "";
        this.x = 0;
        this.y = 0;
        this.angle = 0;
        this.hp = Config.BOT_HP;
        this.lastUpdate = System.currentTimeMillis();
    }

    public BotData(String id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.angle = 0;
        this.hp = Config.BOT_HP;
        this.lastUpdate = System.currentTimeMillis();
    }

    public void update(double x, double y, double angle, int hp) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.hp = hp;
        this.lastUpdate = System.currentTimeMillis();
    }
}
