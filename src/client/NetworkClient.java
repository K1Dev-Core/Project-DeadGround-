package client;

import java.awt.Color;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import shared.*;

public class NetworkClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ClientGamePanel gamePanel;
    private BlockingQueue<NetworkMessage> messageQueue;
    private boolean connected = false;
    private Thread receiveThread;
    private Thread sendThread;
    private ExecutorService executor;
    private AtomicInteger sequenceCounter = new AtomicInteger(0);
    private long lastPingTime = 0;
    private long ping = 0;

    public NetworkClient(ClientGamePanel gamePanel) {
        this.gamePanel = gamePanel;
        this.messageQueue = new LinkedBlockingQueue<>();
        this.executor = Executors.newFixedThreadPool(4);
    }

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(5000);
            
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

       
            receiveThread = new Thread(this::receiveMessages, "NetworkClient-Receive");
            receiveThread.setDaemon(true);
            receiveThread.start();

           
            sendThread = new Thread(this::sendMessages, "NetworkClient-Send");
            sendThread.setDaemon(true);
            sendThread.start();

         
            executor.submit(this::pingLoop);

            System.out.println("Connected to server successfully at " + host + ":" + port);
        } catch (IOException e) {
            System.err.println("Could not connect to server at " + host + ":" + port + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void disconnect() {
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

        if (executor != null) {
            executor.shutdown();
        }
    }
    
    private void showConnectionError(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                gamePanel, 
                "Connection Error:\n" + message + "\n\nGame will be closed.", 
                title, 
                JOptionPane.ERROR_MESSAGE
            );
            System.exit(0);
        });
    }

    private void receiveMessages() {
        while (connected) {
            try {
                Object received = in.readObject();
                if (received instanceof NetworkMessage) {
                    NetworkMessage message = (NetworkMessage) received;
                    processMessage(message);
                } else {
                    System.err.println("Received non-NetworkMessage object: " + received.getClass());
                }
            } catch (java.net.SocketException e) {
                if (connected) {
                    System.err.println("Connection lost: " + e.getMessage());
                    showConnectionError("Socket Error", "Connection lost: " + e.getMessage());
                    connected = false;
                }
                break;
            } catch (java.io.EOFException e) {
                if (connected) {
                    System.err.println("Connection closed by server");
                    showConnectionError("Connection Closed", "Server closed the connection");
                    connected = false;
                }
                break;
            } catch (java.io.StreamCorruptedException e) {
                System.err.println("Stream corrupted: " + e.getMessage());
                showConnectionError("Stream Error", "Data stream corrupted: " + e.getMessage());
                connected = false;
                break;
            } catch (java.net.SocketTimeoutException e) {
          
                continue;
            } catch (IOException e) {
                if (connected) {
                    System.err.println("Connection lost: " + e.getMessage());
                    showConnectionError("Network Error", "Connection lost: " + e.getMessage());
                    connected = false;
                }
                break;
            } catch (ClassNotFoundException e) {
                System.err.println("Invalid data received: " + e.getMessage());
                showConnectionError("Data Error", "Invalid data received: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void sendMessages() {
        while (connected) {
            try {
                NetworkMessage message = messageQueue.take();
                if (out != null) {
                    synchronized (out) {
                        out.writeObject(message);
                        out.flush();
                    }
                }
            } catch (InterruptedException e) {
                break;
            } catch (IOException e) {
                System.err.println("Could not send data: " + e.getMessage());
                connected = false;
                break;
            }
        }
    }

    private void pingLoop() {
        while (connected) {
            try {
                Thread.sleep(1000);
                if (connected && System.currentTimeMillis() - lastPingTime > 1000) {
                    sendMessage(new NetworkMessage(NetworkMessage.PING, "", "ping"));
                    lastPingTime = System.currentTimeMillis();
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void processMessage(NetworkMessage message) {
        switch (message.type) {
            case NetworkMessage.PLAYER_JOIN:
                if (message.data instanceof PlayerData) {
                    PlayerData playerData = (PlayerData) message.data;
                    if (!playerData.id.equals(gamePanel.localPlayer.playerId)) {
                        gamePanel.addPlayer(playerData);
                    }
                }
                break;

            case NetworkMessage.PLAYER_LEAVE:
                if (message.data instanceof String) {
                    String playerId = (String) message.data;
                    gamePanel.removePlayer(playerId);
                }
                break;

            case NetworkMessage.PLAYER_UPDATE:
                if (message.data instanceof PlayerData) {
                    PlayerData updateData = (PlayerData) message.data;
                    if (updateData.id.equals(gamePanel.localPlayer.playerId)) {
                
                        int oldHp = gamePanel.localPlayer.hp;
                        gamePanel.localPlayer.hp = updateData.hp;
                        
                        if (oldHp != gamePanel.localPlayer.hp) {
                            if (gamePanel.localPlayer.hp <= 0) {
                             
                                for (int i = 0; i < 20; i++) {
                                    gamePanel.effects.add(new HitEffect((int) (gamePanel.localPlayer.x + Math.random() * 64), (int) (gamePanel.localPlayer.y + Math.random() * 64)));
                                }
                                NotificationSystem.addNotification("YOU DIED!", Color.RED);
                              
                                gamePanel.localPlayer.hp = 0;
                            } 
                        }
                    } else {
                        gamePanel.updatePlayer(updateData);
                    }
                }
                break;

            case NetworkMessage.BULLET_SPAWN:
                if (message.data instanceof BulletData) {
                    BulletData bulletData = (BulletData) message.data;
                    gamePanel.addBullet(bulletData);
                }
                break;

            case NetworkMessage.PLAYER_HIT:
                if (message.data instanceof String[]) {
                    String[] hitData = (String[]) message.data;
                    String hitPlayerId = hitData[0];
                    int damage = Integer.parseInt(hitData[1]);
                    gamePanel.hitPlayer(hitPlayerId, damage);
                }
                break;

            case NetworkMessage.PONG:
                ping = System.currentTimeMillis() - lastPingTime;
                break;
        }
    }

    public void sendPlayerJoin(PlayerData playerData) {
        sendMessage(new NetworkMessage(NetworkMessage.PLAYER_JOIN, playerData.id, playerData, sequenceCounter.incrementAndGet()));
    }

    public void sendPlayerUpdate(PlayerData playerData) {
        sendMessage(new NetworkMessage(NetworkMessage.PLAYER_UPDATE, playerData.id, playerData, sequenceCounter.incrementAndGet()));
    }

    public void sendBulletSpawn(BulletData bulletData) {
        sendMessage(new NetworkMessage(NetworkMessage.BULLET_SPAWN, gamePanel.localPlayer.playerId, bulletData, sequenceCounter.incrementAndGet()));
    }
    
    public void sendPlayerHit(String playerId, int damage) {
        String[] hitData = { playerId, String.valueOf(damage) };
        sendMessage(new NetworkMessage(NetworkMessage.PLAYER_HIT, gamePanel.localPlayer.playerId, hitData, sequenceCounter.incrementAndGet()));
    }

    private void sendMessage(NetworkMessage message) {
        if (connected && !messageQueue.offer(message)) {
            System.err.println("Message queue full, dropping message");
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public long getPing() {
        return ping;
    }
}