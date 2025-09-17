package shared;

import java.io.Serializable;

public class BulletData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public String id;
    public double x, y;
    public double angle;
    public long timestamp;

    public BulletData() {
        this.id = "";
        this.x = 0;
        this.y = 0;
        this.angle = 0;
        this.timestamp = System.currentTimeMillis();
    }

    public BulletData(String id, double x, double y, double angle) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.timestamp = System.currentTimeMillis();
    }
}
