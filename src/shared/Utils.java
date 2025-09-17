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
        java.util.Random random = new java.util.Random();
        
        for (int attempt = 0; attempt < Config.RESPAWN_ATTEMPTS; attempt++) {
          
            int x = random.nextInt(mapWidth - Config.RESPAWN_SAFE_DISTANCE * 2) + Config.RESPAWN_SAFE_DISTANCE;
            int y = random.nextInt(mapHeight - Config.RESPAWN_SAFE_DISTANCE * 2) + Config.RESPAWN_SAFE_DISTANCE;
            
       
            Rectangle2D.Double testRect = new Rectangle2D.Double(x, y, 32, 32); 
            if (!rectHitsCollision(testRect, collisions)) {
                return new Point2D.Double(x, y);
            }
        }
        
     
        return new Point2D.Double(mapWidth / 2, mapHeight / 2);
    }
}
