package shared;

public class Config {
    public static final int FPS = 60;

    public static final int PLAYER_SPEED = 2;
    public static final int PLAYER_HP = 100;
    public static final int PLAYER_SHOOT_DELAY = 60;
    public static final int MAX_AMMO = 20;
    public static final int RELOAD_TIME = 300;

    public static final int BOT_SPEED = 1;
    public static final int BOT_HP = 100;

    public static final int BULLET_SPEED = 10;
    public static final int BULLET_DAMAGE = 10;
    public static final int BULLET_RANGE = 500;

    public static final int HIT_FX_LIFE = 12;
    

    public static boolean SMOOTH_MOVEMENT = true;
    public static int RENDER_DISTANCE = 2;
    
    public static int NETWORK_UPDATE_RATE = 1;
    public static double PLAYER_LERP_FACTOR = 0.8;
    public static int RESPAWN_TIME = 10;
    public static int RESPAWN_SAFE_DISTANCE = 100;
    public static int RESPAWN_ATTEMPTS = 50;
    public static int CHICKEN_HP = 80;
    public static int CHICKEN_SPAWN_COUNT = 15;
    public static int CHICKEN_RESPAWN_TIME = 300;
    public static int CHICKEN_MOVEMENT_SPEED = 1;
    public static int CHICKEN_HEAL_AMOUNT = 30;
    
    public static int[][] CHICKEN_SPAWN_ZONES = {
        {1000, 1200, 200},
    
        {1500, 600, 180},
        {800, 1800, 160},
        {1323, 1700, 120}
    };
}
