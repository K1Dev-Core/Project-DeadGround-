package client;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import shared.*;

public class ClientPlayer implements IPlayer {
    int x, y;
    int hp = Config.PLAYER_HP;
    int kills = 0;
    BufferedImage stand, shoot, reload, gunStand;
    double angle = 0;
    int shootCooldown = 0;
    boolean shooting = false;
    boolean justShot = false;
    boolean deathSoundPlayed = false;

    int ammo = 0;
    int reloadCooldown = 0;
    boolean reloading = false;
    boolean hasWeapon = false;
    boolean isGodMode = false;
    int meleeCooldown = 0;
    BufferedImage meleeImage;

    boolean isDashing = false;
    int dashCooldown = 0;
    int dashDistance = 0;
    double dashAngle = 0;

    long deathTime = 0;
    boolean isDead = false;
    boolean movingUp = false;
    boolean movingDown = false;
    boolean movingLeft = false;
    boolean movingRight = false;
    boolean isMoving = false;

    Clip footstepClip;
    Clip shootClip;
    Clip reloadClip;
    Clip damageClip;
    Clip deathClip;
    Clip teleportClip;

    int frameCounter = 0;

    public String playerId;
    public String playerName;
    public String characterType;

    public ClientPlayer(int startX, int startY, BufferedImage bulletImg, String playerId, String playerName,
            String characterType)
            throws Exception {
        x = startX;
        y = startY;
        this.playerId = playerId;
        this.playerName = playerName;
        this.characterType = characterType;

        stand = ImageIO.read(new File("assets/player/" + characterType + "stand.png"));
        shoot = ImageIO.read(new File("assets/player/" + characterType + "machine.png"));
        reload = ImageIO.read(new File("assets/player/" + characterType + "reload.png"));
        meleeImage = ImageIO.read(new File("assets/player/" + characterType + "hold.png"));
        gunStand = ImageIO.read(new File("assets/player/" + characterType + "reload.png"));

        try {
            AudioInputStream ais1 = AudioSystem.getAudioInputStream(new File("assets/sfx/footsteps.wav"));
            footstepClip = AudioSystem.getClip();
            footstepClip.open(ais1);
            FloatControl gainControl = (FloatControl) footstepClip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(-10.0f);
        } catch (Exception ignore) {
        }

        try {
            AudioInputStream ais2 = AudioSystem.getAudioInputStream(new File("assets/sfx/short.wav"));
            shootClip = AudioSystem.getClip();
            shootClip.open(ais2);
        } catch (Exception ignore) {
        }

        try {
            AudioInputStream ais3 = AudioSystem.getAudioInputStream(new File("assets/sfx/gun-reload.wav"));
            reloadClip = AudioSystem.getClip();
            reloadClip.open(ais3);
        } catch (Exception ignore) {
        }

        try {
            AudioInputStream ais4 = AudioSystem.getAudioInputStream(new File("assets/sfx/villager.wav"));
            damageClip = AudioSystem.getClip();
            damageClip.open(ais4);
        } catch (Exception ignore) {
        }

        try {
            AudioInputStream ais5 = AudioSystem.getAudioInputStream(new File("assets/sfx/death.wav"));
            deathClip = AudioSystem.getClip();
            deathClip.open(ais5);

            AudioInputStream ais6 = AudioSystem.getAudioInputStream(new File("assets/sfx/teleport.wav"));
            teleportClip = AudioSystem.getClip();
            teleportClip.open(ais6);
        } catch (Exception ignore) {
        }
    }

