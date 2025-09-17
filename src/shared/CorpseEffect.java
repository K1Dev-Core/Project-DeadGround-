package shared;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class CorpseEffect {
    public int x, y;
    public int life;
    public double angle;
    public double velocityX, velocityY;
    public double gravity = 0.3;
    public boolean isFloating = true;
    public String playerName;
    private BufferedImage corpseImage;
    
    public CorpseEffect(int x, int y, String playerName) {
        this.x = x;
        this.y = y;
        this.life = Integer.MAX_VALUE;
        this.angle = Math.random() * Math.PI * 2;
        this.velocityX = (Math.random() - 0.5) * 2;
        this.velocityY = -Math.random() * 2 - 1;
        this.playerName = playerName;
        
        try {
            corpseImage = ImageIO.read(new File("assets/player/tile_320.png"));
        } catch (Exception e) {
            corpseImage = null;
        }
    }
    
    public boolean update() {
        if (isFloating) {
            velocityY += gravity;
            x += velocityX;
            y += velocityY;
            angle += 0.02;
            if (velocityY > 0) {
                isFloating = false;
            }
        }
        return true;
    }
    
    public void draw(Graphics2D g2, int camX, int camY) {
        int screenX = x - camX;
        int screenY = y - camY;
        
        if (screenX < -100 || screenY < -100 || screenX > 1000 || screenY > 800)
            return;
        
        Graphics2D g2d = (Graphics2D) g2.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (corpseImage != null) {
            g2d.rotate(angle, screenX, screenY);
            g2d.drawImage(corpseImage, screenX - 16, screenY - 16, 32, 32, null);
            g2d.rotate(-angle, screenX, screenY);
        } else {
            int alpha = 255;
            g2d.setColor(new Color(100, 0, 0, alpha));
            g2d.fillOval(screenX - 15, screenY - 15, 30, 30);
        }
        
        g2d.setColor(new Color(255, 255, 255, 255));
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        FontMetrics fm = g2d.getFontMetrics();
        int nameX = screenX - fm.stringWidth(playerName) / 2;
        int nameY = screenY - 25;
        g2d.drawString(playerName, nameX, nameY);
        
        g2d.dispose();
    }
}
