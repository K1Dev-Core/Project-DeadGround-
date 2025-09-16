import java.awt.geom.Rectangle2D;
import java.util.List;

public class Utils {
    public static boolean rectHitsCollision(Rectangle2D.Double r, List<Rectangle2D.Double> collisions) {
        for (Rectangle2D.Double c : collisions) {
            if (c.intersects(r)) return true;
        }
        return false;
    }
}
