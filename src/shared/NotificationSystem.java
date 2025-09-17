package shared;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NotificationSystem {
    private static List<Notification> notifications = new ArrayList<>();
    private static long maxDisplayTime = 3000;

    public static class Notification {
        public String message;
        public long timestamp;
        public Color color;

        public Notification(String message, Color color) {
            this.message = message;
            this.color = color;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static void addNotification(String message) {
        addNotification(message, Color.WHITE);
    }

    public static void addNotification(String message, Color color) {
        notifications.add(new Notification(message, color));
        if (notifications.size() > 5) {
            notifications.remove(0);
        }
    }

    public static void drawNotifications(Graphics2D g2, int screenWidth, int screenHeight) {
        notifications.removeIf(n -> System.currentTimeMillis() - n.timestamp > maxDisplayTime);

        g2.setFont(new Font("Arial", Font.BOLD, 14));
        int y = 30;

        for (int i = notifications.size() - 1; i >= 0; i--) {
            Notification n = notifications.get(i);

            long age = System.currentTimeMillis() - n.timestamp;
            float alpha = Math.max(0.1f, 1.0f - (float) age / maxDisplayTime);

            Color colorWithAlpha = new Color(n.color.getRed(), n.color.getGreen(), n.color.getBlue(),
                    (int) (alpha * 255));
            g2.setColor(colorWithAlpha);

            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(n.message);
            int padding = 10;


            g2.setColor(Color.WHITE);
            g2.drawString(n.message, screenWidth - textWidth - padding - 10, y);

            y += 25;
        }
    }
}
