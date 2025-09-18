package client;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    public java.util.List<HitEffect> effects = new ArrayList<>();
    public java.util.List<CorpseEffect> corpses = new ArrayList<>();
    private java.util.List<Chicken> chickens = new ArrayList<>();
    private java.util.List<Long> chickenRespawnTimes = new ArrayList<>();
    private java.util.List<Weapon> weapons = new ArrayList<>();
    private java.util.List<ExplosionEffect> explosionEffects = new ArrayList<>();
    private ZombieManager zombieManager;
    private ChompManager chompManager;
    private BufferedImage customCursor;
    private BufferedImage bulletImg;
    private Point mousePoint = new Point(0, 0);

    private Thread loop;
    private boolean running = true;
    private boolean showHUD = false;
    private long gameStartTime;
    private boolean showHUDHint = true;

    private long lastFPSTime = 0;
    private int frameCount = 0;
    private int currentFPS = 0;
    private String gameVersion = "Unknown";

    private NetworkClient networkClient;

    private String loadVersion() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("version.txt"));
            if (!lines.isEmpty()) {
                return lines.get(0).trim();
            }
        } catch (IOException e) {
            System.out.println("Could not read version.txt: " + e.getMessage());
        }
        return "Unknown";
    }

    public ClientGamePanel(String playerName, String playerId, String serverHost, String characterType)
            throws Exception {
        setPreferredSize(new Dimension(1280, 768));
        setBackground(Color.black);
        setFocusable(true);
        setDoubleBuffered(false);

        if (Config.USE_ACCELERATED_GRAPHICS) {
            System.setProperty("sun.java2d.opengl", Config.ENABLE_OPENGL ? "true" : "false");
            System.setProperty("sun.java2d.d3d", "true");
            System.setProperty("sun.java2d.ddforcevram", "true");
            System.setProperty("sun.java2d.translaccel", "true");
            System.setProperty("sun.java2d.ddscale", "true");
        }

        requestFocusInWindow();
        setCursor(Toolkit.getDefaultToolkit().createCustomCursor(
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "invisible"));

        mapLoader = new MapLoader();
        try {
            mapLoader.load("assets/map/mappgameeeee.tmx");
            System.out.println("Map loaded successfully:");
            System.out.println("  Map size: " + mapLoader.mapWidthTiles + "x" + mapLoader.mapHeightTiles + " tiles");
            System.out.println("  Pixel size: " + mapLoader.mapPixelW + "x" + mapLoader.mapPixelH);
            System.out.println("  Tile size: " + mapLoader.tileWidth + "x" + mapLoader.tileHeight);
            System.out.println("  Layers: " + mapLoader.layers.size());
            System.out.println("  Collisions: " + mapLoader.collisions.size());
            for (int i = 0; i < mapLoader.layers.size(); i++) {
                MapLoader.Layer layer = mapLoader.layers.get(i);
                System.out
                        .println("    Layer " + i + ": " + layer.name + " (" + layer.width + "x" + layer.height + ")");
            }
        } catch (Exception e) {
            System.err.println("Failed to load map: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            customCursor = ImageIO.read(new File("assets/cursor/cursor.png"));
        } catch (Exception e) {
            customCursor = null;
        }

        Point2D.Double spawnPos = Utils.findSafeSpawnPosition(mapLoader.mapPixelW, mapLoader.mapPixelH,
                mapLoader.collisions);
        localPlayer = new ClientPlayer((int) spawnPos.x, (int) spawnPos.y, null, playerId, playerName, characterType);
        
        zombieManager = new ZombieManager(mapLoader.collisions);
        chompManager = new ChompManager(mapLoader.collisions);

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
                    case KeyEvent.VK_SPACE:
                        showHUD = !showHUD;
                        showHUDHint = false;
                        break;
                    case KeyEvent.VK_SHIFT:
                        if (localPlayer.dashCooldown == 0 && !localPlayer.isDashing) {
                            localPlayer.startDash();
                        }
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

        gameStartTime = System.currentTimeMillis();
        gameVersion = loadVersion();

        for (int i = 0; i < Config.CHICKEN_SPAWN_COUNT; i++) {
            int x, y;
            boolean validPosition = false;
            int attempts = 0;

            while (!validPosition && attempts < 50) {
                int[] zone = Config.CHICKEN_SPAWN_ZONES[i % Config.CHICKEN_SPAWN_ZONES.length];
                int zoneX = zone[0];
                int zoneY = zone[1];
                int zoneSize = zone[2];

                x = zoneX + (int) (Math.random() * zoneSize) - zoneSize / 2;
                y = zoneY + (int) (Math.random() * zoneSize) - zoneSize / 2;

                Rectangle2D.Double testRect = new Rectangle2D.Double(x, y, 32, 34);
                boolean canSpawn = !Utils.rectHitsCollision(testRect, mapLoader.collisions) &&
                        x >= 50 && y >= 50 &&
                        x < mapLoader.mapPixelW - 82 && y < mapLoader.mapPixelH - 84;

                if (canSpawn) {
                    chickens.add(new Chicken(i, x, y));
                    validPosition = true;
                }
                attempts++;
            }
        }

        for (int i = 0; i < Config.WEAPON_SPAWN_POINTS.length; i++) {
            int[] point = Config.WEAPON_SPAWN_POINTS[i];
            weapons.add(new Weapon(i, point[0], point[1]));
        }

        loop = new Thread(this, "game-loop");
        loop.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        int viewW = getWidth();
        int viewH = getHeight();
        int startCol = Math.max(0, camera.camX / mapLoader.tileWidth - Config.RENDER_DISTANCE);
        int startRow = Math.max(0, camera.camY / mapLoader.tileHeight - Config.RENDER_DISTANCE);
        int endCol = Math.min(mapLoader.mapWidthTiles - 1,
                (camera.camX + viewW) / mapLoader.tileWidth + Config.RENDER_DISTANCE);
        int endRow = Math.min(mapLoader.mapHeightTiles - 1,
                (camera.camY + viewH) / mapLoader.tileHeight + Config.RENDER_DISTANCE);

        for (MapLoader.Layer layer : mapLoader.layers)
            drawLayer(g2, layer, startCol, startRow, endCol, endRow);

        ArrayList<HitEffect> effectsCopy = new ArrayList<>(effects);
        for (HitEffect e : effectsCopy) {
            if (e != null) {
                e.draw(g2, camera.camX, camera.camY);
            }
        }

        ArrayList<ExplosionEffect> explosionCopy = new ArrayList<>(explosionEffects);
        for (ExplosionEffect e : explosionCopy) {
            if (e != null) {
                e.draw(g2, camera.camX, camera.camY);
            }
        }

        ArrayList<CorpseEffect> corpsesCopy = new ArrayList<>(corpses);
        for (CorpseEffect corpse : corpsesCopy) {
            if (corpse != null) {
                corpse.draw(g2, camera.camX, camera.camY);
            }
        }

        ArrayList<Chicken> chickensCopy = new ArrayList<>(chickens);
        for (Chicken chicken : chickensCopy) {
            if (chicken != null) {
                chicken.draw(g2, camera.camX, camera.camY);
            }
        }

        ArrayList<Weapon> weaponsCopy = new ArrayList<>(weapons);
        for (Weapon weapon : weaponsCopy) {
            if (weapon != null) {
                weapon.draw(g2, camera.camX, camera.camY);
            }
        }

        ArrayList<ClientPlayer> playersCopy = new ArrayList<>(otherPlayers.values());
        for (ClientPlayer player : playersCopy) {
            if (player != null) {
                player.draw(g2, camera.camX, camera.camY, mousePoint, camera);
                if (player.isDashing) {
                    player.drawDashEffect(g2, player.x - camera.camX, player.y - camera.camY);
                }
            }
        }

        localPlayer.draw(g2, camera.camX, camera.camY, mousePoint, camera);

        if (localPlayer.isGodMode) {
            drawGodModeLines(g2);
        }

        if (localPlayer.hp <= 0) {
            drawDeathScreen(g2);
        }

        ArrayList<Bullet> bulletsCopy = new ArrayList<>(bullets);
        for (Bullet b : bulletsCopy) {
            if (b != null) {
                b.draw(g2, camera.camX, camera.camY);
            }
        }

        if (showHUD) {
            drawHUD(g2);
        }
        drawDebugInfo(g2);

        long currentTime = System.currentTimeMillis();
        long timeSinceStart = currentTime - gameStartTime;

        if (showHUDHint && timeSinceStart < 20000) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("Press SPACE to toggle HUD", 10, 30);
        }

        NotificationSystem.drawNotifications(g2, getWidth(), getHeight());

        long fpsTime = System.currentTimeMillis();
        frameCount++;
        if (fpsTime - lastFPSTime >= 1000) {
            currentFPS = frameCount;
            frameCount = 0;
            lastFPSTime = fpsTime;
        }

        if (customCursor != null) {
            g2.drawImage(customCursor, mousePoint.x - 16, mousePoint.y - 16, 32, 32, null);
        }

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        if (localPlayer.hasWeapon) {
            String ammoText;
            if (localPlayer.reloading) {
                ammoText = "RELOADING...";
            } else {
                ammoText = localPlayer.ammo + "/" + Config.MAX_AMMO;
            }
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(ammoText);
            g2.drawString(ammoText, mousePoint.x - textWidth / 2, mousePoint.y - 25);
        }

        drawDashCooldownBar(g2);
        
     
        zombieManager.render(g2, camera.camX, camera.camY);
        chompManager.render(g2, camera.camX, camera.camY);

        g2.dispose();
    }

    private void drawLayer(Graphics2D g2, MapLoader.Layer layer,
            int startCol, int startRow, int endCol, int endRow) {
        int tileW = mapLoader.tileWidth;
        int tileH = mapLoader.tileHeight;
        int camX = camera.camX;
        int camY = camera.camY;

        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                int idx = r * layer.width + c;
                if (idx < 0 || idx >= layer.gids.length)
                    continue;
                int gid = layer.gids[idx];
                if (gid == 0)
                    continue;
                BufferedImage tile = mapLoader.getTile(gid);
                if (tile == null)
                    continue;
                int x = c * tileW - camX;
                int y = r * tileH - camY;
                g2.drawImage(tile, x, y, null);
            }
        }
    }

    @Override
    public void run() {
        long frameTime = 1000L / Config.FPS;
        long lastTime = System.nanoTime() / 1000000;

        while (running) {
            long currentTime = System.nanoTime() / 1000000;
            long deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            if (localPlayer.isDead
                    && System.currentTimeMillis() - localPlayer.deathTime >= Config.RESPAWN_TIME * 1000) {
                localPlayer.respawn(mapLoader.mapPixelW, mapLoader.mapPixelH, mapLoader.collisions, otherPlayers);
                NotificationSystem.addNotification("You respawned!", Color.GREEN);
            }

            localPlayer.update(mousePoint, bullets, mapLoader.collisions,
                    mapLoader.mapPixelW, mapLoader.mapPixelH,
                    camera, otherPlayers, new ArrayList<>());

            if (System.currentTimeMillis() % Config.NETWORK_UPDATE_RATE == 0) {
                networkClient.sendPlayerUpdate(localPlayer.toPlayerData());
            }

            if (networkClient != null && networkClient.lastPlayerData != null) {
                localPlayer.isGodMode = networkClient.lastPlayerData.isGodMode;
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

                    synchronized (otherPlayers) {
                        ArrayList<ClientPlayer> playersCopy = new ArrayList<>(otherPlayers.values());
                        for (ClientPlayer player : playersCopy) {
                            if (player != null && player.hp > 0 && !player.playerId.equals(localPlayer.playerId)) {
                                Rectangle2D.Double playerRect = player.bounds();
                                if (playerRect.intersects(bRect)) {
                                    System.out
                                            .println("BULLET HIT! Player: " + player.playerName + " HP: " + player.hp);
                                    effects.add(new HitEffect((int) (blt.x + 4), (int) (blt.y + 4)));
                                    bulletsToRemove.add(blt);

                                    networkClient.sendPlayerHit(player.playerId, Config.BULLET_DAMAGE);
                                    break;
                                }
                            }
                        }
                    }

                    synchronized (chickens) {
                        ArrayList<Chicken> chickensCopy = new ArrayList<>(chickens);
                        for (Chicken chicken : chickensCopy) {
                            if (chicken != null && chicken.hp > 0) {
                                Rectangle2D.Double chickenRect = chicken.bounds();
                                if (chickenRect.intersects(bRect)) {
                                    int oldHp = chicken.hp;
                                    chicken.takeDamage(Config.BULLET_DAMAGE);
                                    for (int j = 0; j < 5; j++) {
                                        effects.add(new HitEffect((int) (chicken.x + Math.random() * 32),
                                                (int) (chicken.y + Math.random() * 32)));
                                    }
                                    bulletsToRemove.add(blt);

                                    if (chicken.hp <= 0 && oldHp > 0) {
                                        if (localPlayer.hp < Config.PLAYER_HP) {
                                            localPlayer.hp = Math.min(Config.PLAYER_HP,
                                                    localPlayer.hp + Config.CHICKEN_HEAL_AMOUNT);
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    
                    
                    ArrayList<Zombie> zombiesCopy = new ArrayList<>(zombieManager.getZombies());
                    for (Zombie zombie : zombiesCopy) {
                        if (zombie != null && zombie.alive) {
                            Rectangle2D.Double zombieRect = new Rectangle2D.Double(zombie.x, zombie.y, 80, 80);
                            if (zombieRect.intersects(bRect)) {
                                zombie.takeDamage(Config.BULLET_DAMAGE);
                                for (int j = 0; j < 5; j++) {
                                    effects.add(new HitEffect((int) (zombie.x + Math.random() * 80),
                                            (int) (zombie.y + Math.random() * 80)));
                                }
                                bulletsToRemove.add(blt);
                                break;
                            }
                        }
                    }
                    
                    ArrayList<Chomp> chompsCopy = new ArrayList<>(chompManager.getChomps());
                    for (Chomp chomp : chompsCopy) {
                        if (chomp != null && chomp.alive) {
                            Rectangle2D.Double chompRect = new Rectangle2D.Double(chomp.x, chomp.y, 32, 32);
                            if (chompRect.intersects(bRect)) {
                                boolean wasAlive = chomp.alive;
                                chomp.takeDamage(Config.BULLET_DAMAGE);
                                
                                if (wasAlive && !chomp.alive) {
                                    localPlayer.hp = Math.min(localPlayer.hp + Config.CHOMP_HEAL_AMOUNT, Config.PLAYER_MAX_HP);
                                }
                                
                                for (int j = 0; j < 3; j++) {
                                    effects.add(new HitEffect((int) (chomp.x + Math.random() * 32),
                                            (int) (chomp.y + Math.random() * 32)));
                                }
                                bulletsToRemove.add(blt);
                                break;
                            }
                        }
                    }

                }

                for (Bullet blt : bulletsToRemove) {
                    bullets.remove(blt);
                }
            }

            synchronized (weapons) {
                ArrayList<Weapon> weaponsCopy = new ArrayList<>(weapons);
                for (Weapon weapon : weaponsCopy) {
                    if (weapon != null && !weapon.collected) {
                        weapon.update();

                        double distance = Math
                                .sqrt(Math.pow(localPlayer.x - weapon.x, 2) + Math.pow(localPlayer.y - weapon.y, 2));
                        if (distance <= Config.WEAPON_PICKUP_RANGE) {
                            localPlayer.pickupWeapon();
                            weapon.collected = true;
                            NotificationSystem.addNotification("Weapon picked up!", Color.YELLOW);
                        }
                    }
                }
            }

        java.util.List<IPlayer> allPlayers = new ArrayList<>();
        allPlayers.add(localPlayer);
        allPlayers.addAll(otherPlayers.values());
        

        zombieManager.setPlayers(allPlayers);
        zombieManager.update();
        chompManager.setPlayers(allPlayers);
        chompManager.update(mapLoader.collisions, allPlayers);
        
        synchronized (chickens) {
            ArrayList<Chicken> chickensToRemove = new ArrayList<>();
            ArrayList<Long> respawnTimesToRemove = new ArrayList<>();
            long chickenCurrentTime = System.currentTimeMillis();

                for (int i = 0; i < chickens.size(); i++) {
                    Chicken chicken = chickens.get(i);
                    if (chicken != null) {
                        chicken.update(mapLoader.collisions, mapLoader.mapPixelW, mapLoader.mapPixelH, chickens);
                        if (chicken.hp <= 0) {
                            chickensToRemove.add(chicken);
                            chickenRespawnTimes.add(chickenCurrentTime);
                        }
                    }
                }

                for (Chicken chicken : chickensToRemove) {
                    chickens.remove(chicken);
                }

                for (int i = chickenRespawnTimes.size() - 1; i >= 0; i--) {
                    if (chickenCurrentTime - chickenRespawnTimes.get(i) >= Config.CHICKEN_RESPAWN_TIME * 1000) {
                        int x, y;
                        boolean validPosition = false;
                        int attempts = 0;

                        while (!validPosition && attempts < 50) {
                            int[] zone = Config.CHICKEN_SPAWN_ZONES[(int) (Math.random()
                                    * Config.CHICKEN_SPAWN_ZONES.length)];
                            int zoneX = zone[0];
                            int zoneY = zone[1];
                            int zoneSize = zone[2];

                            x = zoneX + (int) (Math.random() * zoneSize) - zoneSize / 2;
                            y = zoneY + (int) (Math.random() * zoneSize) - zoneSize / 2;

                            Rectangle2D.Double testRect = new Rectangle2D.Double(x, y, 32, 34);
                            boolean canSpawn = !Utils.rectHitsCollision(testRect, mapLoader.collisions) &&
                                    x >= 50 && y >= 50 &&
                                    x < mapLoader.mapPixelW - 82 && y < mapLoader.mapPixelH - 84;

                            if (canSpawn) {
                                chickens.add(new Chicken(chickens.size(), x, y));
                                validPosition = true;
                            }
                            attempts++;
                        }
                        chickenRespawnTimes.remove(i);
                    }
                }
            }

            synchronized (effects) {
                ArrayList<HitEffect> effectsCopy = new ArrayList<>(effects);
                ArrayList<HitEffect> effectsToRemove = new ArrayList<>();
                for (HitEffect e : effectsCopy) {
                    if (e != null && !e.update()) {
                        effectsToRemove.add(e);
                    }
                }
                for (HitEffect e : effectsToRemove) {
                    effects.remove(e);
                }
            }

            synchronized (explosionEffects) {
                ArrayList<ExplosionEffect> explosionCopy = new ArrayList<>(explosionEffects);
                ArrayList<ExplosionEffect> explosionToRemove = new ArrayList<>();
                for (ExplosionEffect e : explosionCopy) {
                    if (e != null) {
                        e.update();
                        if (e.isFinished) {
                            explosionToRemove.add(e);
                        }
                    }
                }
                for (ExplosionEffect e : explosionToRemove) {
                    explosionEffects.remove(e);
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
            } catch (InterruptedException e) {
                break;
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

    private void drawDashCooldownBar(Graphics2D g2) {
        if (localPlayer.dashCooldown > 0) {
            int screenWidth = getWidth();
            int screenHeight = getHeight();
            int barWidth = 120;
            int barHeight = 8;
            int x = (screenWidth - barWidth) / 2;
            int y = screenHeight - 30;

            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRect(x - 1, y - 1, barWidth + 2, barHeight + 2);

            g2.setColor(Color.GRAY);
            g2.fillRect(x, y, barWidth, barHeight);

            double progress = 1.0 - (double) localPlayer.dashCooldown / Config.DASH_COOLDOWN;
            int fillWidth = (int) (barWidth * progress);

            g2.setColor(new Color(237, 207, 9, 205));
            g2.fillRect(x, y, fillWidth, barHeight);
        }
    }

    private void drawHUD(Graphics2D g2) {

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

    private void drawDebugInfo(Graphics2D g2) {

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("X: " + (int) localPlayer.x, 15, getHeight() - 70);
        g2.drawString("Y: " + (int) localPlayer.y, 15, getHeight() - 55);
        g2.drawString("FPS: " + currentFPS, 15, getHeight() - 40);
        g2.drawString("Version: " + gameVersion, 15, getHeight() - 25);

    }

    private void drawDeathScreen(Graphics2D g2) {
        int screenWidth = getWidth();
        int screenHeight = getHeight();

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        long timeLeft = Config.RESPAWN_TIME - ((System.currentTimeMillis() - localPlayer.deathTime) / 1000);
        if (timeLeft < 0)
            timeLeft = 0;

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

                Point2D.Double spawnPos = Utils.findSafeSpawnPosition(mapLoader.mapPixelW, mapLoader.mapPixelH,
                        mapLoader.collisions, existingPositions);
                ClientPlayer player = new ClientPlayer((int) spawnPos.x, (int) spawnPos.y, null,
                        playerData.id, playerData.name, playerData.characterType);
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
                    effects.add(new HitEffect((int) (player.x + Math.random() * 32),
                            (int) (player.y + Math.random() * 32)));
                }
                NotificationSystem.addNotification(player.playerName + " left the game", Color.RED);
            }
        }
    }

    public void updateChicken(ChickenData chickenData) {
        synchronized (chickens) {
            Chicken existingChicken = null;
            for (Chicken chicken : chickens) {
                if (chicken.id == chickenData.id) {
                    existingChicken = chicken;
                    break;
                }
            }

            if (existingChicken != null) {
                int oldHp = existingChicken.hp;
                existingChicken.x = chickenData.x;
                existingChicken.y = chickenData.y;
                existingChicken.hp = chickenData.hp;
                existingChicken.angle = chickenData.angle;
                existingChicken.isMoving = chickenData.isMoving;
                existingChicken.isHit = chickenData.isHit;
                existingChicken.isIdle = chickenData.isIdle;
                existingChicken.currentFrame = chickenData.currentFrame;

                if (chickenData.hp < oldHp && chickenData.hp > 0) {
                    existingChicken.takeDamage(0);
                }
            } else if (chickenData.hp > 0) {
                Chicken newChicken = new Chicken(chickenData.id, chickenData.x, chickenData.y);
                newChicken.hp = chickenData.hp;
                newChicken.angle = chickenData.angle;
                newChicken.isMoving = chickenData.isMoving;
                newChicken.isHit = chickenData.isHit;
                newChicken.isIdle = chickenData.isIdle;
                newChicken.currentFrame = chickenData.currentFrame;
                chickens.add(newChicken);
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
                            effects.add(new HitEffect((int) (player.x + Math.random() * 32),
                                    (int) (player.y + Math.random() * 32)));
                        }
                        synchronized (corpses) {
                            corpses.add(new CorpseEffect((int) player.x, (int) player.y, player.playerName));
                        }
                        NotificationSystem.addNotification(player.playerName + " died!", Color.RED);
                        player.playDeathSound();
                        player.isDead = true;
                        player.deathSoundPlayed = true;
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
                player.hasWeapon = playerData.hasWeapon;
                player.isGodMode = playerData.isGodMode;
                player.characterType = playerData.characterType;

                if (playerData.shooting && playerData.hasWeapon) {
                    player.playShootSound();
                }

                if (playerData.isDead) {
                    if (!player.isDead) {
                        player.isDead = true;
                        player.deathTime = playerData.deathTime;
                        for (int i = 0; i < 15; i++) {
                            effects.add(new HitEffect((int) (player.x + Math.random() * 32),
                                    (int) (player.y + Math.random() * 32)));
                        }
                        synchronized (corpses) {
                            corpses.add(new CorpseEffect((int) player.x, (int) player.y, player.playerName));
                        }
                        NotificationSystem.addNotification(player.playerName + " died!", Color.RED);
                        player.playDeathSound();
                        player.deathSoundPlayed = true;
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
                effects.add(new HitEffect((int) (localPlayer.x + Math.random() * 32),
                        (int) (localPlayer.y + Math.random() * 32)));
            }

            localPlayer.playDamageSound();
        } else {
            synchronized (otherPlayers) {
                ClientPlayer player = otherPlayers.get(playerId);
                if (player != null) {
                    for (int i = 0; i < 15; i++) {
                        effects.add(new HitEffect((int) (player.x + Math.random() * 32),
                                (int) (player.y + Math.random() * 32)));
                    }

                }
            }
        }
    }

    private void drawGodModeLines(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 0, 200));
        g2.setStroke(new BasicStroke(3));
        g2.setFont(new Font("Arial", Font.BOLD, 14));

        synchronized (otherPlayers) {
            for (ClientPlayer player : otherPlayers.values()) {
                if (player != null && player.hp > 0) {
                    int startX = localPlayer.x - camera.camX;
                    int startY = localPlayer.y - camera.camY;
                    int endX = player.x - camera.camX;
                    int endY = player.y - camera.camY;

                    g2.drawLine(startX, startY, endX, endY);

                    double distance = Math
                            .sqrt(Math.pow(player.x - localPlayer.x, 2) + Math.pow(player.y - localPlayer.y, 2));
                    String distanceText = String.format("%.0f", distance);
                    FontMetrics fm = g2.getFontMetrics();
                    int textWidth = fm.stringWidth(distanceText);
                    g2.setColor(Color.WHITE);
                    g2.fillRect((startX + endX - textWidth) / 2 - 2, (startY + endY) / 2 - 15, textWidth + 4, 18);
                    g2.setColor(Color.BLACK);
                    g2.drawString(distanceText, (startX + endX - textWidth) / 2, (startY + endY) / 2 - 2);
                    g2.setColor(new Color(255, 255, 0, 200));
                }
            }
        }
    }

}
