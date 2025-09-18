package shared;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ExplosionEffect {
    public int x, y;
    public int frame = 0;
    public int maxFrames = 3;
    public int frameCounter = 0;
    public int frameDelay = 5;
    public boolean isFinished = false;
    
    private BufferedImage[] explosionImages = new BufferedImage[3];
    
    public ExplosionEffect(int x, int y) {
        this.x = x;
        this.y = y;
        
        try {
            explosionImages[0] = ImageIO.read(new File("assets/enemy/tank/explosionSmoke2.png"));
            explosionImages[1] = ImageIO.read(new File("assets/enemy/tank/explosionSmoke3.png"));
            explosionImages[2] = ImageIO.read(new File("assets/enemy/tank/explosionSmoke4.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void update() {
        frameCounter++;
        if (frameCounter >= frameDelay) {
            frameCounter = 0;
            frame++;
            if (frame >= maxFrames) {
                isFinished = true;
            }
        }
    }
    
    public void draw(Graphics2D g2, int camX, int camY) {
        if (isFinished || explosionImages[frame] == null) return;
        
        int drawX = x - camX - explosionImages[frame].getWidth() / 2;
        int drawY = y - camY - explosionImages[frame].getHeight() / 2;
        
        g2.drawImage(explosionImages[frame], drawX, drawY, null);
    }
}
