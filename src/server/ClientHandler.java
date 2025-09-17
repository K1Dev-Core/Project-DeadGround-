package server;

import java.io.*;
import java.net.*;
import shared.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private GameServer server;
    private String playerId;
    private boolean connected = true;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("Client connected: " + socket.getInetAddress());

            while (connected) {
                NetworkMessage message = (NetworkMessage) in.readObject();
                processMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void processMessage(NetworkMessage message) {
        try {
            switch (message.type) {
                case NetworkMessage.PLAYER_JOIN:
                    if (message.data instanceof PlayerData) {
                        PlayerData playerData = (PlayerData) message.data;
                        this.playerId = playerData.id;
                        server.addClient(playerId, this);
                        server.addPlayer(playerData);
                        System.out.println("Player joined: " + playerData.name + " (ID: " + playerId + ")");
                    } else {
                        System.err.println("Invalid PLAYER_JOIN data type: " + message.data.getClass());
                    }
                    break;

                case NetworkMessage.PLAYER_UPDATE:
                    if (message.data instanceof PlayerData) {
                        PlayerData updateData = (PlayerData) message.data;
                        server.updatePlayer(updateData);
                    } else {
                        System.err.println("Invalid PLAYER_UPDATE data type: " + message.data.getClass());
                    }
                    break;

                case NetworkMessage.BULLET_SPAWN:
                    if (message.data instanceof BulletData) {
                        BulletData bulletData = (BulletData) message.data;
                        server.broadcastToAll(
                                new NetworkMessage(NetworkMessage.BULLET_SPAWN, bulletData.id, bulletData));
                    } else {
                        System.err.println("Invalid BULLET_SPAWN data type: " + message.data.getClass());
                    }
                    break;

                case NetworkMessage.BOT_HIT:
                    if (message.data instanceof String[]) {
                        String[] hitData = (String[]) message.data;
                        String botId = hitData[0];
                        int damage = Integer.parseInt(hitData[1]);
                        server.handleBulletHit(botId, damage);
                    } else {
                        System.err.println("Invalid BOT_HIT data type: " + message.data.getClass());
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendMessage(NetworkMessage message) {
        if (connected && out != null) {
            try {
                // Reset the stream to avoid serialization issues
                out.reset();
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                System.err.println("Could not send data to client: " + e.getMessage());
                connected = false;
            }
        }
    }

    private void cleanup() {
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

        if (playerId != null) {
            server.removeClient(playerId);
            System.out.println("Player left game: " + playerId);
        }
    }
}
