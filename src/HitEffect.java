import java.awt.*;

public class HitEffect {
    int x, y;
    int life = Config.HIT_FX_LIFE;

    public HitEffect(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean update() {
        life--;
        return life > 0;
    }

    public void draw(Graphics2D g2, int camX, int camY) {
        int alpha = (int)(180 * (life / (double)Config.HIT_FX_LIFE));
        g2.setColor(new Color(255, 60, 60, Math.max(0, alpha)));
        g2.fillOval(x - camX - 6, y - camY - 6, 12, 12);
    }
}
