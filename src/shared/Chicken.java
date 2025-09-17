package shared;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class Chicken {
    public int id;
    public int x, y;
    public int hp = Config.CHICKEN_HP;
    public double angle;
    public double velocityX, velocityY;
    public double speed = 0.5;
    public boolean isMoving = false;
    public boolean isHit = false;
    public int hitCooldown = 0;
    public boolean isIdle = true;
    
    private BufferedImage idleSheet, runSheet, hitSheet;
    private int frameWidth = 32;
    private int frameHeight = 34;
    private int idleFrames = 13;
    private int runFrames = 14;
    private int hitFrames = 5;
    public int currentFrame = 0;
    private int animationCounter = 0;
    private int animationSpeed = 8;
    private Clip hitClip;
    
    public Chicken(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.angle = Math.random() * Math.PI * 2;
        
        try {
            idleSheet = ImageIO.read(new File("assets/enemy/Chicken/Idle (32x34).png"));
            runSheet = ImageIO.read(new File("assets/enemy/Chicken/Run (32x34).png"));
            hitSheet = ImageIO.read(new File("assets/enemy/Chicken/Hit (32x34).png"));
        } catch (Exception e) {
            idleSheet = null;
            runSheet = null;
            hitSheet = null;
        }
        
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("assets/sfx/chicken-hit.wav"));
            hitClip = AudioSystem.getClip();
            hitClip.open(audioInputStream);
        } catch (Exception e) {
            hitClip = null;
        }
    }
    
    public void update(List<Rectangle2D.Double> collisions, int mapWidth, int mapHeight) {
        if (hp <= 0) return;
        
        if (hitCooldown > 0) {
            hitCooldown--;
            isHit = true;
            isMoving = false;
            isIdle = false;
        } else {
            isHit = false;
            
            if (!isIdle) {
                if (Math.random() < 0.01) {
                    angle = Math.random() * Math.PI * 2;
                }
                
                velocityX = Math.cos(angle) * speed;
                velocityY = Math.sin(angle) * speed;
                
                double newX = x + velocityX;
                double newY = y + velocityY;
                
                Rectangle2D.Double testRect = new Rectangle2D.Double(newX, newY, frameWidth, frameHeight);
                
                boolean canMove = !Utils.rectHitsCollision(testRect, collisions);
                boolean withinAnyZone = false;
                for (int[] zone : Config.CHICKEN_SPAWN_ZONES) {
                    int zoneX = zone[0];
                    int zoneY = zone[1];
                    int zoneSize = zone[2];
                    if (newX >= zoneX - zoneSize/2 && newX <= zoneX + zoneSize/2 &&
                        newY >= zoneY - zoneSize/2 && newY <= zoneY + zoneSize/2) {
                        withinAnyZone = true;
                        break;
                    }
                }
                
                boolean withinBounds = newX >= 50 && newY >= 50 && 
                                    newX < mapWidth - frameWidth - 50 && 
                                    newY < mapHeight - frameHeight - 50;
                
                if (canMove && withinBounds && withinAnyZone) {
                    x = (int) newX;
                    y = (int) newY;
                    isMoving = true;
                } else {
                    if (!withinAnyZone) {
                        int[] nearestZone = Config.CHICKEN_SPAWN_ZONES[0];
                        double minDist = Double.MAX_VALUE;
                        for (int[] zone : Config.CHICKEN_SPAWN_ZONES) {
                            double dist = Math.sqrt(Math.pow(x - zone[0], 2) + Math.pow(y - zone[1], 2));
                            if (dist < minDist) {
                                minDist = dist;
                                nearestZone = zone;
                            }
                        }
                        double dx = nearestZone[0] - x;
                        double dy = nearestZone[1] - y;
                        angle = Math.atan2(dy, dx);
                    } else {
                        angle = Math.random() * Math.PI * 2;
                    }
                    isMoving = false;
                }
            } else {
                isMoving = false;
            }
        }
        
        animationCounter++;
        if (animationCounter >= animationSpeed) {
            animationCounter = 0;
            currentFrame++;
            
            if (isHit) {
                if (currentFrame >= hitFrames) {
                    currentFrame = 0;
                }
            } else if (isMoving) {
                if (currentFrame >= runFrames) {
                    currentFrame = 0;
                }
            } else {
                if (currentFrame >= idleFrames) {
                    currentFrame = 0;
                }
            }
        }
    }
    
    public void updateFromData(ChickenData data) {
        if (data == null) return;
        
        this.x = data.x;
        this.y = data.y;
        this.hp = data.hp;
        this.angle = data.angle;
        this.isMoving = data.isMoving;
        this.isHit = data.isHit;
        this.isIdle = data.isIdle;
        this.currentFrame = data.currentFrame;
        
        if (isHit) {
            hitCooldown = 30;
        }
    }
    
    public void takeDamage(int damage) {
        hp -= damage;
        hitCooldown = 60;
        isHit = true;
        isIdle = false;
        
        if (hitClip != null) {
            if (hitClip.isRunning()) {
                hitClip.stop();
            }
            hitClip.setFramePosition(0);
            hitClip.start();
        }
    }
    
    public void draw(Graphics2D g2, int camX, int camY) {
        if (hp <= 0) return;
        
        int screenX = x - camX;
        int screenY = y - camY;
        
        if (screenX < -frameWidth || screenY < -frameHeight || 
            screenX > 1000 || screenY > 800) {
            return;
        }
        
        BufferedImage currentSheet = null;
        int totalFrames = 0;
        
        if (isHit && hitSheet != null) {
            currentSheet = hitSheet;
            totalFrames = hitFrames;
        } else if (isMoving && runSheet != null) {
            currentSheet = runSheet;
            totalFrames = runFrames;
        } else if (idleSheet != null) {
            currentSheet = idleSheet;
            totalFrames = idleFrames;
        }
        
        if (currentSheet != null) {
            int frameX = currentFrame * frameWidth;
            int frameY = 0;
            
            if (frameX + frameWidth <= currentSheet.getWidth()) {
                BufferedImage frame = currentSheet.getSubimage(frameX, frameY, frameWidth, frameHeight);
                
                Graphics2D g2d = (Graphics2D) g2.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (velocityX < 0) {
                    g2d.scale(-1, 1);
                    g2d.drawImage(frame, -screenX - frameWidth, screenY, null);
                } else {
                    g2d.drawImage(frame, screenX, screenY, null);
                }
                
                g2d.dispose();
            }
        } else {
            g2.setColor(Color.YELLOW);
            g2.fillOval(screenX, screenY, frameWidth, frameHeight);
        }
        
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        int nameX = screenX - fm.stringWidth("Chicken") / 2;
        int nameY = screenY - 5;
        g2.drawString("Chicken", nameX, nameY);
        
        g2.setColor(Color.GREEN);
        g2.fillRect(screenX, screenY - 15, frameWidth, 3);
        g2.setColor(Color.RED);
        g2.fillRect(screenX, screenY - 15, (int)(frameWidth * (hp / (double)Config.CHICKEN_HP)), 3);
        
        g2.setColor(Color.BLACK);
        g2.drawRect(screenX, screenY - 15, frameWidth, 3);
    }
    
    public Rectangle2D.Double bounds() {
        return new Rectangle2D.Double(x, y, frameWidth, frameHeight);
    }
}
