package shared;

import java.io.Serializable;

public class PlayerData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public String id;
    public String name;
    public double x, y;
    public double angle;
    public int hp;
    public int ammo;
    public int kills;
    public boolean shooting;
    public boolean reloading;
    public boolean hasWeapon = false;
    public boolean isGodMode = false;
    public boolean shieldActive = false;
    public String characterType = "hitman1_";
    public long lastUpdate;
    public long deathTime = 0;
    public boolean isDead = false;

    public PlayerData() {
        this.id = "";
        this.name = "";
        this.x = 0;
        this.y = 0;
        this.angle = 0;
        this.hp = 100;
        this.ammo = 0;
        this.kills = 0;
        this.shooting = false;
        this.reloading = false;
        this.lastUpdate = System.currentTimeMillis();
    }

    public PlayerData(String id, String name, double x, double y, String characterType) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.characterType = characterType;
        this.angle = 0;
        this.hp = 100;
        this.ammo = 0;
        this.kills = 0;
        this.shooting = false;
        this.reloading = false;
        this.lastUpdate = System.currentTimeMillis();
    }

    public void update(double x, double y, double angle, int hp, int ammo, int kills, boolean shooting, boolean reloading, boolean isGodMode, boolean shieldActive) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.hp = hp;
        this.ammo = ammo;
        this.kills = kills;
        this.shooting = shooting;
        this.reloading = reloading;
        this.isGodMode = isGodMode;
        this.shieldActive = shieldActive;
        this.lastUpdate = System.currentTimeMillis();
        
        if (hp <= 0 && !isDead) {
            isDead = true;
            deathTime = System.currentTimeMillis();
        } else if (hp > 0 && isDead) {
            isDead = false;
            deathTime = 0;
        }
    }
}
