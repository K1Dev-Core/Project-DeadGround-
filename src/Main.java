import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame f = new JFrame("Top-Down Shooter");
                GamePanel panel = new GamePanel();
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setContentPane(panel);
                f.pack();
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
