package shared;

import java.io.Serializable;

public class PlayerData implements Serializable {
    public String id;
    public String name;
    public double x, y;
    public double angle;
    public int hp;
    public int ammo;
    public boolean shooting;
    public boolean reloading;
    public long lastUpdate;

    public PlayerData(String id, String name, double x, double y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.angle = 0;
        this.hp = 100;
        this.ammo = 30;
        this.shooting = false;
        this.reloading = false;
        this.lastUpdate = System.currentTimeMillis();
    }

    public void update(double x, double y, double angle, int hp, int ammo, boolean shooting, boolean reloading) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.hp = hp;
        this.ammo = ammo;
        this.shooting = shooting;
        this.reloading = reloading;
        this.lastUpdate = System.currentTimeMillis();
    }
}
