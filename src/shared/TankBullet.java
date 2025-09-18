package shared;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

public class TankBullet {
    public double x, y;
    public double dx, dy;
    public double speed = 8;
    public boolean active = true;
    
    private BufferedImage shotImage;
    
    public TankBullet(double x, double y, double angle) {
        this.x = x;
        this.y = y;
        this.dx = Math.cos(angle) * speed;
        this.dy = Math.sin(angle) * speed;
        
        try {
            shotImage = ImageIO.read(new File("assets/enemy/tank/shotOrange.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean update(List<Rectangle2D.Double> collisions, int mapW, int mapH) {
        if (!active) return false;
        
        x += dx;
        y += dy;
        
        if (x < 0 || x > mapW || y < 0 || y > mapH) {
            active = false;
            return false;
        }
        
        Rectangle2D.Double bulletRect = new Rectangle2D.Double(x - 4, y - 4, 8, 8);
        for (Rectangle2D.Double collision : collisions) {
            if (bulletRect.intersects(collision)) {
                active = false;
                return false;
            }
        }
        
        return true;
    }
    
    public void draw(Graphics2D g2, int camX, int camY) {
        if (!active || shotImage == null) return;
        
        int drawX = (int) x - camX;
        int drawY = (int) y - camY;
        
        AffineTransform at = new AffineTransform();
        at.translate(drawX + shotImage.getWidth() / 2.0, drawY + shotImage.getHeight() / 2.0);
        at.rotate(Math.atan2(dy, dx));
        at.translate(-shotImage.getWidth() / 2.0, -shotImage.getHeight() / 2.0);
        
        g2.drawImage(shotImage, at, null);
    }
    
    public Rectangle2D.Double bounds() {
        return new Rectangle2D.Double(x - 4, y - 4, 8, 8);
    }
}
