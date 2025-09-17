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
        
        g2.drawImage(image, drawX, drawY, 32, 32, null);
    }
    
    public Rectangle2D.Double bounds() {
        return new Rectangle2D.Double(x, y, 32, 32);
    }
}
