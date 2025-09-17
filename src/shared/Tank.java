package shared;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class Tank {
    public int x, y;
    public int hp;
    public double angle = 0;
    public int shootCooldown = 0;
    public boolean isDead = false;
    
    private BufferedImage tankImage;
    private BufferedImage shotImage;
    
    public Tank(int x, int y) {
        this.x = x;
        this.y = y;
        this.hp = Config.TANK_HP;
        
        try {
            tankImage = ImageIO.read(new File("assets/enemy/tank/tank_dark.png"));
            shotImage = ImageIO.read(new File("assets/enemy/tank/shotOrange.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void update(Object target, List<Rectangle2D.Double> collisions) {
        if (isDead || target == null) return;
        
        int targetX = 0, targetY = 0;
        try {
            targetX = (Integer) target.getClass().getField("x").get(target);
            targetY = (Integer) target.getClass().getField("y").get(target);
        } catch (Exception e) {
            return;
        }
        
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        angle = Math.atan2(dy, dx);
        
        if (distance <= Config.TANK_RANGE && distance > 50) {
            if (shootCooldown <= 0) {
                shootCooldown = Config.TANK_SHOOT_DELAY;
            }
        }
        
        if (shootCooldown > 0) {
            shootCooldown--;
        }
    }
    
    public void draw(Graphics2D g2, int camX, int camY) {
        if (isDead || tankImage == null) return;
        
        int drawX = x - camX;
        int drawY = y - camY;
        
        g2.setColor(new Color(255, 0, 0, 100));
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(drawX - Config.TANK_RANGE + 32, drawY - Config.TANK_RANGE + 32, Config.TANK_RANGE * 2, Config.TANK_RANGE * 2);
        
        g2.setColor(new Color(255, 0, 0, 50));
        g2.fillOval(drawX - 50 + 32, drawY - 50 + 32, 100, 100);
        
        AffineTransform at = new AffineTransform();
        at.translate(drawX + tankImage.getWidth() / 2.0, drawY + tankImage.getHeight() / 2.0);
        at.rotate(angle);
        at.translate(-tankImage.getWidth() / 2.0, -tankImage.getHeight() / 2.0);
        
        g2.drawImage(tankImage, at, null);
    }
    
    public void drawShot(Graphics2D g2, int camX, int camY, int shotX, int shotY) {
        if (shotImage == null) return;
        
        int drawX = shotX - camX;
        int drawY = shotY - camY;
        
        AffineTransform at = new AffineTransform();
        at.translate(drawX + shotImage.getWidth() / 2.0, drawY + shotImage.getHeight() / 2.0);
        at.rotate(angle);
        at.translate(-shotImage.getWidth() / 2.0, -shotImage.getHeight() / 2.0);
        
        g2.drawImage(shotImage, at, null);
    }
    
    
    public Rectangle2D.Double bounds() {
        if (tankImage == null) return new Rectangle2D.Double(x, y, 32, 32);
        return new Rectangle2D.Double(x, y, tankImage.getWidth(), tankImage.getHeight());
    }
    
    public void takeDamage(int damage) {
        hp -= damage;
        if (hp <= 0) {
            isDead = true;
        }
    }
    
    public boolean canShoot() {
        return shootCooldown <= 0 && !isDead;
    }
    
    public void resetShootCooldown() {
        shootCooldown = Config.TANK_SHOOT_DELAY;
    }
    
    public void playExplosionSound() {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("assets/sfx/explosion.wav"));
            Clip explosionClip = AudioSystem.getClip();
            explosionClip.open(audioInputStream);
            FloatControl gainControl = (FloatControl) explosionClip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(-10.0f);
            explosionClip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
