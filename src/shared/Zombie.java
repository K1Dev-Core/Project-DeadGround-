package shared;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class Zombie {
    public int x, y;
    public int hp, maxHp;
    public boolean alive;
    public double angle;
    
    private int attackCooldown;
    private int movementTimer;
    private Random random;
    private int targetX, targetY;
    private IPlayer targetPlayer;
    

    private BufferedImage[] flySprites;
    private int currentFrame;
    private int frameTimer;
    private static final int FRAME_DELAY = 10;
    
    private Clip zombieSound;
    
  
    // Use values from Config instead of hardcoded constants
    
    public Zombie(int x, int y) {
        this.x = x;
        this.y = y;
        this.hp = Config.ZOMBIE_HP;
        this.maxHp = Config.ZOMBIE_HP;
        this.alive = true;
        this.angle = 0;
        this.attackCooldown = 0;
        this.movementTimer = 0;
        this.random = new Random();
        this.targetX = x;
        this.targetY = y;
        this.currentFrame = 0;
        this.frameTimer = 0;
        
        loadSprites();
        loadSound();
    }
    
    private void loadSprites() {
        try {
            flySprites = new BufferedImage[6];
            
            for (int i = 0; i < 6; i++) {
                String fileName = "assets/enemy/Zombie/fly_" + i + ".png";
                File spriteFile = new File(fileName);
                
                if (spriteFile.exists()) {
                    BufferedImage sprite = ImageIO.read(spriteFile);
                    flySprites[i] = sprite;
                    System.out.println("Loaded zombie fly sprite " + i + ": " + sprite.getWidth() + "x" + sprite.getHeight());
                } else {
                    System.err.println("Zombie sprite file not found: " + fileName);
                  
                    flySprites[i] = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2 = flySprites[i].createGraphics();
                    g2.setColor(Color.RED);
                    g2.fillRect(0, 0, 32, 32);
                    g2.setColor(Color.WHITE);
                    g2.drawString("Z" + i, 5, 20);
                    g2.dispose();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load zombie sprites: " + e.getMessage());
            e.printStackTrace();
            
          
            flySprites = new BufferedImage[6];
            for (int i = 0; i < 6; i++) {
                flySprites[i] = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = flySprites[i].createGraphics();
                g2.setColor(Color.RED);
                g2.fillRect(0, 0, 32, 32);
                g2.setColor(Color.WHITE);
                g2.drawString("Z" + i, 5, 20);
                g2.dispose();
            }
        }
    }
    
    private void loadSound() {
        try {
            File soundFile = new File("assets/sfx/Zombie.wav");
            if (soundFile.exists()) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                zombieSound = AudioSystem.getClip();
                zombieSound.open(audioIn);
            }
        } catch (Exception e) {
            System.err.println("Failed to load zombie sound: " + e.getMessage());
        }
    }
    
    public void update(List<Rectangle2D.Double> collisions, List<IPlayer> players, List<Zombie> otherZombies) {
        if (!alive) return;
        
      
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        
   
        frameTimer++;
        if (frameTimer >= FRAME_DELAY) {
            frameTimer = 0;
            currentFrame = (currentFrame + 1) % flySprites.length;
        }
       
        IPlayer nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        
        if (targetPlayer != null) {
            double dx = targetPlayer.getX() - x;
            double dy = targetPlayer.getY() - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance <= Config.ZOMBIE_CHASE_RANGE) {
                nearestPlayer = targetPlayer;
                nearestDistance = distance;
            } else {
                targetPlayer = null;
            }
        }
        
        if (nearestPlayer == null && players != null) {
            for (IPlayer player : players) {
                if (player != null) {
                    double dx = player.getX() - x;
                    double dy = player.getY() - y;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    if (distance < nearestDistance && distance <= Config.ZOMBIE_DETECTION_RANGE) {
                        nearestDistance = distance;
                        nearestPlayer = player;
                        targetPlayer = player;
                    }
                }
            }
        }
       
        if (nearestPlayer != null && nearestDistance <= Config.ZOMBIE_DETECTION_RANGE) {
            // If we have a target player, check if they're still in chase range
            if (targetPlayer != null && targetPlayer != nearestPlayer) {
                double targetDistance = Math.sqrt((targetPlayer.getX() - x) * (targetPlayer.getX() - x) + (targetPlayer.getY() - y) * (targetPlayer.getY() - y));
                if (targetDistance <= Config.ZOMBIE_CHASE_RANGE) {
                    nearestPlayer = targetPlayer;
                    nearestDistance = targetDistance;
                }
            }
            
            // If no target player or current target is out of range, find new target
            if (targetPlayer == null || nearestDistance > Config.ZOMBIE_CHASE_RANGE) {
                targetPlayer = nearestPlayer;
            }
            double angleToPlayer = Math.atan2(nearestPlayer.getY() - y, nearestPlayer.getX() - x);
            double distanceToPlayer = nearestDistance;
            
            double avoidX = 0;
            double avoidY = 0;
            
            for (Zombie otherZombie : otherZombies) {
                if (otherZombie != this && otherZombie.alive) {
                    double dx = x - otherZombie.x;
                    double dy = y - otherZombie.y;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    if (distance < Config.ZOMBIE_AVOIDANCE_RANGE && distance > 0) {
                        double avoidStrength = (Config.ZOMBIE_AVOIDANCE_RANGE - distance) / Config.ZOMBIE_AVOIDANCE_RANGE;
                        avoidX += (dx / distance) * avoidStrength * Config.ZOMBIE_AVOIDANCE_STRENGTH;
                        avoidY += (dy / distance) * avoidStrength * Config.ZOMBIE_AVOIDANCE_STRENGTH;
                    }
                }
            }
            
            targetX = (int) (nearestPlayer.getX() + Math.cos(angleToPlayer) * 40 + avoidX);
            targetY = (int) (nearestPlayer.getY() + Math.sin(angleToPlayer) * 40 + avoidY);
            
            if (nearestDistance <= Config.ZOMBIE_ATTACK_RANGE && attackCooldown <= 0) {
                nearestPlayer.takeDamage(Config.ZOMBIE_DAMAGE);
                attackCooldown = Config.ZOMBIE_ATTACK_COOLDOWN;
            }
        } else {
            // Only clear target if they're really out of chase range
            if (targetPlayer != null) {
                double targetDistance = Math.sqrt((targetPlayer.getX() - x) * (targetPlayer.getX() - x) + (targetPlayer.getY() - y) * (targetPlayer.getY() - y));
                if (targetDistance > Config.ZOMBIE_CHASE_RANGE) {
                    targetPlayer = null;
                }
            }
            
            double avoidX = 0;
            double avoidY = 0;
            
            for (Zombie otherZombie : otherZombies) {
                if (otherZombie != this && otherZombie.alive) {
                    double dx = x - otherZombie.x;
                    double dy = y - otherZombie.y;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    if (distance < Config.ZOMBIE_AVOIDANCE_RANGE && distance > 0) {
                        double avoidStrength = (Config.ZOMBIE_AVOIDANCE_RANGE - distance) / Config.ZOMBIE_AVOIDANCE_RANGE;
                        avoidX += (dx / distance) * avoidStrength * Config.ZOMBIE_AVOIDANCE_STRENGTH;
                        avoidY += (dy / distance) * avoidStrength * Config.ZOMBIE_AVOIDANCE_STRENGTH;
                    }
                }
            }
            
            // If we have a target player, try to move towards them even in random mode
            if (targetPlayer != null) {
                double targetDistance = Math.sqrt((targetPlayer.getX() - x) * (targetPlayer.getX() - x) + (targetPlayer.getY() - y) * (targetPlayer.getY() - y));
                if (targetDistance <= Config.ZOMBIE_CHASE_RANGE) {
                    double angleToTarget = Math.atan2(targetPlayer.getY() - y, targetPlayer.getX() - x);
                    targetX = (int) (targetPlayer.getX() + Math.cos(angleToTarget) * 40 + avoidX);
                    targetY = (int) (targetPlayer.getY() + Math.sin(angleToTarget) * 40 + avoidY);
                } else {
                    movementTimer--;
                    if (movementTimer <= 0) {
                        targetX = x + random.nextInt(Config.ZOMBIE_RANDOM_MOVE_RANGE) - (Config.ZOMBIE_RANDOM_MOVE_RANGE/2) + (int)avoidX;
                        targetY = y + random.nextInt(Config.ZOMBIE_RANDOM_MOVE_RANGE) - (Config.ZOMBIE_RANDOM_MOVE_RANGE/2) + (int)avoidY;
                        movementTimer = random.nextInt(Config.ZOMBIE_RANDOM_MOVE_TIMER) + 60;
                    }
                }
            } else {
                movementTimer--;
                if (movementTimer <= 0) {
                    targetX = x + random.nextInt(Config.ZOMBIE_RANDOM_MOVE_RANGE) - (Config.ZOMBIE_RANDOM_MOVE_RANGE/2) + (int)avoidX;
                    targetY = y + random.nextInt(Config.ZOMBIE_RANDOM_MOVE_RANGE) - (Config.ZOMBIE_RANDOM_MOVE_RANGE/2) + (int)avoidY;
                    movementTimer = random.nextInt(Config.ZOMBIE_RANDOM_MOVE_TIMER) + 60;
                }
            }
        }
        
       
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 0) {
            double moveX = (dx / distance) * Config.ZOMBIE_MOVEMENT_SPEED;
            double moveY = (dy / distance) * Config.ZOMBIE_MOVEMENT_SPEED;
            
            int newX = (int) (x + moveX);
            int newY = (int) (y + moveY);
            
       
            Rectangle2D.Double testRect = new Rectangle2D.Double(newX, newY, 80, 80);
            if (!Utils.rectHitsCollision(testRect, collisions)) {
                boolean canMove = true;
                for (Zombie otherZombie : otherZombies) {
                    if (otherZombie != this && otherZombie.alive) {
                        Rectangle2D.Double otherRect = new Rectangle2D.Double(otherZombie.x, otherZombie.y, 80, 80);
                        if (testRect.intersects(otherRect)) {
                            canMove = false;
                            break;
                        }
                    }
                }
                if (canMove) {
                    x = newX;
                    y = newY;
                }
            }
            
        
            angle = Math.atan2(dy, dx);
        }
    }
    
    public void takeDamage(int damage) {
        hp -= damage;
        if (hp <= 0) {
            hp = 0;
            alive = false;
        }
        
        if (zombieSound != null) {
            zombieSound.setFramePosition(0);
            zombieSound.start();
        }
    }
    
    public void render(Graphics2D g2, int camX, int camY) {
        if (!alive) return;
        
        int screenX = x - camX;
        int screenY = y - camY;
        

        if (screenX < -Config.RENDER_DISTANCE_X || screenX > 800 + Config.RENDER_DISTANCE_X || 
            screenY < -Config.RENDER_DISTANCE_Y || screenY > 600 + Config.RENDER_DISTANCE_Y) {
            return;
        }
        
        BufferedImage currentSprite = flySprites[currentFrame];
        
        if (currentSprite != null) {
         
            if (angle > Math.PI / 2 || angle < -Math.PI / 2) {
                g2.drawImage(currentSprite, screenX + 80, screenY, -80, 80, null);
            } else {
                g2.drawImage(currentSprite, screenX, screenY, 80, 80, null);
            }
        }
        

        g2.setColor(Color.BLACK);
        g2.fillRect(screenX - 20, screenY - 25, 40, 6);
        
        g2.setColor(Color.RED);
        g2.fillRect(screenX - 19, screenY - 24, 38, 4);
        
        g2.setColor(Color.GREEN);
        int healthWidth = (int) ((double) hp / maxHp * 38);
        g2.fillRect(screenX - 19, screenY - 24, healthWidth, 4);
    }
}
