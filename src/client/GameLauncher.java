package client;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.sound.sampled.*;

public class GameLauncher extends JFrame {
    private JTextField nameField;
    private JTextField serverField;
    private JTextField portField;
    private JLabel statusLabel;
    private JButton connectButton;
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    
   
    private final String[] characterTypes = {"hitman1_", "manBrown_", "soldier1_","robot1_", "womanGreen_","manOld_","survivor1_","zoimbie1_"};
    private final String[] characterNames = {"Hitman", "Brown Man", "Soldier", "Robot", "Green Woman", "Man Old", "Survivor", "Zombie"};
    private int selectedCharacterIndex = 0;
    private JLabel characterPreviewLabel;
    private JLabel characterNameLabel;
    private JButton prevCharacterButton;
    private JButton nextCharacterButton;
    
    private void playButtonSound() {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("assets/sfx/button.wav"));
            Clip buttonClip = AudioSystem.getClip();
            buttonClip.open(audioInputStream);
            FloatControl gainControl = (FloatControl) buttonClip.getControl(FloatControl.Type.MASTER_GAIN);
       
            buttonClip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public GameLauncher() {
        initializeUI();
        setupEventListeners();
    }
    
    private void initializeUI() {
        setTitle("Project-DeadGround - Online Multiplayer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(1000, 450);
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
            }
        };
        mainPanel.setLayout(null);
        setContentPane(mainPanel);
        