    public void update(Point mouse,
            List<Bullet> bullets,
            List<Rectangle2D.Double> collisions,
            int mapW, int mapH,
            Camera camera,
            Map<String, ClientPlayer> otherPlayers,
            List<?> tanks) {

        if (hp <= 0) {
            if (!isDead) {
                isDead = true;
                deathTime = System.currentTimeMillis();
                if (!deathSoundPlayed) {
                    playDeathSound();
                    deathSoundPlayed = true;
                }
            }

            if (isDead && System.currentTimeMillis() - deathTime >= Config.RESPAWN_TIME * 1000) {
                hp = Config.PLAYER_HP;
                ammo = 0;
                hasWeapon = false;
                isDead = false;
                deathTime = 0;
                deathSoundPlayed = false;
            }
            return;
        }

        int centerX = getCenterX();
        int centerY = getCenterY();

        angle = Math.atan2(
                mouse.y + camera.camY - centerY,
                mouse.x + camera.camX - centerX);
        int dx = 0, dy = 0;
        isMoving = false;

        if (isDashing) {
            double dashDx = Math.cos(dashAngle) * Config.DASH_SPEED;
            double dashDy = Math.sin(dashAngle) * Config.DASH_SPEED;
            dx = (int) dashDx;
            dy = (int) dashDy;
            dashDistance += Config.DASH_SPEED;
            if (dashDistance >= Config.DASH_DISTANCE) {
                isDashing = false;
                dashDistance = 0;
            }
        } else {
            if (movingUp) {
                dy -= Config.PLAYER_SPEED;
                isMoving = true;
            }
            if (movingDown) {
                dy += Config.PLAYER_SPEED;
                isMoving = true;
            }
            if (movingLeft) {
                dx -= Config.PLAYER_SPEED;
                isMoving = true;
            }
            if (movingRight) {
                dx += Config.PLAYER_SPEED;
                isMoving = true;
            }
        }

        if (dx != 0) {
            Rectangle2D.Double nextX = new Rectangle2D.Double(x + dx, y, stand.getWidth(), stand.getHeight());
            if (!Utils.rectHitsCollision(nextX, collisions) && !collidesWithPlayers(nextX, otherPlayers))
                x += dx;
        }

        if (dy != 0) {
            Rectangle2D.Double nextY = new Rectangle2D.Double(x, y + dy, stand.getWidth(), stand.getHeight());
            if (!Utils.rectHitsCollision(nextY, collisions) && !collidesWithPlayers(nextY, otherPlayers))
                y += dy;
        }

        if (isMoving) {
            if (footstepClip != null && !footstepClip.isRunning()) {
                footstepClip.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } else {
            if (footstepClip != null && footstepClip.isRunning()) {
                footstepClip.stop();
            }
        }

        if (reloading) {
            if (reloadCooldown > 0) {
                reloadCooldown--;
                if (reloadClip != null && !reloadClip.isRunning()) {
                    reloadClip.loop(Clip.LOOP_CONTINUOUSLY);
                }
            } else {
                ammo = Config.MAX_AMMO;
                reloading = false;
                if (reloadClip != null && reloadClip.isRunning()) {
                    reloadClip.stop();
                }
            }
        } else if (shooting && shootCooldown == 0 && hasWeapon && ammo > 0 && !isDead && !justShot && !isDashing) {
            spawnBulletFromMuzzle(bullets);
            ammo--;
            playShootSound();
            shootCooldown = Config.PLAYER_SHOOT_DELAY;
            justShot = true;
        } else if (shooting && meleeCooldown == 0 && !hasWeapon && !isDead && !isDashing) {
            performMeleeAttack(otherPlayers);
            meleeCooldown = Config.MELEE_COOLDOWN;
        } else if (shooting && hasWeapon && ammo == 0 && !reloading) {
            reloading = true;
            reloadCooldown = Config.RELOAD_TIME;
        }
        if (shootCooldown > 0)
            shootCooldown--;
        if (meleeCooldown > 0)
            meleeCooldown--;
        if (dashCooldown > 0)
            dashCooldown--;

        if (justShot && shootCooldown == 0) {
            justShot = false;
        }
    }

    public void startDash() {
        if (dashCooldown == 0 && !isDashing) {
            isDashing = true;
            dashAngle = angle;
            dashDistance = 0;
            dashCooldown = Config.DASH_COOLDOWN;
            playTeleportSound();
        }
    }

    public void playTeleportSound() {
        try {
            if (teleportClip != null) {
                teleportClip.setFramePosition(0);
                teleportClip.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void drawDashEffect(Graphics2D g2, int drawX, int drawY) {
        g2.setColor(new Color(255, 255, 255, 150));
        g2.setStroke(new BasicStroke(3));

        int centerX = drawX + stand.getWidth() / 2;
        int centerY = drawY + stand.getHeight() / 2;

        double trailLength = 30;
        double trailX = centerX - Math.cos(angle) * trailLength;
        double trailY = centerY - Math.sin(angle) * trailLength;

        g2.drawLine(centerX, centerY, (int) trailX, (int) trailY);

        g2.setColor(new Color(255, 255, 255, 100));
        g2.fillOval(centerX - 15, centerY - 15, 30, 30);

        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillOval(centerX - 8, centerY - 8, 16, 16);
    }

    private void spawnBulletFromMuzzle(List<Bullet> bullets) {
        int centerX = getCenterX();
        int centerY = getCenterY();

        double cos = Math.cos(angle), sin = Math.sin(angle);
        double muzzleDistance = 20;
        double bx = centerX + cos * muzzleDistance;
        double by = centerY + sin * muzzleDistance;

        bullets.add(new Bullet(bx, by, angle, null));
    }

    public int getCenterX() {
        return x + stand.getWidth() / 2;
    }

    public int getCenterY() {
        return y + stand.getHeight() / 2;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }

    public void setMovingUp(boolean moving) {
        this.movingUp = moving;
    }

    public void setMovingDown(boolean moving) {
        this.movingDown = moving;
    }

    public void setMovingLeft(boolean moving) {
        this.movingLeft = moving;
    }

    public void setMovingRight(boolean moving) {
        this.movingRight = moving;
    }

    public void takeDamage(int damage) {
        if (!isGodMode) {
            hp -= damage;
            if (hp < 0)
                hp = 0;
            if (hp <= 0 && !isDead) {
                isDead = true;
                deathTime = System.currentTimeMillis();
            }
        }
    }

    public void respawn(int mapWidth, int mapHeight, java.util.List<Rectangle2D.Double> collisions,
            Map<String, ClientPlayer> otherPlayers) {
        List<Point2D.Double> existingPositions = new ArrayList<>();
        for (ClientPlayer existingPlayer : otherPlayers.values()) {
            if (existingPlayer != null && existingPlayer.hp > 0) {
                existingPositions.add(new Point2D.Double(existingPlayer.x, existingPlayer.y));
            }
        }

        Point2D.Double safePos = Utils.findSafeSpawnPosition(mapWidth, mapHeight, collisions, existingPositions);
        this.x = (int) safePos.x;
        this.y = (int) safePos.y;
        this.hp = Config.PLAYER_HP;
        this.ammo = 0;
        this.hasWeapon = false;
        this.isDead = false;
        this.deathTime = 0;
        this.deathSoundPlayed = false;
    }

    public void playDamageSound() {
        if (damageClip != null) {
            if (damageClip.isRunning()) {
                damageClip.stop();
            }
            damageClip.setFramePosition(0);
            damageClip.start();
        }
    }

    public void playDeathSound() {
        if (deathClip != null && !deathSoundPlayed) {
            if (deathClip.isRunning()) {
                deathClip.stop();
            }
            deathClip.setFramePosition(0);
            deathClip.start();
            deathSoundPlayed = true;
        }
    }

    public void playShootSound() {
        if (shootClip != null) {
            if (shootClip.isRunning())
                shootClip.stop();
            shootClip.setFramePosition(0);
            shootClip.start();
        }
    }

    private boolean collidesWithPlayers(Rectangle2D.Double rect, Map<String, ClientPlayer> otherPlayers) {
        for (ClientPlayer player : otherPlayers.values()) {
            if (player != null && player.hp > 0 && rect.intersects(player.bounds())) {
                return true;
            }
        }
        return false;
    }

    public void draw(Graphics2D g2, int camX, int camY, Point mouse, Camera camera) {
        int drawX = x - camX;
        int drawY = y - camY;

        if (hp <= 0) {
            return;
        }

        BufferedImage img;
        if (reloading && hasWeapon) {
            img = reload;
        } else if (shooting && hasWeapon) {
            img = shoot;
        } else if (shooting && !hasWeapon) {
            img = meleeImage;
        } else if (hasWeapon) {
            img = gunStand;
        } else {
            img = stand;
        }

        AffineTransform at = new AffineTransform();

        if (isDashing) {
            double dashBounce = Math.sin(System.currentTimeMillis() * 0.02) * 4;
            at.translate(drawX, drawY + dashBounce);
        } else if (isMoving) {
            double bounce = Math.sin(System.currentTimeMillis() * 0.01) * 2;
            at.translate(drawX, drawY + bounce);
        } else {
            at.translate(drawX, drawY);
        }

        at.rotate(angle, img.getWidth() / 2.0, img.getHeight() / 2.0);
        g2.drawImage(img, at, null);

        if (isDashing) {
            drawDashEffect(g2, drawX, drawY);
        }

        drawHpBar(g2, drawX, drawY, 60, hp);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        int nameWidth = fm.stringWidth(playerName);
        g2.drawString(playerName, drawX + (img.getWidth() - nameWidth) / 2, drawY - 15);
    }

    public PlayerData toPlayerData() {
        PlayerData data = new PlayerData(playerId, playerName, x, y, characterType);
        data.update(x, y, angle, hp, ammo, kills, shooting, reloading, isGodMode);
        data.hasWeapon = hasWeapon;
        return data;
    }

    public void performMeleeAttack(Map<String, ClientPlayer> otherPlayers) {
        double attackX = x + Math.cos(angle) * Config.MELEE_RANGE;
        double attackY = y + Math.sin(angle) * Config.MELEE_RANGE;
        Rectangle2D.Double attackRect = new Rectangle2D.Double(attackX - 20, attackY - 20, 40, 40);

        for (ClientPlayer player : otherPlayers.values()) {
            if (player != null && player.hp > 0 && attackRect.intersects(player.bounds())) {
                player.hp -= Config.MELEE_DAMAGE;
                if (player.hp < 0)
                    player.hp = 0;
            }
        }
    }

    public void pickupWeapon() {
        if (!hasWeapon) {
            hasWeapon = true;
            ammo = Config.MAX_AMMO;
            playPickupSound();
        }
    }

    public void playPickupSound() {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("assets/sfx/pickup.wav"));
            Clip pickupClip = AudioSystem.getClip();
            pickupClip.open(audioInputStream);
            FloatControl gainControl = (FloatControl) pickupClip.getControl(FloatControl.Type.MASTER_GAIN);

            pickupClip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Rectangle2D.Double bounds() {
        return new Rectangle2D.Double(x, y, w(), h());
    }

    int w() {
        return stand.getWidth();
    }

    int h() {
        return stand.getHeight();
    }

    private void drawHpBar(Graphics2D g2, int drawX, int drawY, int width, int hp) {
        g2.setColor(new Color(180, 0, 0));
        g2.fillRect(drawX, drawY - 10, width, 5);
        g2.setColor(new Color(30, 200, 60));
        g2.fillRect(drawX, drawY - 10, (int) (width * (hp / 100.0)), 5);
        g2.setColor(Color.black);
        g2.drawRect(drawX, drawY - 10, width, 5);
    }
}
