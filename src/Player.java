import javax.sound.sampled.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class Player {
    int x, y;
    int hp = Config.PLAYER_HP;
    BufferedImage stand, walk, shoot;
    double angle = 0;
    int shootCooldown = 0;

    Clip footstepClip;
    Clip shootClip;
    BufferedImage bulletImg;

    public Player(int startX, int startY, BufferedImage bulletImg) throws Exception {
        x = startX;
        y = startY;
        this.bulletImg = bulletImg;
        stand = ImageIO.read(new File("assets/player/hitman1_stand.png"));
        walk  = ImageIO.read(new File("assets/player/hitman1_hold.png"));
        shoot = ImageIO.read(new File("assets/player/hitman1_gun.png"));

        try {
            AudioInputStream ais1 = AudioSystem.getAudioInputStream(new File("assets/sfx/footsteps.wav"));
            footstepClip = AudioSystem.getClip();
            footstepClip.open(ais1);
        } catch (Exception ignore) {}

        try {
            AudioInputStream ais2 = AudioSystem.getAudioInputStream(new File("assets/sfx/short.wav"));
            shootClip = AudioSystem.getClip();
            shootClip.open(ais2);
        } catch (Exception ignore) {}
    }

    public void update(Point mouse,
                       List<Bullet> bullets,
                       List<Rectangle2D.Double> collisions,
                       int mapW, int mapH,
                       Camera camera) {

        int centerX = getCenterX();
        int centerY = getCenterY();

        // หันตามเมาส์
        angle = Math.atan2(
                (mouse.y / camera.scale) + camera.camY - centerY,
                (mouse.x / camera.scale) + camera.camX - centerX
        );


        int dx = (int)(Math.cos(angle) * Config.PLAYER_SPEED);
        int dy = (int)(Math.sin(angle) * Config.PLAYER_SPEED);

        Rectangle2D.Double nextX = new Rectangle2D.Double(x + dx, y, stand.getWidth(), stand.getHeight());
        if (!Utils.rectHitsCollision(nextX, collisions)) x += dx;

        Rectangle2D.Double nextY = new Rectangle2D.Double(x, y + dy, stand.getWidth(), stand.getHeight());
        if (!Utils.rectHitsCollision(nextY, collisions)) y += dy;

        // เสียงเดิน
        if (footstepClip != null && !footstepClip.isRunning()) {
            footstepClip.loop(Clip.LOOP_CONTINUOUSLY);
        }


        if (shootCooldown == 0) {
            int ox = centerX, oy = centerY;
            double cos = Math.cos(angle), sin = Math.sin(angle);
            double bx = ox + Config.PLAYER_FORWARD_OFFSET * cos + Config.PLAYER_SIDE_OFFSET * -sin;
            double by = oy + Config.PLAYER_FORWARD_OFFSET * sin + Config.PLAYER_SIDE_OFFSET * cos;

            bullets.add(new Bullet(bx, by, angle, bulletImg));

            if (shootClip != null) {
                if (shootClip.isRunning()) shootClip.stop();
                shootClip.setFramePosition(0);
                shootClip.start();
            }
            shootCooldown = Config.PLAYER_SHOOT_DELAY;
        }
        if (shootCooldown > 0) shootCooldown--;
    }

    public int getCenterX() { return x + stand.getWidth()/2; }
    public int getCenterY() { return y + stand.getHeight()/2; }

    public void draw(Graphics2D g2, int camX, int camY) {
        BufferedImage img = walk;
        int drawX = x - camX;
        int drawY = y - camY;

        AffineTransform at = new AffineTransform();
        at.translate(drawX, drawY);
        at.rotate(angle, img.getWidth()/2.0, img.getHeight()/2.0);
        g2.drawImage(img, at, null);

        GamePanel.drawHpBar(g2, drawX, drawY, img.getWidth(), hp);
    }
}
