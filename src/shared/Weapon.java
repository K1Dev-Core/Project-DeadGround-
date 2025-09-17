package shared;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Weapon {
    public int id;
    public int x, y;
    public boolean collected = false;
    public BufferedImage image;
    public int animationCounter = 0;
    public int animationSpeed = 8;
    public int currentFrame = 0;
    public int totalFrames = 4;
    
    public Weapon(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        
        try {
            image = ImageIO.read(new File("assets/gun/weapon_gun.png"));
        } catch (Exception e) {
            image = null;
        }
    }
    
    public void update() {
        if (collected) return;
        
        animationCounter++;
        if (animationCounter >= animationSpeed) {
            animationCounter = 0;
            currentFrame = (currentFrame + 1) % totalFrames;
        }
    }
    
    public void draw(Graphics2D g2, int cameraX, int cameraY) {
        if (collected || image == null) return;
        
        int drawX = x - cameraX;
        int drawY = y - cameraY;
        
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        
        int scale = 1;
        int scaledWidth = originalWidth * scale;
        int scaledHeight = originalHeight * scale;
        
        g2.setColor(new Color(0, 255, 0, 100));
        g2.fillOval(drawX - 10, drawY - 10, scaledWidth + 20, scaledHeight + 20);
        
        g2.setColor(Color.GREEN);
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(drawX - 10, drawY - 10, scaledWidth + 20, scaledHeight + 20);
        
        g2.drawImage(image, drawX, drawY, drawX + scaledWidth, drawY + scaledHeight, 
                     0, 0, originalWidth, originalHeight, null);
    }
    
    public Rectangle2D.Double bounds() {
        if (image == null) return new Rectangle2D.Double(x, y, 32, 32);
        
        int scale = 1;
        int scaledWidth = image.getWidth() * scale;
        int scaledHeight = image.getHeight() * scale;
        
        return new Rectangle2D.Double(x, y, scaledWidth, scaledHeight);
    }
}