        titleLabel = new JLabel("PROJECT-DEADGROUND");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(new Color(255, 100, 100));
        titleLabel.setBounds(300, 20, 400, 50);
        mainPanel.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel("TACTICAL COMBAT SIMULATOR");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(150, 150, 150));
        subtitleLabel.setBounds(350, 70, 200, 20);
        mainPanel.add(subtitleLabel);
        
        createCharacterSelectionPanel();
        createConnectionPanel();
        createPlayerNamePanel();
        
        statusLabel = new JLabel("Ready to connect...");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setBounds(50, 600, 400, 25);
        mainPanel.add(statusLabel);
    }
    
    private void createCharacterSelectionPanel() {
        JPanel characterPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                
                g2.setColor(new Color(30, 30, 30));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(100, 100, 100));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                
                g2.setColor(new Color(50, 50, 50));
                g2.fillRect(5, 5, getWidth() - 10, getHeight() - 10);
            }
        };
        characterPanel.setLayout(null);
        characterPanel.setBounds(50, 100, 400, 180);
        mainPanel.add(characterPanel);
        
        JLabel characterTitleLabel = new JLabel("SELECT CHARACTER");
        characterTitleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        characterTitleLabel.setForeground(Color.WHITE);
        characterTitleLabel.setBounds(20, 20, 200, 25);
        characterPanel.add(characterTitleLabel);
        
        characterPreviewLabel = new JLabel();
        characterPreviewLabel.setBounds(160, 45, 80, 80);
        characterPreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        characterPreviewLabel.setVerticalAlignment(SwingConstants.CENTER);
        characterPreviewLabel.setOpaque(true);
        characterPreviewLabel.setBackground(new Color(20, 20, 20));
        characterPreviewLabel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2));
        characterPanel.add(characterPreviewLabel);
        
        characterNameLabel = new JLabel(characterNames[selectedCharacterIndex]);
        characterNameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        characterNameLabel.setForeground(new Color(200, 200, 200));
        characterNameLabel.setBounds(20, 130, 360, 25);
        characterNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        characterPanel.add(characterNameLabel);
        
        prevCharacterButton = createStyledButton("<", 20, 40, 40, 40);
        prevCharacterButton.setBounds(60, 55, 40, 40);
        characterPanel.add(prevCharacterButton);
        
        nextCharacterButton = createStyledButton(">", 20, 40, 40, 40);
        nextCharacterButton.setBounds(300, 55, 40, 40);
        characterPanel.add(nextCharacterButton);

        updateCharacterPreview();
    }
    
    private void createConnectionPanel() {
        JPanel connectionPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                
                g2.setColor(new Color(30, 30, 30));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(100, 100, 100));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                
                g2.setColor(new Color(50, 50, 50));
                g2.fillRect(5, 5, getWidth() - 10, getHeight() - 10);
            }
        };
        connectionPanel.setLayout(null);
        connectionPanel.setBounds(500, 100, 400, 180);
        mainPanel.add(connectionPanel);
        
        JLabel connectionTitleLabel = new JLabel("SERVER CONNECTION");
        connectionTitleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        connectionTitleLabel.setForeground(Color.WHITE);
        connectionTitleLabel.setBounds(20, 20, 200, 25);
        connectionPanel.add(connectionTitleLabel);
        
        JLabel serverLabel = new JLabel("IP:");
        serverLabel.setFont(new Font("Arial", Font.BOLD, 14));
        serverLabel.setForeground(Color.WHITE);
        serverLabel.setBounds(20, 60, 30, 20);
        connectionPanel.add(serverLabel);
        
        serverField = new JTextField("localhost");
        serverField.setFont(new Font("Arial", Font.PLAIN, 16));
        serverField.setForeground(Color.BLACK);
        serverField.setBackground(Color.WHITE);
        serverField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        serverField.setBounds(60, 55, 150, 30);
        connectionPanel.add(serverField);
        
        JLabel portLabel = new JLabel("PORT:");
        portLabel.setFont(new Font("Arial", Font.BOLD, 14));
        portLabel.setForeground(Color.WHITE);
        portLabel.setBounds(230, 60, 50, 20);
        connectionPanel.add(portLabel);
        
        portField = new JTextField("8888");
        portField.setFont(new Font("Arial", Font.PLAIN, 16));
        portField.setForeground(Color.BLACK);
        portField.setBackground(Color.WHITE);
        portField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        portField.setBounds(280, 55, 80, 30);
        connectionPanel.add(portField);
        
        connectButton = createStyledButton("JOIN GAME", 16, 200, 40, 200);
        connectButton.setBounds(100, 130, 200, 40);
        connectionPanel.add(connectButton);
    }
    
    private void createPlayerNamePanel() {
        JPanel namePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                
                g2.setColor(new Color(30, 30, 30));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(100, 100, 100));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                
                g2.setColor(new Color(50, 50, 50));
                g2.fillRect(5, 5, getWidth() - 10, getHeight() - 10);
            }
        };
        namePanel.setLayout(null);
        namePanel.setBounds(50, 300, 400, 80);
        mainPanel.add(namePanel);
        
        JLabel nameTitleLabel = new JLabel("PLAYER NAME");
        nameTitleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        nameTitleLabel.setForeground(Color.WHITE);
        nameTitleLabel.setBounds(20, 20, 150, 25);
        namePanel.add(nameTitleLabel);
        
        String[] names = {"Player", "Warrior", "Hunter", "Sniper", "Soldier", "Commando", "Ranger", "Fighter"};
        String randomName = names[(int)(Math.random() * names.length)] + (int)(Math.random() * 999);
        
        nameField = new JTextField(randomName);
        nameField.setFont(new Font("Arial", Font.PLAIN, 16));
        nameField.setForeground(Color.BLACK);
        nameField.setBackground(Color.WHITE);
        nameField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        nameField.setBounds(20, 45, 360, 30);
        namePanel.add(nameField);
    }
    
    private JButton createStyledButton(String text, int fontSize, int width, int height) {
        return createStyledButton(text, fontSize, width, height, width);
    }
    
    private JButton createStyledButton(String text, int fontSize, int width, int height, int actualWidth) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                
                if (getModel().isPressed()) {
                    g2.setColor(Color.GRAY);
                } else if (getModel().isRollover()) {
                    g2.setColor(Color.DARK_GRAY);
                } else {
                    g2.setColor(Color.BLACK);
                }
                
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, fontSize));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 2;
                g2.drawString(text, x, y);
            }
        };
        button.setBounds(0, 0, actualWidth, height);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        return button;
    }
    
    private void selectCharacter(int index) {
        selectedCharacterIndex = index;
        updateCharacterPreview();
    }
    
    private void updateCharacterPreview() {
        try {
            String imagePath = "assets/player/" + characterTypes[selectedCharacterIndex] + "stand.png";
            ImageIcon icon = new ImageIcon(ImageIO.read(new File(imagePath)));
            Image scaledImage = icon.getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH);
            characterPreviewLabel.setIcon(new ImageIcon(scaledImage));
            characterNameLabel.setText(characterNames[selectedCharacterIndex]);
        } catch (Exception e) {
            characterPreviewLabel.setIcon(null);
            characterPreviewLabel.setText("?");
        }
    }
    
    private void setupEventListeners() {
        connectButton.addActionListener(e -> {
            playButtonSound();
            connectToServer();
        });
        
       
        prevCharacterButton.addActionListener(e -> {
            playButtonSound();
            selectedCharacterIndex = (selectedCharacterIndex - 1 + characterTypes.length) % characterTypes.length;
            updateCharacterPreview();
        });
        
        nextCharacterButton.addActionListener(e -> {
            playButtonSound();
            selectedCharacterIndex = (selectedCharacterIndex + 1) % characterTypes.length;
            updateCharacterPreview();
        });
        
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    playButtonSound();
                    connectToServer();
                }
            }
        });
        
        serverField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    playButtonSound();
                    connectToServer();
                }
            }
        });
        
        portField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    playButtonSound();
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
                        String selectedCharacter = characterTypes[selectedCharacterIndex];
                        ClientGamePanel gamePanel = new ClientGamePanel(playerName, playerId, serverAddress, selectedCharacter);
                        JFrame gameFrame = new JFrame("Project-DeadGround - " + playerName);
                        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        gameFrame.setResizable(false);
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
