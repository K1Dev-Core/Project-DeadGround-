package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PlayerNameDialog extends JDialog {
    private JTextField nameField;
    private JButton joinButton;
    private String playerName;
    private boolean joined = false;

    public PlayerNameDialog(JFrame parent) {
        super(parent, "Join Game", true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setSize(400, 200);
        setLocationRelativeTo(parent);

        initComponents();
        layoutComponents();
        setupEventHandlers();
    }

    private void initComponents() {
        nameField = new JTextField(20);
        nameField.setFont(new Font("Tahoma", Font.PLAIN, 16));
        nameField.setHorizontalAlignment(JTextField.CENTER);

        joinButton = new JButton("Join Game");
        joinButton.setFont(new Font("Tahoma", Font.BOLD, 14));
        joinButton.setPreferredSize(new Dimension(120, 35));
        joinButton.setBackground(new Color(0, 150, 0));
        joinButton.setForeground(Color.WHITE);
        joinButton.setFocusPainted(false);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        
        JLabel titleLabel = new JLabel("Enter your player name", JLabel.CENTER);
        titleLabel.setFont(new Font("Tahoma", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

     
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(nameField);

   
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(joinButton);

   
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(inputPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        add(mainPanel, BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        joinButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(PlayerNameDialog.this,
                            "Please enter a player name", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (name.length() > 20) {
                    JOptionPane.showMessageDialog(PlayerNameDialog.this,
                            "Player name must not exceed 20 characters", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                playerName = name;
                joined = true;
                dispose();
            }
        });

        nameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                joinButton.doClick();
            }
        });
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean hasJoined() {
        return joined;
    }

    public static String showDialog(JFrame parent) {
        PlayerNameDialog dialog = new PlayerNameDialog(parent);
        dialog.setVisible(true);
        return dialog.hasJoined() ? dialog.getPlayerName() : null;
    }
}
