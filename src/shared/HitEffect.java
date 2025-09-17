package shared;

import java.awt.*;

public class HitEffect {
    int x, y;
    int life = Config.HIT_FX_LIFE;
    double velocityX, velocityY;
    double gravity = 0.2;
    boolean isFloating = false;

    public HitEffect(int x, int y) {
        this.x = x;
        this.y = y;
        this.velocityX = (Math.random() - 0.5) * 4;
        this.velocityY = -Math.random() * 3 - 1; 
        this.isFloating = true;
    }

    public boolean update() {
        life--;
        
        if (isFloating) {
          
            velocityY += gravity;
            
           
            x += velocityX;
            y += velocityY;
            
         
            if (life <= 5) {
                isFloating = false;
            }
        }
        
        return life > 0;
    }

    public void draw(Graphics2D g2, int camX, int camY) {
        int alpha = (int) (180 * (life / (double) Config.HIT_FX_LIFE));
        
    
        Color color;
        if (life > 8) {
            color = new Color(255, 100, 100, Math.max(0, alpha)); 
        } else if (life > 4) {
            color = new Color(255, 200, 100, Math.max(0, alpha)); 
        } else {
            color = new Color(255, 255, 100, Math.max(0, alpha)); 
        }
        
        g2.setColor(color);
        g2.fillOval(x - camX - 6, y - camY - 6, 12, 12);
        
     
        g2.setColor(new Color(255, 255, 255, Math.max(0, alpha / 2)));
        g2.fillOval(x - camX - 3, y - camY - 3, 6, 6);
    }
}
