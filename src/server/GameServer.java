package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import shared.*;

public class GameServer {
    private ServerSocket serverSocket;
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private Map<String, PlayerData> players = new ConcurrentHashMap<>();
    private Map<String, BotData> bots = new ConcurrentHashMap<>();
    private boolean running = false;
    private int port;
    private Thread gameLoop;

    public GameServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("=== Game Server Started ===");
            System.out.println("Port: " + port);
            System.out.println("Local IP: " + getLocalIPAddress());
            System.out.println("Waiting for connections...");
            System.out.println("================================");

            // Spawn initial bots
            spawnInitialBots();

            // Start game loop
            gameLoop = new Thread(this::runGameLoop);
            gameLoop.start();

            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
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
        broadcastToOthers(playerData.id, new NetworkMessage(NetworkMessage.PLAYER_JOIN, playerData.id, playerData));

        // Send existing players to new player
        ClientHandler newClient = clients.get(playerData.id);
        if (newClient != null) {
            for (PlayerData existingPlayer : players.values()) {
                if (!existingPlayer.id.equals(playerData.id)) {
                    newClient.sendMessage(
                            new NetworkMessage(NetworkMessage.PLAYER_JOIN, existingPlayer.id, existingPlayer));
                }
            }

            // Send existing bots to new player
            for (BotData bot : bots.values()) {
                newClient.sendMessage(new NetworkMessage(NetworkMessage.BOT_SPAWN, "server", bot));
            }
        }
    }

    public void updatePlayer(PlayerData playerData) {
        players.put(playerData.id, playerData);
        broadcastToOthers(playerData.id, new NetworkMessage(NetworkMessage.PLAYER_UPDATE, playerData.id, playerData));
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

    private void spawnInitialBots() {
        for (int i = 0; i < 5; i++) {
            String botId = "bot_" + System.currentTimeMillis() + "_" + i;
            double x = Math.random() * 1000 + 200;
            double y = Math.random() * 800 + 200;
            BotData bot = new BotData(botId, x, y);
            bots.put(botId, bot);
            broadcastToAll(new NetworkMessage(NetworkMessage.BOT_SPAWN, "server", bot));
        }
    }

    private void runGameLoop() {
        long frameTime = 1000L / Config.FPS;
        while (running) {
            long start = System.currentTimeMillis();

            // Update bots
            updateBots();

            long dt = System.currentTimeMillis() - start;
            long sleep = Math.max(2, frameTime - dt);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void updateBots() {
        for (BotData bot : bots.values()) {
            // Find nearest player
            PlayerData nearestPlayer = null;
            double minDistance = Double.MAX_VALUE;

            for (PlayerData player : players.values()) {
                double distance = Math.sqrt(
                        Math.pow(player.x - bot.x, 2) +
                                Math.pow(player.y - bot.y, 2));
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestPlayer = player;
                }
            }

            if (nearestPlayer != null) {
                // Move towards nearest player
                double tx = nearestPlayer.x - bot.x;
                double ty = nearestPlayer.y - bot.y;
                bot.angle = Math.atan2(ty, tx);

                double dx = Math.cos(bot.angle) * Config.BOT_SPEED;
                double dy = Math.sin(bot.angle) * Config.BOT_SPEED;

                bot.x += dx;
                bot.y += dy;

                // Update bot data
                bot.update(bot.x, bot.y, bot.angle, bot.hp);

                // Only send bot updates to clients that are not the server
                try {
                    for (ClientHandler handler : clients.values()) {
                        handler.sendMessage(new NetworkMessage(NetworkMessage.BOT_UPDATE, "server", bot));
                    }
                } catch (Exception e) {
                    System.err.println("Error sending bot update: " + e.getMessage());
                }
            }
        }
    }

    public void handleBulletHit(String botId, int damage) {
        BotData bot = bots.get(botId);
        if (bot != null) {
            bot.hp -= damage;
            if (bot.hp <= 0) {
                bots.remove(botId);
                broadcastToAll(new NetworkMessage(NetworkMessage.BOT_HIT, "server", botId));
                // Spawn new bot
                spawnNewBot();
            }
        }
    }

    private void spawnNewBot() {
        String botId = "bot_" + System.currentTimeMillis();
        double x = Math.random() * 1000 + 200;
        double y = Math.random() * 800 + 200;
        BotData bot = new BotData(botId, x, y);
        bots.put(botId, bot);
        broadcastToAll(new NetworkMessage(NetworkMessage.BOT_SPAWN, "server", bot));
    }

    public static void main(String[] args) {
        GameServer server = new GameServer(8888);
        server.start();
    }
}
