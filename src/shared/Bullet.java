package shared;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class Bullet {
    public double x, y;
    public double dx, dy;
    public BufferedImage img;
    public boolean justSpawned = true;
    public double startX, startY;
    public double distanceTraveled = 0;

    public Bullet(double x, double y, double angle, BufferedImage img) {
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.img = img;
        this.dx = Math.cos(angle) * Config.BULLET_SPEED;
        this.dy = Math.sin(angle) * Config.BULLET_SPEED;
        this.distanceTraveled = 0;
    }

    public boolean update(java.util.List<Rectangle2D.Double> collisions, int mapW, int mapH) {
        x += dx;
        y += dy;
        
        double deltaX = x - startX;
        double deltaY = y - startY;
        distanceTraveled = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        
        if (distanceTraveled > Config.BULLET_RANGE) {
            return false;
        }
        
        Rectangle2D.Double r = new Rectangle2D.Double(x, y, 4, 4);
        for (Rectangle2D.Double c : collisions)
            if (c.intersects(r))
                return false;
        return !(x < -64 || y < -64 || x > mapW + 64 || y > mapH + 64);
    }

    public Rectangle2D.Double bounds() {
        return new Rectangle2D.Double(x, y, 4, 4);
    }

    public void draw(Graphics2D g2, int camX, int camY) {
        g2.setColor(Color.BLACK);
        g2.fillOval((int) (x - camX), (int) (y - camY), 4, 4);
    }
    
    public BulletData toBulletData() {
        return new BulletData("bullet_" + System.currentTimeMillis(), x, y, Math.atan2(dy, dx));
    }
}
