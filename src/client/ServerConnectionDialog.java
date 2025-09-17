package client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class ServerConnectionDialog extends JDialog {
    private JTextField serverField;
    private JButton connectButton;
    private String serverHost;
    private boolean connected = false;

    public ServerConnectionDialog(JFrame parent) {
        super(parent, "Connect to Server", true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setSize(400, 250);
        setLocationRelativeTo(parent);

        initComponents();
        layoutComponents();
        setupEventHandlers();
    }

    private void initComponents() {
        serverField = new JTextField("localhost", 20);
        serverField.setFont(new Font("Tahoma", Font.PLAIN, 16));
        serverField.setHorizontalAlignment(JTextField.CENTER);

        connectButton = new JButton("Connect");
        connectButton.setFont(new Font("Tahoma", Font.BOLD, 14));
        connectButton.setPreferredSize(new Dimension(120, 35));
        connectButton.setBackground(new Color(0, 150, 0));
        connectButton.setForeground(Color.WHITE);
        connectButton.setFocusPainted(false);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

       
        JLabel titleLabel = new JLabel("Connect to Game Server", JLabel.CENTER);
        titleLabel.setFont(new Font("Tahoma", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        
        JPanel inputPanel = new JPanel(new FlowLayout());
        JLabel serverLabel = new JLabel("Server IP:");
        serverLabel.setFont(new Font("Tahoma", Font.BOLD, 14));
        inputPanel.add(serverLabel);
        inputPanel.add(serverField);

        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel infoLabel = new JLabel("<html><center>Enter the IP address of the game server<br/>" +
                "For local testing, use 'localhost'<br/>" +
                "For network play, use the server's IP address</center></html>");
        infoLabel.setFont(new Font("Tahoma", Font.PLAIN, 12));
        infoLabel.setHorizontalAlignment(JLabel.CENTER);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        infoPanel.add(infoLabel, BorderLayout.CENTER);

     
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(connectButton);

     
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(inputPanel, BorderLayout.CENTER);
        mainPanel.add(infoPanel, BorderLayout.SOUTH);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        add(mainPanel, BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String host = serverField.getText().trim();
                if (host.isEmpty()) {
                    JOptionPane.showMessageDialog(ServerConnectionDialog.this,
                            "Please enter a server IP address", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                serverHost = host;
                connected = true;
                dispose();
            }
        });

        serverField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectButton.doClick();
            }
        });
    }

    public String getServerHost() {
        return serverHost;
    }

    public boolean hasConnected() {
        return connected;
    }

    public static String showDialog(JFrame parent) {
        ServerConnectionDialog dialog = new ServerConnectionDialog(parent);
        dialog.setVisible(true);
        return dialog.hasConnected() ? dialog.getServerHost() : null;
    }
}
