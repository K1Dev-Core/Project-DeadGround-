import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class Bullet {
    double x, y;
    double dx, dy;
    BufferedImage img;

    public Bullet(double x, double y, double angle, BufferedImage img) {
        this.x = x;
        this.y = y;
        this.img = img;
        this.dx = Math.cos(angle) * Config.BULLET_SPEED;
        this.dy = Math.sin(angle) * Config.BULLET_SPEED;
    }

    public boolean update(java.util.List<Rectangle2D.Double> collisions, int mapW, int mapH) {
        x += dx;
        y += dy;
        Rectangle2D.Double r = new Rectangle2D.Double(x, y, img.getWidth(), img.getHeight());
        for (Rectangle2D.Double c : collisions) if (c.intersects(r)) return false;
        return !(x < -64 || y < -64 || x > mapW + 64 || y > mapH + 64);
    }

    public Rectangle2D.Double bounds() {
        return new Rectangle2D.Double(x, y, img.getWidth(), img.getHeight());
    }

    public void draw(Graphics2D g2, int camX, int camY) {
        g2.drawImage(img, (int)(x - camX), (int)(y - camY), null);
    }
}
