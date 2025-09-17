package client;

import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import shared.*;

public class NetworkClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ClientGamePanel gamePanel;
    private BlockingQueue<NetworkMessage> messageQueue;
    private boolean connected = false;
    private Thread receiveThread;

    public NetworkClient(ClientGamePanel gamePanel) {
        this.gamePanel = gamePanel;
        this.messageQueue = new LinkedBlockingQueue<>();
    }

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

            receiveThread = new Thread(this::receiveMessages);
            receiveThread.start();

            System.out.println("Connected to server successfully at " + host + ":" + port);
        } catch (IOException e) {
            System.err.println("Could not connect to server at " + host + ":" + port + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null)
                socket.close();
            if (out != null)
                out.close();
            if (in != null)
                in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        while (connected) {
            try {
                NetworkMessage message = (NetworkMessage) in.readObject();
                processMessage(message);
            } catch (IOException | ClassNotFoundException e) {
                if (connected) {
                    System.err.println("Error receiving data: " + e.getMessage());
                    connected = false;
                }
                break;
            }
        }
    }

    private void processMessage(NetworkMessage message) {
        switch (message.type) {
            case NetworkMessage.PLAYER_JOIN:
                PlayerData playerData = (PlayerData) message.data;
                if (!playerData.id.equals(gamePanel.localPlayer.playerId)) {
                    gamePanel.addPlayer(playerData);
                }
                break;

            case NetworkMessage.PLAYER_LEAVE:
                String playerId = (String) message.data;
                gamePanel.removePlayer(playerId);
                break;

            case NetworkMessage.PLAYER_UPDATE:
                PlayerData updateData = (PlayerData) message.data;
                if (!updateData.id.equals(gamePanel.localPlayer.playerId)) {
                    gamePanel.updatePlayer(updateData);
                }
                break;

            case NetworkMessage.BULLET_SPAWN:
                BulletData bulletData = (BulletData) message.data;
                gamePanel.addBullet(bulletData);
                break;

            case NetworkMessage.BOT_SPAWN:
                BotData botData = (BotData) message.data;
                gamePanel.addBot(botData);
                break;

            case NetworkMessage.BOT_UPDATE:
                BotData updateBotData = (BotData) message.data;
                gamePanel.updateBot(updateBotData);
                break;

            case NetworkMessage.BOT_HIT:
                String botId = (String) message.data;
                gamePanel.removeBot(botId);
                break;
        }
    }

    public void sendPlayerJoin(PlayerData playerData) {
        sendMessage(new NetworkMessage(NetworkMessage.PLAYER_JOIN, playerData.id, playerData));
    }

    public void sendPlayerUpdate(PlayerData playerData) {
        sendMessage(new NetworkMessage(NetworkMessage.PLAYER_UPDATE, playerData.id, playerData));
    }

    public void sendBulletSpawn(BulletData bulletData) {
        sendMessage(new NetworkMessage(NetworkMessage.BULLET_SPAWN, bulletData.id, bulletData));
    }

    public void sendBotHit(Bot bot, int damage) {
        // Create a simple data structure for bot hit
        String[] hitData = { bot.id, String.valueOf(damage) };
        sendMessage(new NetworkMessage(NetworkMessage.BOT_HIT, gamePanel.localPlayer.playerId, hitData));
    }

    private void sendMessage(NetworkMessage message) {
        if (connected && out != null) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                System.err.println("Could not send data: " + e.getMessage());
                connected = false;
            }
        }
    }
}
