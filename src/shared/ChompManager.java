package shared;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChompManager {
    private List<Chomp> chomps;
    private int[] respawnTimers;
    private Random random;
    
    public ChompManager(List<Rectangle2D.Double> collisions) {
        this.chomps = new ArrayList<>();
        this.respawnTimers = new int[Config.CHOMP_SPAWN_ZONES.length];
        this.random = new Random();
        
        for (int i = 0; i < Config.CHOMP_SPAWN_ZONES.length; i++) {
            respawnTimers[i] = 0;
        }
    }
    
    public void setPlayers(List<IPlayer> players) {
        for (Chomp chomp : chomps) {
            // Players are passed to update method
        }
    }
    
    public void update(List<Rectangle2D.Double> collisions, List<IPlayer> players) {
        for (int i = chomps.size() - 1; i >= 0; i--) {
            Chomp chomp = chomps.get(i);
            chomp.update(collisions, players, chomps);
            
            if (!chomp.alive) {
                chomps.remove(i);
            }
        }
        
        for (int i = 0; i < chomps.size(); i++) {
            Chomp chomp = chomps.get(i);
            keepChompInZone(chomp, i, collisions);
        }
        
        if (chomps.size() < Config.MAX_TOTAL_CHOMPS) {
            for (int i = 0; i < Config.CHOMP_SPAWN_ZONES.length; i++) {
                int[] zone = Config.CHOMP_SPAWN_ZONES[i];
                int x = zone[0];
                int y = zone[1];
                int radius = zone[2];
                int maxChompsInZone = zone[3];
                
                int chompsInZone = 0;
                for (Chomp chomp : chomps) {
                    if (chomp.alive) {
                        double dx = chomp.x - x;
                        double dy = chomp.y - y;
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        if (distance <= radius) {
                            chompsInZone++;
                        }
                    }
                }
                
                if (chompsInZone < maxChompsInZone) {
                    if (respawnTimers[i] <= 0) {
                        spawnChompInZone(i, collisions);
                        respawnTimers[i] = Config.CHOMP_RESPAWN_TIME;
                    } else {
                        respawnTimers[i]--;
                    }
                }
            }
        }
    }
    
    private void keepChompInZone(Chomp chomp, int zoneIndex, List<Rectangle2D.Double> collisions) {
        if (zoneIndex >= Config.CHOMP_SPAWN_ZONES.length) return;
        
        int[] zone = Config.CHOMP_SPAWN_ZONES[zoneIndex];
        int centerX = zone[0];
        int centerY = zone[1];
        int radius = zone[2];
        
        double dx = chomp.x - centerX;
        double dy = chomp.y - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > radius) {
            double moveX = (dx / distance) * 2;
            double moveY = (dy / distance) * 2;
            
            int newX = (int) (chomp.x - moveX);
            int newY = (int) (chomp.y - moveY);
            
            Rectangle2D.Double testRect = new Rectangle2D.Double(newX, newY, 32, 32);
            if (collisions == null || !Utils.rectHitsCollision(testRect, collisions)) {
                chomp.x = newX;
                chomp.y = newY;
            }
        }
    }
    
    private void spawnChompInZone(int zoneIndex, List<Rectangle2D.Double> collisions) {
        int[] zone = Config.CHOMP_SPAWN_ZONES[zoneIndex];
        int centerX = zone[0];
        int centerY = zone[1];
        int radius = zone[2];
        
        for (int attempts = 0; attempts < 10; attempts++) {
            int x = centerX + random.nextInt(radius * 2) - radius;
            int y = centerY + random.nextInt(radius * 2) - radius;
            
            Rectangle2D.Double testRect = new Rectangle2D.Double(x, y, 32, 32);
            if (!Utils.rectHitsCollision(testRect, collisions)) {
                chomps.add(new Chomp(x, y));
                System.out.println("Chomp spawned in zone " + zoneIndex + " at " + x + "," + y);
                break;
            }
        }
    }
    
    public void render(Graphics2D g2, int camX, int camY) {
        List<Chomp> chompsCopy = new ArrayList<>(chomps);
        for (Chomp chomp : chompsCopy) {
            chomp.render(g2, camX, camY);
        }
    }
    
    public List<Chomp> getChomps() {
        return chomps;
    }
}
