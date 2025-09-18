package shared;

public class Config {
    public static final int FPS = 120;

    public static final int PLAYER_SPEED = 1;
    public static final int DASH_SPEED = 18;
    public static final int DASH_DISTANCE = 300;
    public static final int DASH_COOLDOWN = 1000;
    public static final int PLAYER_HP = 100;
    public static final int PLAYER_SHOOT_DELAY = 80;
    public static final int MAX_AMMO = 20;
    public static final int RELOAD_TIME = 600;

    public static final int BOT_SPEED = 1;
    public static final int BOT_HP = 100;

    public static final int BULLET_SPEED = 7;
    public static final int BULLET_DAMAGE = 10;
    public static final int BULLET_RANGE = 500;

    public static final int HIT_FX_LIFE = 12;

    public static int RENDER_DISTANCE = 1;
    public static boolean USE_ACCELERATED_GRAPHICS = true;
    public static boolean ENABLE_OPENGL = false;

    public static int NETWORK_UPDATE_RATE = 2;
    public static double PLAYER_LERP_FACTOR = 0.8;
    public static int RESPAWN_TIME = 10;
    public static int RESPAWN_SAFE_DISTANCE = 100;
    public static int RESPAWN_ATTEMPTS = 50;
    public static int CHICKEN_HP = 80;
    public static int CHICKEN_SPAWN_COUNT = 8;
    public static int CHICKEN_RESPAWN_TIME = 300;
    public static int CHICKEN_MOVEMENT_SPEED = 1;
    public static int CHICKEN_HEAL_AMOUNT = 30;

    public static int[][] CHICKEN_SPAWN_ZONES = {
            { 1000, 1200, 200 },

            { 1500, 600, 180 },
            { 800, 1800, 160 },
            { 2524, 2023, 120 }
    };

    public static int WEAPON_PICKUP_RANGE = 50;
    public static int MELEE_DAMAGE = 5;
    public static int MELEE_RANGE = 60;
    public static int MELEE_COOLDOWN = 30;

    public static int[][] WEAPON_SPAWN_POINTS = {
            { 1175, 2028 },
            { 1451, 1184 },
            { 691, 602 },
            { 2649, 818 },
            { 464, 1641 },
            { 2224, 1839 },
            { 1071, 2650 }
    };

}
