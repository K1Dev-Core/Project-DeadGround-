package shared;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ZombieManager {
    public List<Zombie> zombies;
    private List<Rectangle2D.Double> collisions;
    private List<IPlayer> players;
    private int[] respawnTimers;
    private Random random;
    
    public ZombieManager(List<Rectangle2D.Double> collisions) {
        this.collisions = collisions;
        this.zombies = new ArrayList<>();
        this.random = new Random();
        this.respawnTimers = new int[Config.ZOMBIE_SPAWN_ZONES.length];
        
      
        for (int i = 0; i < respawnTimers.length; i++) {
            respawnTimers[i] = random.nextInt(Config.ZOMBIE_RESPAWN_TIME);
        }
    }
    
    public void update() {
   
        for (int i = zombies.size() - 1; i >= 0; i--) {
            Zombie zombie = zombies.get(i);
            zombie.update(collisions, players, zombies);
            
            if (!zombie.alive) {
                zombies.remove(i);
            } else {
                keepZombieInZone(zombie);
            }
        }
        

        if (zombies.size() < Config.MAX_TOTAL_ZOMBIES) {
            for (int zoneIndex = 0; zoneIndex < Config.ZOMBIE_SPAWN_ZONES.length; zoneIndex++) {
                int[] zone = Config.ZOMBIE_SPAWN_ZONES[zoneIndex];
                int centerX = zone[0];
                int centerY = zone[1];
                int radius = zone[2];
                int maxZombiesInZone = zone[3];
                
              
                int zombiesInZone = 0;
                for (Zombie zombie : zombies) {
                    double dx = zombie.x - centerX;
                    double dy = zombie.y - centerY;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance <= radius) {
                        zombiesInZone++;
                    }
                }
                
              
                if (zombiesInZone < maxZombiesInZone) {
                    respawnTimers[zoneIndex]--;
                    if (respawnTimers[zoneIndex] <= 0) {
                        spawnZombieInZone(zoneIndex);
                        respawnTimers[zoneIndex] = Config.ZOMBIE_RESPAWN_TIME;
                    }
                }
            }
        }
    }
    
    private void keepZombieInZone(Zombie zombie) {
     
        for (int[] zone : Config.ZOMBIE_SPAWN_ZONES) {
            int centerX = zone[0];
            int centerY = zone[1];
            int radius = zone[2];
            
            double dx = zombie.x - centerX;
            double dy = zombie.y - centerY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance <= radius) {
                return; 
            }
        }
        
  
        int[] nearestZone = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (int[] zone : Config.ZOMBIE_SPAWN_ZONES) {
            int centerX = zone[0];
            int centerY = zone[1];
            int radius = zone[2];
            
            double dx = zombie.x - centerX;
            double dy = zombie.y - centerY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestZone = zone;
            }
        }
        
        if (nearestZone != null) {
            int centerX = nearestZone[0];
            int centerY = nearestZone[1];
            
            double dx = centerX - zombie.x;
            double dy = centerY - zombie.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance > 0) {
                double moveX = (dx / distance) * 2;
                double moveY = (dy / distance) * 2;
                
                int newX = (int) (zombie.x + moveX);
                int newY = (int) (zombie.y + moveY);
                
                Rectangle2D.Double testRect = new Rectangle2D.Double(newX, newY, 80, 80);
                if (!Utils.rectHitsCollision(testRect, collisions)) {
                    zombie.x = newX;
                    zombie.y = newY;
                }
            }
        }
    }
    
    private void spawnZombieInZone(int zoneIndex) {
        int[] zone = Config.ZOMBIE_SPAWN_ZONES[zoneIndex];
        int centerX = zone[0];
        int centerY = zone[1];
        int radius = zone[2];
        
      
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = centerX + random.nextInt(radius * 2) - radius;
            int y = centerY + random.nextInt(radius * 2) - radius;
            
            Rectangle2D.Double testRect = new Rectangle2D.Double(x, y, 80, 80);
            if (!Utils.rectHitsCollision(testRect, collisions)) {
                zombies.add(new Zombie(x, y));
                System.out.println("Zombie spawned in zone " + zoneIndex + " at " + x + "," + y);
                break;
            }
        }
    }
    
    public void render(Graphics2D g2, int camX, int camY) {
        ArrayList<Zombie> zombiesCopy = new ArrayList<>(zombies);
        for (Zombie zombie : zombiesCopy) {
            if (zombie != null && zombie.alive) {
                zombie.render(g2, camX, camY);
            }
        }
    }
    
    public void setPlayers(List<IPlayer> players) {
        this.players = players;
    }
    
    public List<Zombie> getZombies() {
        return zombies;
    }
}
