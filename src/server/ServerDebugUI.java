package server;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import shared.*;

public class ServerDebugUI extends JFrame {
    private JTextArea logArea;
    private JTable playerTable;
    private JTable messageTable;
    private DefaultTableModel playerTableModel;
    private DefaultTableModel messageTableModel;
    private JLabel statusLabel;
    private JLabel statsLabel;
    private Timer updateTimer;
    private GameServer server;
    private int messageCount = 0;
    private int hitCount = 0;
    private int bulletCount = 0;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private Map<String, Boolean> godModePlayers = new ConcurrentHashMap<>();

    public ServerDebugUI(GameServer server) {
        this.server = server;
        initializeUI();
        startUpdateTimer();
    }

    private void initializeUI() {
        setTitle("Game Server Debug Console");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

       
        JPanel mainPanel = new JPanel(new BorderLayout());
        
       
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Server Status: Running");
        statusLabel.setForeground(Color.GREEN);
        statsLabel = new JLabel("Messages: 0 | Hits: 0 | Bullets: 0 | Players: 0");
        topPanel.add(statusLabel);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(statsLabel);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        
        JPanel centerPanel = new JPanel(new GridLayout(1, 3));
        
     
        JPanel playerPanel = new JPanel(new BorderLayout());
        playerPanel.setBorder(BorderFactory.createTitledBorder("Connected Players"));
        String[] playerColumns = {"Player ID", "Name", "X", "Y", "HP", "Ammo", "Status", "God Mode"};
        playerTableModel = new DefaultTableModel(playerColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        playerTable = new JTable(playerTableModel);
        playerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playerTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    toggleGodMode();
                }
            }
        });
        JScrollPane playerScrollPane = new JScrollPane(playerTable);
        playerPanel.add(playerScrollPane, BorderLayout.CENTER);
        
        centerPanel.add(playerPanel);

    
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBorder(BorderFactory.createTitledBorder("Recent Messages"));
        String[] messageColumns = {"Time", "Type", "Player", "Data"};
        messageTableModel = new DefaultTableModel(messageColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        messageTable = new JTable(messageTableModel);
        messageTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane messageScrollPane = new JScrollPane(messageTable);
        messagePanel.add(messageScrollPane, BorderLayout.CENTER);
        centerPanel.add(messagePanel);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

  
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Server Log"));
        logArea = new JTextArea(8, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        bottomPanel.add(logScrollPane, BorderLayout.CENTER);

   
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton clearLogButton = new JButton("Clear Log");
        JButton refreshButton = new JButton("Refresh");
        JButton kickPlayerButton = new JButton("Kick Selected Player");
        
        clearLogButton.addActionListener(e -> logArea.setText(""));
        refreshButton.addActionListener(e -> updatePlayerTable());
        kickPlayerButton.addActionListener(e -> kickSelectedPlayer());
        
        buttonPanel.add(clearLogButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(kickPlayerButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void startUpdateTimer() {
        updateTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updatePlayerTable();
                updateStats();
            }
        });
        updateTimer.start();
    }

    private void updatePlayerTable() {
        SwingUtilities.invokeLater(() -> {
            playerTableModel.setRowCount(0);
            for (Map.Entry<String, PlayerData> entry : server.players.entrySet()) {
                PlayerData player = entry.getValue();
                String status = player.hp <= 0 ? "DEAD" : "ALIVE";
                boolean godMode = godModePlayers.getOrDefault(player.id, false);
                playerTableModel.addRow(new Object[]{
                    player.id,
                    player.name,
                    String.format("%.1f", player.x),
                    String.format("%.1f", player.y),
                    player.hp,
                    player.ammo,
                    status,
                    godMode ? "ON" : "OFF"
                });
            }
        });
    }


    private void updateStats() {
        SwingUtilities.invokeLater(() -> {
            statsLabel.setText(String.format("Messages: %d | Hits: %d | Bullets: %d | Players: %d", 
                messageCount, hitCount, bulletCount, server.players.size()));
        });
    }

    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = timeFormat.format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void logNetworkMessage(NetworkMessage message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = timeFormat.format(new Date());
            String type = getMessageTypeName(message.type);
            String data = getMessageDataString(message.data);
            
            messageTableModel.insertRow(0, new Object[]{
                timestamp,
                type,
                message.playerId,
                data
            });
            
    
            if (messageTableModel.getRowCount() > 50) {
                messageTableModel.removeRow(50);
            }
            
            messageCount++;
        });
    }

    public void logPlayerHit(String playerId, int damage) {
        hitCount++;
        logMessage("Player " + playerId + " took " + damage + " damage");
    }

    public void logBulletSpawn() {
        bulletCount++;
    }
    

    private String getMessageTypeName(int type) {
        switch (type) {
            case NetworkMessage.PLAYER_JOIN: return "PLAYER_JOIN";
            case NetworkMessage.PLAYER_LEAVE: return "PLAYER_LEAVE";
            case NetworkMessage.PLAYER_UPDATE: return "PLAYER_UPDATE";
            case NetworkMessage.BULLET_SPAWN: return "BULLET_SPAWN";
            case NetworkMessage.PLAYER_HIT: return "PLAYER_HIT";
            case NetworkMessage.PING: return "PING";
            case NetworkMessage.PONG: return "PONG";
            default: return "UNKNOWN";
        }
    }

    private String getMessageDataString(Object data) {
        if (data == null) return "null";
        if (data instanceof PlayerData) {
            PlayerData pd = (PlayerData) data;
            return String.format("Player(%s, %.1f, %.1f, %d, kills:%d)", pd.name, pd.x, pd.y, pd.hp, pd.kills);
        }
        if (data instanceof BulletData) {
            BulletData bd = (BulletData) data;
            return String.format("Bullet(%.1f, %.1f, %.2f)", bd.x, bd.y, bd.angle);
        }
        if (data instanceof String[]) {
            String[] arr = (String[]) data;
            return String.join(", ", arr);
        }
        return data.toString();
    }

    private void kickSelectedPlayer() {
        int selectedRow = playerTable.getSelectedRow();
        if (selectedRow >= 0) {
            String playerId = (String) playerTableModel.getValueAt(selectedRow, 0);

            logMessage("Kicked player: " + playerId);
        }
    }

    public void setServerStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Server Status: " + status);
            statusLabel.setForeground(color);
        });
    }
    
    private void toggleGodMode() {
        int selectedRow = playerTable.getSelectedRow();
        if (selectedRow >= 0) {
            String playerId = (String) playerTableModel.getValueAt(selectedRow, 0);
            String playerName = (String) playerTableModel.getValueAt(selectedRow, 1);
            
            boolean currentGodMode = godModePlayers.getOrDefault(playerId, false);
            godModePlayers.put(playerId, !currentGodMode);
            
            logMessage("Player " + playerName + " god mode: " + (!currentGodMode ? "ON" : "OFF"));
            updatePlayerTable();
        }
    }
    
    public boolean isPlayerInGodMode(String playerId) {
        return godModePlayers.getOrDefault(playerId, false);
    }
}
