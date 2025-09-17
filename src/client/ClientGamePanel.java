package client;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import shared.*;

public class ClientGamePanel extends JPanel implements Runnable {
    private MapLoader mapLoader;
    private Camera camera = new Camera();

    public ClientPlayer localPlayer;
    private Map<String, ClientPlayer> otherPlayers = new HashMap<>();
    private java.util.List<Bullet> bullets = new ArrayList<>();
    private Map<String, Bot> bots = new HashMap<>();
    public java.util.List<HitEffect> effects = new ArrayList<>();
    public java.util.List<CorpseEffect> corpses = new ArrayList<>();
    private java.util.List<Chicken> chickens = new ArrayList<>();
    private BufferedImage customCursor;
    private BufferedImage bulletImg;
    private Point mousePoint = new Point(0, 0);

    private Thread loop;
    private boolean running = true;

    private NetworkClient networkClient;

    public ClientGamePanel(String playerName, String playerId, String serverHost) throws Exception {
        setPreferredSize(new Dimension(1280, 768));
        setBackground(Color.black);
        setFocusable(true);
        requestFocusInWindow();
        setCursor(Toolkit.getDefaultToolkit().createCustomCursor(
            new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "invisible"));

        mapLoader = new MapLoader();
        mapLoader.load("assets/map/mappgameeeee.tmx");

        try {
            customCursor = ImageIO.read(new File("assets/cursor/cursor.png"));
        } catch (Exception e) {
            customCursor = null;
        }

        Point2D.Double spawnPos = Utils.findSafeSpawnPosition(mapLoader.mapPixelW, mapLoader.mapPixelH, mapLoader.collisions);
        localPlayer = new ClientPlayer((int)spawnPos.x, (int)spawnPos.y, null, playerId, playerName);
        
        for (int i = 0; i < 5; i++) {
            int chickenX = (int)(Math.random() * (mapLoader.mapPixelW - 100)) + 50;
            int chickenY = (int)(Math.random() * (mapLoader.mapPixelH - 100)) + 50;
            chickens.add(new Chicken(chickenX, chickenY));
        }

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
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W:
                        localPlayer.setMovingUp(true);
                        break;
                    case KeyEvent.VK_S:
                        localPlayer.setMovingDown(true);
                        break;
                    case KeyEvent.VK_A:
                        localPlayer.setMovingLeft(true);
                        break;
                    case KeyEvent.VK_D:
                        localPlayer.setMovingRight(true);
                        break;
                    case KeyEvent.VK_R:
                        localPlayer.reloading = true;
                        break;
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W:
                        localPlayer.setMovingUp(false);
                        break;
                    case KeyEvent.VK_S:
                        localPlayer.setMovingDown(false);
                        break;
                    case KeyEvent.VK_A:
                        localPlayer.setMovingLeft(false);
                        break;
                    case KeyEvent.VK_D:
                        localPlayer.setMovingRight(false);
                        break;
                }
            }
        });

        networkClient = new NetworkClient(this);
        networkClient.connect(serverHost, 8888);
        networkClient.sendPlayerJoin(localPlayer.toPlayerData());

        NotificationSystem.addNotification("Connected to " + serverHost, Color.GREEN);

        loop = new Thread(this, "game-loop");
        loop.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        
        if (Config.SMOOTH_MOVEMENT) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }

        int viewW = getWidth();
        int viewH = getHeight();
        int startCol = Math.max(0, camera.camX / mapLoader.tileWidth - Config.RENDER_DISTANCE);
        int startRow = Math.max(0, camera.camY / mapLoader.tileHeight - Config.RENDER_DISTANCE);
        int endCol = Math.min(mapLoader.mapWidthTiles - 1, (camera.camX + viewW) / mapLoader.tileWidth + Config.RENDER_DISTANCE);
        int endRow = Math.min(mapLoader.mapHeightTiles - 1, (camera.camY + viewH) / mapLoader.tileHeight + Config.RENDER_DISTANCE);

        for (MapLoader.Layer layer : mapLoader.layers)
            drawLayer(g2, layer, startCol, startRow, endCol, endRow);

        synchronized (effects) {
            ArrayList<HitEffect> effectsCopy = new ArrayList<>(effects);
            for (HitEffect e : effectsCopy) {
                if (e != null) {
                    e.draw(g2, camera.camX, camera.camY);
                }
            }
        }

        synchronized (corpses) {
            ArrayList<CorpseEffect> corpsesCopy = new ArrayList<>(corpses);
            for (CorpseEffect corpse : corpsesCopy) {
                if (corpse != null) {
                    corpse.draw(g2, camera.camX, camera.camY);
                }
            }
        }

        synchronized (chickens) {
            ArrayList<Chicken> chickensCopy = new ArrayList<>(chickens);
            for (Chicken chicken : chickensCopy) {
                if (chicken != null) {
                    chicken.draw(g2, camera.camX, camera.camY);
                }
            }
        }

        synchronized (otherPlayers) {
            ArrayList<ClientPlayer> playersCopy = new ArrayList<>(otherPlayers.values());
            for (ClientPlayer player : playersCopy) {
                if (player != null) {
                    player.draw(g2, camera.camX, camera.camY, mousePoint, camera);
                }
            }
        }

        localPlayer.draw(g2, camera.camX, camera.camY, mousePoint, camera);
        
        if (localPlayer.hp <= 0) {
            drawDeathScreen(g2);
        }

        synchronized (bullets) {
            ArrayList<Bullet> bulletsCopy = new ArrayList<>(bullets);
            for (Bullet b : bulletsCopy) {
                if (b != null) {
                    b.draw(g2, camera.camX, camera.camY);
                }
            }
        }

        drawHUD(g2);

        NotificationSystem.drawNotifications(g2, getWidth(), getHeight());

        if (customCursor != null) {
            g2.drawImage(customCursor, mousePoint.x - 16, mousePoint.y - 16, 32, 32, null);
        }

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        String ammoText;
        if (localPlayer.reloading) {
            ammoText = "RELOADING...";
        } else {
            ammoText = localPlayer.ammo + "/" + Config.MAX_AMMO;
        }
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(ammoText);
        g2.drawString(ammoText, mousePoint.x - textWidth/2, mousePoint.y - 25);

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
        long lastTime = System.currentTimeMillis();
        while (running) {
            long currentTime = System.currentTimeMillis();
            long deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            if (localPlayer.isDead && System.currentTimeMillis() - localPlayer.deathTime >= Config.RESPAWN_TIME * 1000) {
                localPlayer.respawn(mapLoader.mapPixelW, mapLoader.mapPixelH, mapLoader.collisions, otherPlayers);
                NotificationSystem.addNotification("You respawned!", Color.GREEN);
            }
            
            localPlayer.update(mousePoint, bullets, mapLoader.collisions,
                    mapLoader.mapPixelW, mapLoader.mapPixelH,
                    camera, otherPlayers);

            synchronized (chickens) {
                ArrayList<Chicken> chickensToRemove = new ArrayList<>();
                for (int i = 0; i < chickens.size(); i++) {
                    Chicken chicken = chickens.get(i);
                    if (chicken != null) {
                        chicken.update(mapLoader.collisions, mapLoader.mapPixelW, mapLoader.mapPixelH);
                        if (chicken.hp <= 0) {
                            chickensToRemove.add(chicken);
                        } else {
                            ChickenData chickenData = new ChickenData(i, chicken.x, chicken.y);
                            chickenData.hp = chicken.hp;
                            chickenData.angle = chicken.angle;
                            chickenData.isMoving = chicken.isMoving;
                            chickenData.isHit = chicken.isHit;
                            chickenData.isIdle = chicken.isIdle;
                            chickenData.currentFrame = chicken.currentFrame;
                            networkClient.sendChickenUpdate(chickenData);
                        }
                    }
                }
                chickens.removeAll(chickensToRemove);
            }

            if (System.currentTimeMillis() % Config.NETWORK_UPDATE_RATE == 0) {
                networkClient.sendPlayerUpdate(localPlayer.toPlayerData());
            }

            synchronized (bullets) {
                ArrayList<Bullet> bulletsToRemove = new ArrayList<>();
                for (int i = bullets.size() - 1; i >= 0; i--) {
                    Bullet blt = bullets.get(i);
                    if (blt == null) {
                        bullets.remove(i);
                        continue;
                    }
                    
                    if (!blt.update(mapLoader.collisions, mapLoader.mapPixelW, mapLoader.mapPixelH)) {
                        bulletsToRemove.add(blt);
                        continue;
                    }
                    
                    if (blt.justSpawned) {
                        networkClient.sendBulletSpawn(blt.toBulletData());
                        blt.justSpawned = false;
                    }
                    
                    Rectangle2D.Double bRect = blt.bounds();
                    
                    synchronized (chickens) {
                        ArrayList<Chicken> chickensCopy = new ArrayList<>(chickens);
                        for (Chicken chicken : chickensCopy) {
                            if (chicken != null && chicken.hp > 0) {
                                Rectangle2D.Double chickenRect = chicken.bounds();
                                if (chickenRect.intersects(bRect)) {
                                    effects.add(new HitEffect((int) (blt.x + 4), (int) (blt.y + 4)));
                                    bulletsToRemove.add(blt);
                                    chicken.takeDamage(Config.BULLET_DAMAGE);
                                    if (chicken.hp <= 0) {
                                        if (localPlayer.hp < Config.PLAYER_HP) {
                                            localPlayer.hp = Math.min(Config.PLAYER_HP, localPlayer.hp + Config.CHICKEN_HEAL_AMOUNT);
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    
                    synchronized (otherPlayers) {
                        ArrayList<ClientPlayer> playersCopy = new ArrayList<>(otherPlayers.values());
                        for (ClientPlayer player : playersCopy) {
                            if (player != null && player.hp > 0 && !player.playerId.equals(localPlayer.playerId)) {
                                Rectangle2D.Double playerRect = player.bounds();
                                if (playerRect.intersects(bRect)) {
                                    System.out.println("BULLET HIT! Player: " + player.playerName + " HP: " + player.hp);
                                    effects.add(new HitEffect((int) (blt.x + 4), (int) (blt.y + 4)));
                                    bulletsToRemove.add(blt);
                                    
                                    networkClient.sendPlayerHit(player.playerId, Config.BULLET_DAMAGE);
                                    break;
                                }
                            }
                        }
                    }
                }
                
                for (Bullet blt : bulletsToRemove) {
                    bullets.remove(blt);
                }
            }

            synchronized (effects) {
                ArrayList<HitEffect> effectsToRemove = new ArrayList<>();
                for (HitEffect e : effects) {
                    if (e != null && !e.update()) {
                        effectsToRemove.add(e);
                    }
                }
                for (HitEffect e : effectsToRemove) {
                    effects.remove(e);
                }
            }
            
            synchronized (corpses) {
                ArrayList<CorpseEffect> corpsesToRemove = new ArrayList<>();
                for (CorpseEffect corpse : corpses) {
                    if (corpse != null && !corpse.update()) {
                        corpsesToRemove.add(corpse);
                    }
                }
                for (CorpseEffect corpse : corpsesToRemove) {
                    corpses.remove(corpse);
                }
            }

            camera.centerOn(localPlayer.getCenterX(), localPlayer.getCenterY(),
                    getWidth(), getHeight(),
                    mapLoader.mapPixelW, mapLoader.mapPixelH);

            repaint();
            
            long targetTime = frameTime;
            long sleep = Math.max(1, targetTime - deltaTime);
            try {
                if (sleep > 0) {
                    Thread.sleep(sleep);
                }
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

        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, 250, 200);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        int y = 20;
        g2.drawString("Online Players:", 10, y);
        y += 20;
        g2.drawString(localPlayer.playerName + " (You) - Kills: " + localPlayer.kills, 10, y);
        y += 20;
        synchronized (otherPlayers) {
            ArrayList<ClientPlayer> playersCopy2 = new ArrayList<>(otherPlayers.values());
            for (ClientPlayer player : playersCopy2) {
                if (player != null) {
                    g2.drawString(player.playerName + " - Kills: " + player.kills, 10, y);
                    y += 20;
                }
            }
        }
    }
    
    private void drawDeathScreen(Graphics2D g2) {
        int screenWidth = getWidth();
        int screenHeight = getHeight();
        
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        long timeLeft = Config.RESPAWN_TIME - ((System.currentTimeMillis() - localPlayer.deathTime) / 1000);
        if (timeLeft < 0) timeLeft = 0;
        
        g2.setFont(new Font("Arial", Font.BOLD, 48));
        g2.setColor(Color.WHITE);
        
        FontMetrics fm = g2.getFontMetrics();
        String countdownText = "RESPAWN IN: " + timeLeft;
        int textX = (screenWidth - fm.stringWidth(countdownText)) / 2;
        int textY = screenHeight / 2;
        
        g2.setColor(Color.BLACK);
        g2.drawString(countdownText, textX - 2, textY - 2);
        g2.drawString(countdownText, textX + 2, textY - 2);
        g2.drawString(countdownText, textX - 2, textY + 2);
        g2.drawString(countdownText, textX + 2, textY + 2);
        
        g2.setColor(Color.WHITE);
        g2.drawString(countdownText, textX, textY);
        
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.setColor(Color.RED);
        String diedText = "YOU DIED";
        fm = g2.getFontMetrics();
        int diedX = (screenWidth - fm.stringWidth(diedText)) / 2;
        int diedY = textY - 60;
        
        g2.setColor(Color.BLACK);
        g2.drawString(diedText, diedX - 1, diedY - 1);
        g2.drawString(diedText, diedX + 1, diedY - 1);
        g2.drawString(diedText, diedX - 1, diedY + 1);
        g2.drawString(diedText, diedX + 1, diedY + 1);
        
        g2.setColor(Color.RED);
        g2.drawString(diedText, diedX, diedY);
    }

    public void addPlayer(PlayerData playerData) {
        try {
            synchronized (otherPlayers) {
                List<Point2D.Double> existingPositions = new ArrayList<>();
                existingPositions.add(new Point2D.Double(localPlayer.x, localPlayer.y));
                for (ClientPlayer existingPlayer : otherPlayers.values()) {
                    existingPositions.add(new Point2D.Double(existingPlayer.x, existingPlayer.y));
                }
                
                Point2D.Double spawnPos = Utils.findSafeSpawnPosition(mapLoader.mapPixelW, mapLoader.mapPixelH, mapLoader.collisions, existingPositions);
                ClientPlayer player = new ClientPlayer((int) spawnPos.x, (int) spawnPos.y, null,
                        playerData.id, playerData.name);
                otherPlayers.put(playerData.id, player);
                NotificationSystem.addNotification(playerData.name + " joined the game", Color.GREEN);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removePlayer(String playerId) {
        synchronized (otherPlayers) {
            ClientPlayer player = otherPlayers.remove(playerId);
            if (player != null) {
                for (int i = 0; i < 20; i++) {
                    effects.add(new HitEffect((int) (player.x + Math.random() * 32), (int) (player.y + Math.random() * 32)));
                }
                NotificationSystem.addNotification(player.playerName + " left the game", Color.RED);
            }
        }
    }

    public void updatePlayer(PlayerData playerData) {
        synchronized (otherPlayers) {
            ClientPlayer player = otherPlayers.get(playerData.id);
            if (player != null) {
                int oldHp = player.hp;
                player.hp = playerData.hp;
                
                if (oldHp != player.hp) {
                    if (player.hp <= 0 && oldHp > 0 && !player.isDead) {
                        for (int i = 0; i < 15; i++) {
                            effects.add(new HitEffect((int) (player.x + Math.random() * 32), (int) (player.y + Math.random() * 32)));
                        }
                        synchronized (corpses) {
                            corpses.add(new CorpseEffect((int) player.x, (int) player.y, player.playerName));
                        }
                        NotificationSystem.addNotification(player.playerName + " died!", Color.RED);
                        player.playDeathSound();
                        player.isDead = true;
                        player.deathSoundPlayed = true;
                        localPlayer.kills++;
                    } else if (oldHp > player.hp && player.hp > 0) {
                        player.playDamageSound();
                    }
                }
                
                double lerpFactor = Config.PLAYER_LERP_FACTOR;
                player.x = (int) (player.x + (playerData.x - player.x) * lerpFactor);
                player.y = (int) (player.y + (playerData.y - player.y) * lerpFactor);
                player.angle = playerData.angle;
                player.ammo = playerData.ammo;
                player.kills = playerData.kills;
                player.shooting = playerData.shooting;
                player.reloading = playerData.reloading;
                
                if (playerData.isDead) {
                    if (!player.isDead) {
                        player.isDead = true;
                        player.deathTime = playerData.deathTime;
                    }
                } else {
                    if (player.isDead) {
                        synchronized (corpses) {
                            corpses.removeIf(corpse -> corpse.playerName.equals(player.playerName));
                        }
                    }
                    player.isDead = false;
                    player.deathTime = 0;
                }
            }
        }
    }

    public void addBullet(BulletData bulletData) {
        synchronized (bullets) {
            boolean exists = false;
            for (Bullet b : bullets) {
                if (b != null && Math.abs(b.x - bulletData.x) < 10 && Math.abs(b.y - bulletData.y) < 10) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                bullets.add(new Bullet(bulletData.x, bulletData.y, bulletData.angle, null));
            }
        }
    }
    
    public void hitPlayer(String playerId, int damage) {
        if (playerId.equals(localPlayer.playerId)) {
            for (int i = 0; i < 15; i++) {
                effects.add(new HitEffect((int) (localPlayer.x + Math.random() * 32), (int) (localPlayer.y + Math.random() * 32)));
            }
  
            localPlayer.playDamageSound();
        } else {
            synchronized (otherPlayers) {
                ClientPlayer player = otherPlayers.get(playerId);
                if (player != null) {
                    for (int i = 0; i < 15; i++) {
                        effects.add(new HitEffect((int) (player.x + Math.random() * 32), (int) (player.y + Math.random() * 32)));
                    }
                 
                }
            }
        }
    }

}
