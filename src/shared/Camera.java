package shared;

public class Camera {
    public int camX = 0, camY = 0;

    public void centerOn(int targetX, int targetY, int screenW, int screenH,
            int mapPixelW, int mapPixelH) {
        camX = targetX - screenW / 2;
        camY = targetY - screenH / 2;
        clamp(screenW, screenH, mapPixelW, mapPixelH);
    }

    public void clamp(int screenW, int screenH, int mapPixelW, int mapPixelH) {
        camX = Math.max(0, Math.min(camX, mapPixelW - screenW));
        camY = Math.max(0, Math.min(camY, mapPixelH - screenH));
    }
}
