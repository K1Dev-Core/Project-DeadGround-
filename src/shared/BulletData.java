package shared;

import java.io.Serializable;

public class BulletData implements Serializable {
    public String id;
    public double x, y;
    public double angle;
    public long timestamp;

    public BulletData(String id, double x, double y, double angle) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.timestamp = System.currentTimeMillis();
    }
}
