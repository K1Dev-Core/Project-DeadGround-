package server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import shared.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private GameServer server;
    private String playerId;
    private boolean connected = true;
    private BlockingQueue<NetworkMessage> sendQueue;
    private Thread sendThread;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
        this.sendQueue = new LinkedBlockingQueue<>();
        try {
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(5000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

     
            sendThread = new Thread(this::sendMessages, "ClientHandler-Send-" + socket.getInetAddress());
            sendThread.setDaemon(true);
            sendThread.start();

            System.out.println("Client connected: " + socket.getInetAddress());

            while (connected) {
                try {
                    Object received = in.readObject();
                    if (received instanceof NetworkMessage) {
                        NetworkMessage message = (NetworkMessage) received;
                        processMessage(message);
                    } else {
                        System.err.println("Received non-NetworkMessage object: " + received.getClass());
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Invalid data received: " + e.getMessage());
                }
            }
        } catch (java.net.SocketException e) {
            System.err.println("Client disconnected: " + e.getMessage());
        } catch (java.io.EOFException e) {
            System.err.println("Client connection closed");
        } catch (java.net.SocketTimeoutException e) {
         
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void sendMessages() {
        while (connected) {
            try {
                NetworkMessage message = sendQueue.take();
                if (out != null) {
                    synchronized (out) {
                        out.writeObject(message);
                        out.flush();
                    }
                }
            } catch (InterruptedException e) {
                break;
            } catch (IOException e) {
                System.err.println("Could not send data to client: " + e.getMessage());
                connected = false;
                break;
            }
        }
    }

    private void processMessage(NetworkMessage message) {
        try {
      
            if (server.debugUI != null) {
                server.debugUI.logNetworkMessage(message);
            }
            
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
                        if (server.debugUI != null) {
                            server.debugUI.logNetworkMessage(message);
                        }
                    } else {
                        System.err.println("Invalid PLAYER_UPDATE data type: " + message.data.getClass());
                    }
                    break;

                case NetworkMessage.BULLET_SPAWN:
                    if (message.data instanceof BulletData) {
                        BulletData bulletData = (BulletData) message.data;
                        server.broadcastToAll(
                                new NetworkMessage(NetworkMessage.BULLET_SPAWN, bulletData.id, bulletData, server.messageCounter.incrementAndGet()));
                        if (server.debugUI != null) {
                            server.debugUI.logBulletSpawn();
                        }
                        
                        for (ChickenData chicken : server.chickens.values()) {
                            if (chicken != null && chicken.hp > 0) {
                                double distance = Math.sqrt(Math.pow(bulletData.x - chicken.x, 2) + Math.pow(bulletData.y - chicken.y, 2));
                                if (distance < 30) {
                                    chicken.hp -= Config.BULLET_DAMAGE;
                                    chicken.isIdle = false;
                                    chicken.isHit = true;
                                    chicken.currentFrame = 0;
                                    server.debugUI.logMessage("Chicken " + chicken.id + " hit! HP: " + chicken.hp + " Distance: " + distance);
                                    if (chicken.hp <= 0) {
                                        server.debugUI.logMessage("Chicken " + chicken.id + " died!");
                                    }
                                    break;
                                }
                            }
                        }
                    } else {
                        System.err.println("Invalid BULLET_SPAWN data type: " + message.data.getClass());
                    }
                    break;

                case NetworkMessage.PLAYER_HIT:
                    if (message.data instanceof String[]) {
                        String[] hitData = (String[]) message.data;
                        String hitPlayerId = hitData[0];
                        int damage = Integer.parseInt(hitData[1]);
                        server.handlePlayerHit(hitPlayerId, damage);
                    } else {
                        System.err.println("Invalid PLAYER_HIT data type: " + message.data.getClass());
                    }
                    break;

                case NetworkMessage.PING:
                    sendMessage(new NetworkMessage(NetworkMessage.PONG, "", "pong", message.sequence));
                    break;

            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendMessage(NetworkMessage message) {
        if (connected && !sendQueue.offer(message)) {
            System.err.println("Send queue full, dropping message");
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    private void cleanup() {
        connected = false;
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore
        }

        if (playerId != null) {
            // Broadcast 
            server.broadcastToAll(new NetworkMessage(NetworkMessage.PLAYER_LEAVE, playerId, playerId, server.messageCounter.incrementAndGet()));
            server.removeClient(playerId);
            System.out.println("Player left game: " + playerId);
        }
    }
}