package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import shared.*;

public class GameServer {
    private ServerSocket serverSocket;
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    public Map<String, PlayerData> players = new ConcurrentHashMap<>();
    private Map<String, BotData> bots = new ConcurrentHashMap<>();
    public Map<Integer, ChickenData> chickens = new ConcurrentHashMap<>();
    private boolean running = false;
    private int port;
    private Thread gameLoop;
    private ExecutorService executor;
    public AtomicInteger messageCounter = new AtomicInteger(0);
    public ServerDebugUI debugUI;

    public GameServer(int port) {
        this.port = port;
        this.executor = Executors.newFixedThreadPool(10);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(1000);
            running = true;
            
            debugUI = new ServerDebugUI(this);
            debugUI.setVisible(true);
            
            spawnInitialChickens();
            
            System.out.println("=== Game Server Started ===");
            System.out.println("Port: " + port);
            System.out.println("Local IP: " + getLocalIPAddress());
            System.out.println("Waiting for connections...");
            System.out.println("================================");
            
            debugUI.logMessage("Server started on port " + port);
            debugUI.logMessage("Local IP: " + getLocalIPAddress());
            gameLoop = new Thread(this::runGameLoop, "GameLoop");
            gameLoop.setDaemon(true);
            gameLoop.start();

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    executor.submit(handler);
                    debugUI.logMessage("New client connected: " + clientSocket.getInetAddress());
                } catch (SocketTimeoutException e) {
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getLocalIPAddress() {
        try {
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void addClient(String playerId, ClientHandler handler) {
        clients.put(playerId, handler);
    }

    public void removeClient(String playerId) {
        clients.remove(playerId);
        players.remove(playerId);
    }

    public void addPlayer(PlayerData playerData) {
        players.put(playerData.id, playerData);
        broadcastToOthers(playerData.id, new NetworkMessage(NetworkMessage.PLAYER_JOIN, playerData.id, playerData, messageCounter.incrementAndGet()));

        ClientHandler newClient = clients.get(playerData.id);
        if (newClient != null) {
            for (PlayerData existingPlayer : players.values()) {
                if (!existingPlayer.id.equals(playerData.id)) {
                    newClient.sendMessage(
                            new NetworkMessage(NetworkMessage.PLAYER_JOIN, existingPlayer.id, existingPlayer, messageCounter.incrementAndGet()));
                }
            }
        }
        
        debugUI.logMessage("Player joined: " + playerData.name + " (ID: " + playerData.id + ")");
    }

    public void updatePlayer(PlayerData playerData) {
        players.put(playerData.id, playerData);
        broadcastToOthers(playerData.id, new NetworkMessage(NetworkMessage.PLAYER_UPDATE, playerData.id, playerData, messageCounter.incrementAndGet()));
    }

    public void broadcastToOthers(String excludePlayerId, NetworkMessage message) {
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (!entry.getKey().equals(excludePlayerId)) {
                entry.getValue().sendMessage(message);
            }
        }
    }

    public void broadcastToAll(NetworkMessage message) {
        for (ClientHandler handler : clients.values()) {
            handler.sendMessage(message);
        }
    }

    private void runGameLoop() {
        long frameTime = 1000L / Config.FPS;
        while (running) {
            long start = System.currentTimeMillis();

            cleanupDisconnectedClients();

            long dt = System.currentTimeMillis() - start;
            long sleep = Math.max(2, frameTime - dt);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private void spawnInitialChickens() {
        for (int i = 0; i < Config.CHICKEN_SPAWN_COUNT; i++) {
            int[] zone = Config.CHICKEN_SPAWN_ZONES[i % Config.CHICKEN_SPAWN_ZONES.length];
            int zoneX = zone[0];
            int zoneY = zone[1];
            int zoneSize = zone[2];
            
            int x = zoneX + (int)(Math.random() * zoneSize) - zoneSize/2;
            int y = zoneY + (int)(Math.random() * zoneSize) - zoneSize/2;
            chickens.put(i, new ChickenData(i, x, y));
            System.out.println("Spawned chicken " + i + " at " + x + ", " + y);
        }
    }
    
    private void updateChickens() {
        for (ChickenData chicken : chickens.values()) {
            if (chicken != null) {
                if (chicken.hp > 0) {
                    if (chicken.isHit) {
                        chicken.isHit = false;
                    }
                    
                    if (Math.random() < 0.02) {
                        chicken.angle = Math.random() * Math.PI * 2;
                        chicken.isIdle = false;
                    }
                    
                    if (!chicken.isIdle) {
                        double velocityX = Math.cos(chicken.angle) * Config.CHICKEN_MOVEMENT_SPEED;
                        double velocityY = Math.sin(chicken.angle) * Config.CHICKEN_MOVEMENT_SPEED;
                        
                        int newX = chicken.x + (int)velocityX;
                        int newY = chicken.y + (int)velocityY;
                        
                        boolean withinAnyZone = false;
                        for (int[] zone : Config.CHICKEN_SPAWN_ZONES) {
                            int zoneX = zone[0];
                            int zoneY = zone[1];
                            int zoneSize = zone[2];
                            if (newX >= zoneX - zoneSize/2 && newX <= zoneX + zoneSize/2 &&
                                newY >= zoneY - zoneSize/2 && newY <= zoneY + zoneSize/2) {
                                withinAnyZone = true;
                                break;
                            }
                        }
                        
                        if (withinAnyZone) {
                            chicken.x = newX;
                            chicken.y = newY;
                            chicken.isMoving = true;
                        } else {
                            chicken.angle = Math.random() * Math.PI * 2;
                            chicken.isMoving = false;
                        }
                    } else {
                        chicken.isMoving = false;
                    }
                    
                    if (chicken.isHit) {
                        chicken.currentFrame = (chicken.currentFrame + 1) % 5;
                    } else if (chicken.isMoving) {
                        chicken.currentFrame = (chicken.currentFrame + 1) % 14;
                    } else {
                        chicken.currentFrame = (chicken.currentFrame + 1) % 13;
                    }
                } else {
                    chicken.respawnTimer--;
                    if (chicken.respawnTimer <= 0) {
                        int[] zone = Config.CHICKEN_SPAWN_ZONES[(int)(Math.random() * Config.CHICKEN_SPAWN_ZONES.length)];
                        int zoneX = zone[0];
                        int zoneY = zone[1];
                        int zoneSize = zone[2];
                        
                        int x = zoneX + (int)(Math.random() * zoneSize) - zoneSize/2;
                        int y = zoneY + (int)(Math.random() * zoneSize) - zoneSize/2;
                        chicken.x = x;
                        chicken.y = y;
                        chicken.hp = Config.CHICKEN_HP;
                        chicken.isIdle = true;
                        chicken.isHit = false;
                        chicken.isMoving = false;
                        chicken.currentFrame = 0;
                        chicken.respawnTimer = Config.CHICKEN_RESPAWN_TIME * 60;
                        System.out.println("Chicken " + chicken.id + " respawned at " + x + ", " + y);
                        broadcastChickenUpdates();
                    }
                }
            }
        }
    }
    
    public void broadcastChickenUpdates() {
        for (ChickenData chicken : chickens.values()) {
            if (chicken != null) {
                NetworkMessage message = new NetworkMessage(NetworkMessage.CHICKEN_UPDATE, "", chicken, messageCounter.incrementAndGet());
                broadcastToAll(message);
            }
        }
    }

    private void cleanupDisconnectedClients() {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (!entry.getValue().isConnected()) {
                toRemove.add(entry.getKey());
            }
        }
        for (String playerId : toRemove) {
            removeClient(playerId);
            broadcastToAll(new NetworkMessage(NetworkMessage.PLAYER_LEAVE, playerId, playerId, messageCounter.incrementAndGet()));
        }
    }

    public void handlePlayerHit(String playerId, int damage) {
        PlayerData player = players.get(playerId);
        if (player != null && player.hp > 0) {
            player.hp -= damage;
            debugUI.logPlayerHit(playerId, damage);
            debugUI.logMessage("Player " + playerId + " HP: " + player.hp + " (took " + damage + " damage)");
            
            if (player.hp <= 0) {
                player.hp = 0;
                player.isDead = true;
                player.deathTime = System.currentTimeMillis();
                debugUI.logMessage("Player " + playerId + " died!");
                
                for (Map.Entry<String, PlayerData> entry : players.entrySet()) {
                    if (!entry.getKey().equals(playerId)) {
                        entry.getValue().kills++;
                    }
                }
                
                for (Map.Entry<String, PlayerData> entry : players.entrySet()) {
                    broadcastToAll(new NetworkMessage(NetworkMessage.PLAYER_UPDATE, entry.getKey(), entry.getValue(), messageCounter.incrementAndGet()));
                }
                
                executor.submit(() -> {
                    try {
                        Thread.sleep(Config.RESPAWN_TIME * 1000);
                        player.hp = Config.PLAYER_HP;
                        player.isDead = false;
                        player.deathTime = 0;
                        player.x = 200 + (int)(Math.random() * 400);
                        player.y = 200 + (int)(Math.random() * 400);
                        players.put(playerId, player);
                        broadcastToAll(new NetworkMessage(NetworkMessage.PLAYER_UPDATE, playerId, player, messageCounter.incrementAndGet()));
                        debugUI.logMessage("Player " + playerId + " respawned!");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
            
            players.put(playerId, player);
            broadcastToAll(new NetworkMessage(NetworkMessage.PLAYER_UPDATE, playerId, player, messageCounter.incrementAndGet()));
        }
    }

    public static void main(String[] args) {
        GameServer server = new GameServer(8888);
        server.start();
    }
}