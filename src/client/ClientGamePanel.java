package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;
import shared.*;

public class ClientGamePanel extends JPanel implements Runnable {
    private MapLoader mapLoader;
    private Camera camera = new Camera();

    public ClientPlayer localPlayer;
    private Map<String, ClientPlayer> otherPlayers = new HashMap<>();
    private java.util.List<Bullet> bullets = new ArrayList<>();
    private Map<String, Bot> bots = new HashMap<>();
    private java.util.List<HitEffect> effects = new ArrayList<>();
    private BufferedImage bulletImg;
    private Point mousePoint = new Point(0, 0);

    private Thread loop;
    private boolean running = true;

    private NetworkClient networkClient;

    public ClientGamePanel(String playerName, String playerId) throws Exception {
        setPreferredSize(new Dimension(1280, 768));
        setBackground(Color.black);

        mapLoader = new MapLoader();
        mapLoader.load("assets/map/mappgameeeee.tmx");

        bulletImg = ImageIO.read(new File("assets/gun/bullet.png"));
        localPlayer = new ClientPlayer(200, 200, bulletImg, playerId, playerName);

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
                    localPlayer.shooting = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    localPlayer.shooting = false;
                }
            }
        });

        // Initialize network client
        networkClient = new NetworkClient(this);
        networkClient.connect("localhost", 8888);
        networkClient.sendPlayerJoin(localPlayer.toPlayerData());

        // Show connection notification
        NotificationSystem.addNotification("Connected to server", Color.GREEN);

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

        // Draw other players first
        for (ClientPlayer player : otherPlayers.values())
            player.draw(g2, camera.camX, camera.camY);

        // Draw local player on top
        localPlayer.draw(g2, camera.camX, camera.camY);

        for (Bot b : bots.values())
            b.draw(g2, camera.camX, camera.camY);
        for (Bullet b : bullets)
            b.draw(g2, camera.camX, camera.camY);
        for (HitEffect e : effects)
            e.draw(g2, camera.camX, camera.camY);

        drawHUD(g2);

        // Draw notifications
        NotificationSystem.drawNotifications(g2, getWidth(), getHeight());

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

            localPlayer.update(mousePoint, bullets, mapLoader.collisions,
                    mapLoader.mapPixelW, mapLoader.mapPixelH,
                    camera);

            // Send player update to server
            networkClient.sendPlayerUpdate(localPlayer.toPlayerData());

            Iterator<Bullet> it = bullets.iterator();
            while (it.hasNext()) {
                Bullet blt = it.next();
                if (!blt.update(mapLoader.collisions, mapLoader.mapPixelW, mapLoader.mapPixelH)) {
                    it.remove();
                    continue;
                }
                Rectangle2D.Double bRect = blt.bounds();
                for (Bot bot : bots.values()) {
                    if (bot.bounds().intersects(bRect)) {
                        effects.add(new HitEffect((int) (blt.x + 4), (int) (blt.y + 4)));
                        it.remove();
                        // Send bullet hit to server
                        networkClient.sendBotHit(bot, Config.BULLET_DAMAGE);
                        break;
                    }
                }
            }

            effects.removeIf(e -> !e.update());

            camera.centerOn(localPlayer.getCenterX(), localPlayer.getCenterY(),
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
        g2.drawString(localPlayer.ammo + "/" + Config.MAX_AMMO, hudX, hudY);

        if (localPlayer.reloading) {
            g2.setColor(new Color(255, 100, 100));
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString("RELOADING...", hudX, hudY + 20);
        }

        // Draw player list
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        int y = 20;
        g2.drawString("Online Players:", 10, y);
        y += 20;
        g2.drawString(localPlayer.playerName + " (You)", 10, y);
        y += 20;
        for (ClientPlayer player : otherPlayers.values()) {
            g2.drawString(player.playerName, 10, y);
            y += 20;
        }
    }

    public void addPlayer(PlayerData playerData) {
        try {
            ClientPlayer player = new ClientPlayer((int) playerData.x, (int) playerData.y, bulletImg,
                    playerData.id, playerData.name);
            otherPlayers.put(playerData.id, player);
            NotificationSystem.addNotification(playerData.name + " joined the game", Color.GREEN);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removePlayer(String playerId) {
        ClientPlayer player = otherPlayers.remove(playerId);
        if (player != null) {
            NotificationSystem.addNotification(player.playerName + " left the game", Color.RED);
        }
    }

    public void updatePlayer(PlayerData playerData) {
        ClientPlayer player = otherPlayers.get(playerData.id);
        if (player != null) {
            player.x = (int) playerData.x;
            player.y = (int) playerData.y;
            player.angle = playerData.angle;
            player.hp = playerData.hp;
            player.ammo = playerData.ammo;
            player.shooting = playerData.shooting;
            player.reloading = playerData.reloading;
        }
    }

    public void addBullet(BulletData bulletData) {
        bullets.add(new Bullet(bulletData.x, bulletData.y, bulletData.angle, bulletImg));
    }

    public void addBot(BotData botData) {
        try {
            Bot bot = new Bot((int) botData.x, (int) botData.y);
            bot.hp = botData.hp;
            bot.angle = botData.angle;
            bots.put(botData.id, bot);
            NotificationSystem.addNotification("Zombie spawned nearby!", Color.ORANGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateBot(BotData botData) {
        Bot bot = bots.get(botData.id);
        if (bot != null) {
            bot.x = botData.x;
            bot.y = botData.y;
            bot.angle = botData.angle;
            bot.hp = botData.hp;
        }
    }

    public void removeBot(String botId) {
        Bot bot = bots.remove(botId);
        if (bot != null) {
            NotificationSystem.addNotification("Zombie eliminated!", Color.CYAN);
        }
    }
}
