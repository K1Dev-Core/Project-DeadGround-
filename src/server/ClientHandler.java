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
        switch (message.type) {
            case NetworkMessage.PLAYER_JOIN:
                PlayerData playerData = (PlayerData) message.data;
                this.playerId = playerData.id;
                server.addClient(playerId, this);
                server.addPlayer(playerData);
                System.out.println("Player joined: " + playerData.name + " (ID: " + playerId + ")");
                break;

            case NetworkMessage.PLAYER_UPDATE:
                PlayerData updateData = (PlayerData) message.data;
                server.updatePlayer(updateData);
                break;

            case NetworkMessage.BULLET_SPAWN:
                BulletData bulletData = (BulletData) message.data;
                server.broadcastToAll(new NetworkMessage(NetworkMessage.BULLET_SPAWN, bulletData.id, bulletData));
                break;

            case NetworkMessage.BOT_HIT:
                String[] hitData = (String[]) message.data;
                String botId = hitData[0];
                int damage = Integer.parseInt(hitData[1]);
                server.handleBulletHit(botId, damage);
                break;
        }
    }

    public void sendMessage(NetworkMessage message) {
        if (connected && out != null) {
            try {
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
