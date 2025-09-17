package shared;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

public class Bot {
    public String id;
    public double x, y;
    public int hp = Config.BOT_HP;
    public double angle = 0;
    public int speed = Config.BOT_SPEED;
    public BufferedImage stand, walk;

    public Bot(int startX, int startY) throws Exception {
        this.id = "bot_" + System.currentTimeMillis();
        x = startX;
        y = startY;
        stand = ImageIO.read(new File("assets/enemy/zombie1_stand.png"));
        walk = ImageIO.read(new File("assets/enemy/zombie1_hold.png"));
    }

    int w() {
        return stand.getWidth();
    }

    int h() {
        return stand.getHeight();
    }

    public void update(IPlayer player, java.util.List<Rectangle2D.Double> collisions, int mapW, int mapH) {
        double tx = player.getCenterX() - (x + w() / 2.0);
        double ty = player.getCenterY() - (y + h() / 2.0);
        angle = Math.atan2(ty, tx);
        int dx = (int) (Math.cos(angle) * speed);
        int dy = (int) (Math.sin(angle) * speed);

        Rectangle2D.Double nextX = new Rectangle2D.Double(x + dx, y, w(), h());
        if (!Utils.rectHitsCollision(nextX, collisions))
            x += dx;
        Rectangle2D.Double nextY = new Rectangle2D.Double(x, y + dy, w(), h());
        if (!Utils.rectHitsCollision(nextY, collisions))
            y += dy;
    }

    public Rectangle2D.Double bounds() {
        return new Rectangle2D.Double(x, y, w(), h());
    }

    public void draw(Graphics2D g2, int camX, int camY) {
        BufferedImage img = walk;
        int drawX = (int) x - camX, drawY = (int) y - camY;
        AffineTransform at = new AffineTransform();
        at.translate(drawX, drawY);
        at.rotate(angle, img.getWidth() / 2.0, img.getHeight() / 2.0);
        g2.drawImage(img, at, null);
        drawHpBar(g2, drawX, drawY, img.getWidth(), hp);
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
