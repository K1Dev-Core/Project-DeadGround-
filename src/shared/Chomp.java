package shared;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class Chomp {
    public int x, y;
    public int hp, maxHp;
    public boolean alive;
    public int attackCooldown;
    public int movementTimer;
    public Random random;
    public int targetX, targetY;
    public int currentFrame;
    public int frameTimer;
    public IPlayer targetPlayer;
    
    private BufferedImage[][] idleSprites;
    private static final int FRAME_DELAY = 15;
    private Clip chompSound;
    
    public Chomp(int x, int y) {
        this.x = x;
        this.y = y;
        this.hp = Config.CHOMP_HP;
        this.maxHp = Config.CHOMP_HP;
        this.alive = true;
        this.attackCooldown = 0;
        this.movementTimer = 0;
        this.random = new Random();
        this.targetX = x;
        this.targetY = y;
        this.currentFrame = 0;
        this.frameTimer = 0;
        this.targetPlayer = null;
        
        idleSprites = new BufferedImage[3][4];
        
        try {
            BufferedImage spriteSheet = ImageIO.read(new File("assets/chomp/chomp_idle.png"));
            
            for (int direction = 0; direction < 3; direction++) {
                for (int frame = 0; frame < 4; frame++) {
                    int spriteX = frame * 32;
                    int spriteY = direction * 32;
                    idleSprites[direction][frame] = spriteSheet.getSubimage(spriteX, spriteY, 32, 32);
                }
            }
            
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("assets/sfx/chomp.wav"));
            chompSound = AudioSystem.getClip();
            chompSound.open(audioInputStream);
        } catch (Exception e) {
            System.out.println("Error loading chomp sprites: " + e.getMessage());
        }
    }
    
    public void update(List<Rectangle2D.Double> collisions, List<IPlayer> players, List<Chomp> otherChomps) {
        if (!alive) return;
        
        attackCooldown--;
        frameTimer++;
        if (frameTimer >= FRAME_DELAY) {
            frameTimer = 0;
            currentFrame = (currentFrame + 1) % 4;
        }
        
        IPlayer nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        
        if (targetPlayer != null) {
            double dx = targetPlayer.getX() - x;
            double dy = targetPlayer.getY() - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance <= Config.CHOMP_CHASE_RANGE) {
                nearestPlayer = targetPlayer;
                nearestDistance = distance;
            } else {
                targetPlayer = null;
            }
        }
        
        if (nearestPlayer == null) {
            for (IPlayer player : players) {
                if (player != null) {
                    double dx = player.getX() - x;
                    double dy = player.getY() - y;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    if (distance < nearestDistance && distance <= Config.CHOMP_DETECTION_RANGE) {
                        nearestDistance = distance;
                        nearestPlayer = player;
                        targetPlayer = player;
                    }
                }
            }
        }
        
        if (nearestPlayer != null && nearestDistance <= Config.CHOMP_DETECTION_RANGE) {
            if (targetPlayer != null && targetPlayer != nearestPlayer) {
                double targetDistance = Math.sqrt((targetPlayer.getX() - x) * (targetPlayer.getX() - x) + (targetPlayer.getY() - y) * (targetPlayer.getY() - y));
                if (targetDistance <= Config.CHOMP_CHASE_RANGE) {
                    nearestPlayer = targetPlayer;
                    nearestDistance = targetDistance;
                }
            }
            
            if (targetPlayer == null || nearestDistance > Config.CHOMP_CHASE_RANGE) {
                targetPlayer = nearestPlayer;
            }
            
            double angleToPlayer = Math.atan2(nearestPlayer.getY() - y, nearestPlayer.getX() - x);
            
            double avoidX = 0;
            double avoidY = 0;
            
            for (Chomp otherChomp : otherChomps) {
                if (otherChomp != this && otherChomp.alive) {
                    double dx = x - otherChomp.x;
                    double dy = y - otherChomp.y;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    if (distance < Config.CHOMP_AVOIDANCE_RANGE && distance > 0) {
                        double avoidStrength = (Config.CHOMP_AVOIDANCE_RANGE - distance) / Config.CHOMP_AVOIDANCE_RANGE;
                        avoidX += (dx / distance) * avoidStrength * Config.CHOMP_AVOIDANCE_STRENGTH;
                        avoidY += (dy / distance) * avoidStrength * Config.CHOMP_AVOIDANCE_STRENGTH;
                    }
                }
            }
            
            targetX = (int) (nearestPlayer.getX() + Math.cos(angleToPlayer) * 40 + avoidX);
            targetY = (int) (nearestPlayer.getY() + Math.sin(angleToPlayer) * 40 + avoidY);
            
            if (nearestDistance <= Config.CHOMP_ATTACK_RANGE && attackCooldown <= 0) {
                nearestPlayer.takeDamage(Config.CHOMP_DAMAGE);
                attackCooldown = Config.CHOMP_ATTACK_COOLDOWN;
            }
        } else {
            if (targetPlayer != null) {
                double targetDistance = Math.sqrt((targetPlayer.getX() - x) * (targetPlayer.getX() - x) + (targetPlayer.getY() - y) * (targetPlayer.getY() - y));
                if (targetDistance > Config.CHOMP_CHASE_RANGE) {
                    targetPlayer = null;
                }
            }
            
            double avoidX = 0;
            double avoidY = 0;
            
            for (Chomp otherChomp : otherChomps) {
                if (otherChomp != this && otherChomp.alive) {
                    double dx = x - otherChomp.x;
                    double dy = y - otherChomp.y;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    if (distance < Config.CHOMP_AVOIDANCE_RANGE && distance > 0) {
                        double avoidStrength = (Config.CHOMP_AVOIDANCE_RANGE - distance) / Config.CHOMP_AVOIDANCE_RANGE;
                        avoidX += (dx / distance) * avoidStrength * Config.CHOMP_AVOIDANCE_STRENGTH;
                        avoidY += (dy / distance) * avoidStrength * Config.CHOMP_AVOIDANCE_STRENGTH;
                    }
                }
            }
            
            if (targetPlayer != null) {
                double targetDistance = Math.sqrt((targetPlayer.getX() - x) * (targetPlayer.getX() - x) + (targetPlayer.getY() - y) * (targetPlayer.getY() - y));
                if (targetDistance <= Config.CHOMP_CHASE_RANGE) {
                    double angleToTarget = Math.atan2(targetPlayer.getY() - y, targetPlayer.getX() - x);
                    targetX = (int) (targetPlayer.getX() + Math.cos(angleToTarget) * 40 + avoidX);
                    targetY = (int) (targetPlayer.getY() + Math.sin(angleToTarget) * 40 + avoidY);
                } else {
                    movementTimer--;
                    if (movementTimer <= 0) {
                        targetX = x + random.nextInt(Config.CHOMP_RANDOM_MOVE_RANGE) - (Config.CHOMP_RANDOM_MOVE_RANGE/2) + (int)avoidX;
                        targetY = y + random.nextInt(Config.CHOMP_RANDOM_MOVE_RANGE) - (Config.CHOMP_RANDOM_MOVE_RANGE/2) + (int)avoidY;
                        movementTimer = random.nextInt(Config.CHOMP_RANDOM_MOVE_TIMER) + 60;
                    }
                }
            } else {
                movementTimer--;
                if (movementTimer <= 0) {
                    targetX = x + random.nextInt(Config.CHOMP_RANDOM_MOVE_RANGE) - (Config.CHOMP_RANDOM_MOVE_RANGE/2) + (int)avoidX;
                    targetY = y + random.nextInt(Config.CHOMP_RANDOM_MOVE_RANGE) - (Config.CHOMP_RANDOM_MOVE_RANGE/2) + (int)avoidY;
                    movementTimer = random.nextInt(Config.CHOMP_RANDOM_MOVE_TIMER) + 60;
                }
            }
        }
        
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 0) {
            double moveX = (dx / distance) * Config.CHOMP_MOVEMENT_SPEED;
            double moveY = (dy / distance) * Config.CHOMP_MOVEMENT_SPEED;
            
            int newX = (int) (x + moveX);
            int newY = (int) (y + moveY);
            
            Rectangle2D.Double testRect = new Rectangle2D.Double(newX, newY, 32, 32);
            if (!Utils.rectHitsCollision(testRect, collisions)) {
                boolean canMove = true;
                for (Chomp otherChomp : otherChomps) {
                    if (otherChomp != this && otherChomp.alive) {
                        Rectangle2D.Double otherRect = new Rectangle2D.Double(otherChomp.x, otherChomp.y, 32, 32);
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
        }
    }
    
    public void takeDamage(int damage) {
        hp -= damage;
        if (hp <= 0) {
            hp = 0;
            alive = false;
        }
        
        if (chompSound != null) {
            chompSound.setFramePosition(0);
            chompSound.start();
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
        
        int direction = 0;
        if (targetPlayer != null) {
            double angle = Math.atan2(targetPlayer.getY() - y, targetPlayer.getX() - x);
            if (angle >= -Math.PI/3 && angle < Math.PI/3) {
                direction = 0;
            } else if (angle >= Math.PI/3 && angle < 2*Math.PI/3) {
                direction = 1;
            } else {
                direction = 2;
            }
        }
        
        BufferedImage currentSprite = idleSprites[direction][currentFrame];
        
        if (currentSprite != null) {
            g2.drawImage(currentSprite, screenX, screenY, 32, 32, null);
        }
        
        g2.setColor(Color.BLACK);
        g2.fillRect(screenX - 15, screenY - 20, 30, 4);
        g2.setColor(Color.RED);
        g2.fillRect(screenX - 14, screenY - 19, 28, 2);
        int healthWidth = (int) ((double) hp / maxHp * 28);
        g2.setColor(Color.GREEN);
        g2.fillRect(screenX - 14, screenY - 19, healthWidth, 2);
    }
}
