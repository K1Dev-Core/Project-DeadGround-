import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;

public class GamePanel extends JPanel implements Runnable {
    private MapLoader mapLoader;
    private Camera camera = new Camera();

    private Player player;
    private java.util.List<Bullet> bullets = new ArrayList<>();
    private java.util.List<Bot> bots = new ArrayList<>();
    private java.util.List<HitEffect> effects = new ArrayList<>();
    private BufferedImage bulletImg;
    private Point mousePoint = new Point(0, 0);

    private Thread loop;
    private boolean running = true;

    public GamePanel() throws Exception {
        setPreferredSize(new Dimension(1280, 768));
        setBackground(Color.black);

        mapLoader = new MapLoader();
        mapLoader.load("assets/map/mappgameeeee.tmx");

        bulletImg = ImageIO.read(new File("assets/gun/bullet.png"));
        player = new Player(200, 200, bulletImg);

        for (int i = 0; i < 3; i++)
            spawnBotNearPlayer();

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePoint = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mousePoint = e.getPoint();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    player.shooting = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    player.shooting = false;
                }
            }
        });

        loop = new Thread(this, "game-loop");
        loop.start();

    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        int viewW = getWidth();
        int viewH = getHeight();
        int startCol = Math.max(0, camera.camX / mapLoader.tileWidth);
        int startRow = Math.max(0, camera.camY / mapLoader.tileHeight);
        int endCol = Math.min(mapLoader.mapWidthTiles - 1, (camera.camX + viewW) / mapLoader.tileWidth + 1);
        int endRow = Math.min(mapLoader.mapHeightTiles - 1, (camera.camY + viewH) / mapLoader.tileHeight + 1);

        for (MapLoader.Layer layer : mapLoader.layers)
            drawLayer(g2, layer, startCol, startRow, endCol, endRow);

        player.draw(g2, camera.camX, camera.camY);
        for (Bot b : bots)
            b.draw(g2, camera.camX, camera.camY);
        for (Bullet b : bullets)
            b.draw(g2, camera.camX, camera.camY);
        for (HitEffect e : effects)
            e.draw(g2, camera.camX, camera.camY);

        drawHUD(g2);

        g2.dispose();
    }

    private void drawLayer(Graphics2D g2, MapLoader.Layer layer,
            int startCol, int startRow, int endCol, int endRow) {
        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                int idx = r * layer.width + c;
                if (idx < 0 || idx >= layer.gids.length)
                    continue;
                int gid = layer.gids[idx];
                if (gid == 0)
                    continue;
                BufferedImage tile = mapLoader.tileset.getTile(gid);
                if (tile == null)
                    continue;
                int x = c * mapLoader.tileWidth - camera.camX;
                int y = r * mapLoader.tileHeight - camera.camY;
                g2.drawImage(tile, x, y, null);
            }
        }
    }

    @Override
    public void run() {
        long frameTime = 1000L / Config.FPS;
        while (running) {
            long start = System.currentTimeMillis();

            player.update(mousePoint, bullets, mapLoader.collisions,
                    mapLoader.mapPixelW, mapLoader.mapPixelH,
                    camera);

            for (Bot b : bots)
                b.update(player, mapLoader.collisions,
                        mapLoader.mapPixelW, mapLoader.mapPixelH);

            Iterator<Bullet> it = bullets.iterator();
            while (it.hasNext()) {
                Bullet blt = it.next();
                if (!blt.update(mapLoader.collisions, mapLoader.mapPixelW, mapLoader.mapPixelH)) {
                    it.remove();
                    continue;
                }
                Rectangle2D.Double bRect = blt.bounds();
                for (int i = 0; i < bots.size(); i++) {
                    Bot bot = bots.get(i);
                    if (bot.bounds().intersects(bRect)) {
                        bot.hp -= Config.BULLET_DAMAGE;
                        effects.add(new HitEffect((int) (blt.x + 4), (int) (blt.y + 4)));
                        it.remove();
                        if (bot.hp <= 0) {
                            bots.remove(i);
                            spawnBotNearPlayer();
                        }
                        break;
                    }
                }
            }

            effects.removeIf(e -> !e.update());

            camera.centerOn(player.getCenterX(), player.getCenterY(),
                    getWidth(), getHeight(),
                    mapLoader.mapPixelW, mapLoader.mapPixelH);

            repaint();
            long dt = System.currentTimeMillis() - start;
            long sleep = Math.max(2, frameTime - dt);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void spawnBotNearPlayer() {
        try {
            BufferedImage tmp = ImageIO.read(new File("assets/zombie1_stand.png"));
            int bw = tmp.getWidth(), bh = tmp.getHeight();
            Random rnd = new Random();

            for (int tries = 0; tries < 30; tries++) {

                double ang = rnd.nextDouble() * Math.PI * 2;
                double dist = 200 + rnd.nextDouble() * 250;
                int cx = player.getCenterX() + (int) (Math.cos(ang) * dist) - bw / 2;
                int cy = player.getCenterY() + (int) (Math.sin(ang) * dist) - bh / 2;

                Rectangle2D.Double botRect = new Rectangle2D.Double(cx, cy, bw, bh);

                if (cx < 0 || cy < 0 || cx + bw > mapLoader.mapPixelW || cy + bh > mapLoader.mapPixelH)
                    continue;
                if (rectHitsCollision(botRect, mapLoader.collisions))
                    continue;
                System.out.println("x " + cx + " y " + cy);
                bots.add(new Bot(cx, cy));
                return;
            }

            bots.add(new Bot(mapLoader.mapPixelW / 2, mapLoader.mapPixelH / 2));
        } catch (Exception ignored) {
        }
    }

    public static boolean rectHitsCollision(Rectangle2D.Double r, java.util.List<Rectangle2D.Double> collisions) {
        for (Rectangle2D.Double c : collisions)
            if (c.intersects(r))
                return true;
        return false;
    }

    public static void drawHpBar(Graphics2D g2, int drawX, int drawY, int width, int hp) {
        g2.setColor(new Color(180, 0, 0));
        g2.fillRect(drawX, drawY - 10, width, 5);
        g2.setColor(new Color(30, 200, 60));
        g2.fillRect(drawX, drawY - 10, (int) (width * (hp / 100.0)), 5);
        g2.setColor(Color.black);
        g2.drawRect(drawX, drawY - 10, width, 5);
    }

    public static void drawAmmoText(Graphics2D g2, int drawX, int drawY, int ammo, int maxAmmo) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString(ammo + "/" + maxAmmo, drawX, drawY);
    }

    private void drawHUD(Graphics2D g2) {
        int screenWidth = getWidth();
        int screenHeight = getHeight();

        int hudX = screenWidth - 80;
        int hudY = screenHeight - 30;

        g2.setColor(new Color(255, 200, 0));
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString(player.ammo + "/" + Config.MAX_AMMO, hudX, hudY);

        if (player.reloading) {
            g2.setColor(new Color(255, 100, 100));
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString("RELOADING...", hudX, hudY + 20);
        }
    }

}
