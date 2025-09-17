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
                debugUI.logMessage("Player " + playerId + " died!");
                executor.submit(() -> {
                    try {
                        Thread.sleep(3000);
                        player.hp = Config.PLAYER_HP;
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