package client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.Socket;
import javax.swing.*;

public class GameLauncher extends JFrame {
    private JTextField nameField;
    private JTextField serverField;
    private JTextField portField;
    private JLabel statusLabel;
    private JButton connectButton;
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    
    public GameLauncher() {
        initializeUI();
        setupEventListeners();
    }
    
    private void initializeUI() {
        setTitle("Project-DeadGround - Online Multiplayer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                
                g2.setColor(new Color(20, 20, 20));
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                g2.setColor(new Color(40, 40, 40));
                for (int x = 0; x < getWidth(); x += 40) {
                    g2.drawLine(x, 0, x, getHeight());
                }
                for (int y = 0; y < getHeight(); y += 40) {
                    g2.drawLine(0, y, getWidth(), y);
                }
                
                g2.setColor(new Color(30, 30, 30));
                g2.fillRect(0, 0, 60, getHeight());
                g2.fillRect(getWidth() - 60, 0, 60, getHeight());
            }
        };
        mainPanel.setLayout(null);
        setContentPane(mainPanel);
        
        titleLabel = new JLabel("DeadGround");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 30));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBounds(300, 80, 200, 60);
        mainPanel.add(titleLabel);
        
        subtitleLabel = new JLabel("ONLINE MULTIPLAYER SHOOTER");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(180, 180, 180));
        subtitleLabel.setBounds(280, 140, 240, 20);
        mainPanel.add(subtitleLabel);
        
        JPanel controlPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                
                g2.setColor(new Color(50, 50, 50));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(100, 100, 100));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        controlPanel.setLayout(null);
        controlPanel.setBounds(200, 200, 400, 200);
        mainPanel.add(controlPanel);
        
        JLabel serverLabel = new JLabel("SERVER IP:");
        serverLabel.setFont(new Font("Arial", Font.BOLD, 12));
        serverLabel.setForeground(Color.WHITE);
        serverLabel.setBounds(20, 20, 80, 20);
        controlPanel.add(serverLabel);
        
        serverField = new JTextField("localhost");
        serverField.setFont(new Font("Arial", Font.PLAIN, 14));
        serverField.setForeground(Color.WHITE);
        serverField.setBackground(new Color(30, 30, 30));
        serverField.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2));
        serverField.setBounds(20, 45, 150, 30);
        controlPanel.add(serverField);
        
        JLabel portLabel = new JLabel("PORT:");
        portLabel.setFont(new Font("Arial", Font.BOLD, 12));
        portLabel.setForeground(Color.WHITE);
        portLabel.setBounds(200, 20, 80, 20);
        controlPanel.add(portLabel);
        
        portField = new JTextField("8888");
        portField.setFont(new Font("Arial", Font.PLAIN, 14));
        portField.setForeground(Color.WHITE);
        portField.setBackground(new Color(30, 30, 30));
        portField.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2));
        portField.setBounds(200, 45, 100, 30);
        controlPanel.add(portField);
        
        JLabel nameLabel = new JLabel("PLAYER NAME:");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setBounds(20, 90, 120, 20);
        controlPanel.add(nameLabel);
        
        String[] names = {"Player", "Warrior", "Hunter", "Sniper", "Soldier", "Commando", "Ranger", "Fighter"};
        String randomName = names[(int)(Math.random() * names.length)] + (int)(Math.random() * 999);
        
        nameField = new JTextField(randomName);
        nameField.setFont(new Font("Arial", Font.PLAIN, 16));
        nameField.setForeground(Color.WHITE);
        nameField.setBackground(new Color(30, 30, 30));
        nameField.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2));
        nameField.setBounds(20, 115, 360, 35);
        controlPanel.add(nameField);
        
        connectButton = new JButton("JOIN GAME") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                
                if (getModel().isPressed()) {
                    g2.setColor(new Color(40, 40, 40));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(60, 60, 60));
                } else {
                    g2.setColor(new Color(50, 50, 50));
                }
                
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 16));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth("JOIN GAME")) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 2;
                g2.drawString("JOIN GAME", x, y);
            }
        };
        connectButton.setBounds(20, 160, 360, 40);
        connectButton.setFocusPainted(false);
        connectButton.setBorderPainted(false);
        connectButton.setContentAreaFilled(false);
        controlPanel.add(connectButton);
        
        statusLabel = new JLabel("Ready to connect...");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(150, 150, 150));
        statusLabel.setBounds(20, 520, 400, 20);
        mainPanel.add(statusLabel);
    }
    
    private void setupEventListeners() {
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectToServer();
            }
        });
        
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    connectToServer();
                }
            }
        });
        
        serverField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    connectToServer();
                }
            }
        });
        
        portField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    connectToServer();
                }
            }
        });
    }
    
    private void connectToServer() {
        String playerName = nameField.getText().trim();
        String serverAddress = serverField.getText().trim();
        String portText = portField.getText().trim();
        
        if (playerName.isEmpty()) {
            showError("Please enter a player name!");
            return;
        }
        
        if (serverAddress.isEmpty()) {
            showError("Please enter server address!");
            return;
        }
        
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            showError("Invalid port number!");
            return;
        }
        
        connectButton.setEnabled(false);
        statusLabel.setText("Connecting to server...");
        statusLabel.setForeground(new Color(255, 255, 0));
        
        SwingUtilities.invokeLater(() -> {
            try {
            
                Socket testSocket = new Socket(serverAddress, port);
                testSocket.close();
                
            
                statusLabel.setText("Connected! Starting game...");
                statusLabel.setForeground(new Color(0, 255, 0));
                
             
                SwingUtilities.invokeLater(() -> {
                    setVisible(false);
                    dispose();
                    
                    try {
                        String playerId = "player_" + System.currentTimeMillis();
                        ClientGamePanel gamePanel = new ClientGamePanel(playerName, playerId, serverAddress);
                        JFrame gameFrame = new JFrame("Project-DeadGround - " + playerName);
                        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        gameFrame.add(gamePanel);
                        gameFrame.pack();
                        gameFrame.setLocationRelativeTo(null);
                        gameFrame.setVisible(true);
                        
                    } catch (Exception ex) {
                        showError("Failed to start game: " + ex.getMessage());
                        setVisible(true);
                        connectButton.setEnabled(true);
                    }
                });
                
            } catch (IOException e) {
                showError("Connection failed: " + e.getMessage());
                connectButton.setEnabled(true);
            }
        });
    }
    
    private void showError(String message) {
        statusLabel.setText("ERROR: " + message);
        statusLabel.setForeground(new Color(255, 100, 100));
        
   
        JOptionPane.showMessageDialog(this, message, "Connection Error", JOptionPane.ERROR_MESSAGE);
    }
    
    public static void main(String[] args) {

        
        SwingUtilities.invokeLater(() -> {
            new GameLauncher().setVisible(true);
        });
    }
}
