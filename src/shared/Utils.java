package shared;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class Utils {
    public static boolean rectHitsCollision(Rectangle2D.Double r, List<Rectangle2D.Double> collisions) {
        for (Rectangle2D.Double c : collisions) {
            if (c.intersects(r))
                return true;
        }
        return false;
    }
    
    public static Point2D.Double findSafeSpawnPosition(int mapWidth, int mapHeight, List<Rectangle2D.Double> collisions) {
        return findSafeSpawnPosition(mapWidth, mapHeight, collisions, null);
    }
    
    public static Point2D.Double findSafeSpawnPosition(int mapWidth, int mapHeight, List<Rectangle2D.Double> collisions, List<Point2D.Double> existingPositions) {
        java.util.Random random = new java.util.Random();
        int minDistance = 150;
        
        for (int attempt = 0; attempt < Config.RESPAWN_ATTEMPTS; attempt++) {
            int[] spawnZone = Config.PLAYER_SPAWN_ZONES[random.nextInt(Config.PLAYER_SPAWN_ZONES.length)];
            int centerX = spawnZone[0];
            int centerY = spawnZone[1];
            int radius = spawnZone[2];
            
            int x = centerX + random.nextInt(radius * 2) - radius;
            int y = centerY + random.nextInt(radius * 2) - radius;
            
            if (x < 0 || x >= mapWidth || y < 0 || y >= mapHeight) {
                continue;
            }
            
            Rectangle2D.Double testRect = new Rectangle2D.Double(x, y, 32, 32); 
            if (!rectHitsCollision(testRect, collisions)) {
                if (existingPositions != null) {
                    boolean tooClose = false;
                    for (Point2D.Double existingPos : existingPositions) {
                        double distance = Math.sqrt(Math.pow(x - existingPos.x, 2) + Math.pow(y - existingPos.y, 2));
                        if (distance < minDistance) {
                            tooClose = true;
                            break;
                        }
                    }
                    if (tooClose) {
                        continue;
                    }
                }
                return new Point2D.Double(x, y);
            }
        }
        
        return new Point2D.Double(mapWidth / 2, mapHeight / 2);
    }
}
