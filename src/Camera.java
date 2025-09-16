public class Camera {
    public int camX = 0, camY = 0;
    public double scale = 1.0;

    public void centerOn(int targetX, int targetY, int screenW, int screenH,
                         int mapPixelW, int mapPixelH) {
        camX = targetX - (int)(screenW / (2 * scale));
        camY = targetY - (int)(screenH / (2 * scale));
        clamp(screenW, screenH, mapPixelW, mapPixelH);
    }

    public void zoom(double delta, int mouseX, int mouseY,
                     int screenW, int screenH) {
        double old = scale;
        if (delta < 0) scale = Math.min(Config.MAX_SCALE, scale + 0.1);
        else scale = Math.max(Config.MIN_SCALE, scale - 0.1);

        double px = (camX + mouseX / old);
        double py = (camY + mouseY / old);
        camX = (int)(px - mouseX / scale);
        camY = (int)(py - mouseY / scale);
    }

    public void clamp(int screenW, int screenH, int mapPixelW, int mapPixelH) {
        int viewW = (int)(screenW / scale);
        int viewH = (int)(screenH / scale);
        camX = Math.max(0, Math.min(camX, mapPixelW - viewW));
        camY = Math.max(0, Math.min(camY, mapPixelH - viewH));
    }
}
