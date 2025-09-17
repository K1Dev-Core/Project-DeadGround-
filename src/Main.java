import javax.swing.*;
import client.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame f = new JFrame("Top-Down Shooter Online");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setLocationRelativeTo(null);

                // แสดงหน้าจอใส่ชื่อผู้เล่น
                String playerName = PlayerNameDialog.showDialog(f);
                if (playerName == null) {
                    System.exit(0);
                    return;
                }

                // สร้าง ID สำหรับผู้เล่น
                String playerId = "player_" + System.currentTimeMillis();

                // เริ่มเกม
                ClientGamePanel panel = new ClientGamePanel(playerName, playerId);
                f.setContentPane(panel);
                f.pack();
                f.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error starting game: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
