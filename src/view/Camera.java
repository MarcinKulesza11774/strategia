package view;

import model.world.WorldMap;
import util.GameConfig;

/**
 * Kamera z przewijaniem i zoomem.
 * tileSize (rozmiar kafelka w pikselach) kontroluje zoom.
 */
public class Camera {
    private double camX, camY;
    private int    tileSize;
    private int    minTileSize, maxTileSize, zoomStep;
    private double scrollSpeed;
    private int    viewW, viewH;   // w kafelkach
    private boolean left, right, up, down;

    public Camera(int viewPixelW, int viewPixelH, double startX, double startY) {
        GameConfig cfg = GameConfig.get();
        tileSize    = cfg.tileSize();
        minTileSize = cfg.minTileSize();
        maxTileSize = cfg.maxTileSize();
        zoomStep    = cfg.zoomStep();
        scrollSpeed = cfg.scrollSpeed();
        camX = startX; camY = startY;
        resize(viewPixelW, viewPixelH);
    }

    public void resize(int pw, int ph) {
        viewW = pw / tileSize + 2;
        viewH = ph / tileSize + 2;
    }

    public void tick(double dt) {
        if (left)  camX -= scrollSpeed * dt;
        if (right) camX += scrollSpeed * dt;
        if (up)    camY -= scrollSpeed * dt;
        if (down)  camY += scrollSpeed * dt;
        clamp();
    }

    private void clamp() {
        camX = Math.max(0, Math.min(WorldMap.WIDTH  - viewW, camX));
        camY = Math.max(0, Math.min(WorldMap.HEIGHT - viewH, camY));
    }

    /** Zoom in/out zachowując punkt pod kursorem myszy. */
    public void zoom(int delta, int mouseScreenX, int mouseScreenY, int panelW, int panelH) {
        // Pozycja świata pod kursorem przed zoomem
        double worldX = screenToWorldX(mouseScreenX);
        double worldY = screenToWorldY(mouseScreenY);

        int newSize = tileSize + delta * zoomStep;
        tileSize = Math.max(minTileSize, Math.min(maxTileSize, newSize));
        resize(panelW, panelH);

        // Przesuń kamerę tak żeby ten sam punkt świata był pod kursorem
        camX = worldX - mouseScreenX / (double) tileSize;
        camY = worldY - mouseScreenY / (double) tileSize;
        clamp();
    }

    public double screenToWorldX(int px) { return px / (double) tileSize + camX; }
    public double screenToWorldY(int py) { return py / (double) tileSize + camY; }
    public int    worldToScreenX(double wx) { return (int)((wx - camX) * tileSize); }
    public int    worldToScreenY(double wy) { return (int)((wy - camY) * tileSize); }

    // Konwersja do kafelka (int)
    public int screenToTileX(int px) { return (int) Math.floor(screenToWorldX(px)); }
    public int screenToTileY(int py) { return (int) Math.floor(screenToWorldY(py)); }
    public int tileToScreenX(double tx) { return worldToScreenX(tx); }
    public int tileToScreenY(double ty) { return worldToScreenY(ty); }

    public boolean isVisible(int tx, int ty) {
        return tx >= camX-1 && tx <= camX+viewW+1 && ty >= camY-1 && ty <= camY+viewH+1;
    }

    public int getFirstTileX() { return Math.max(0, (int)camX); }
    public int getFirstTileY() { return Math.max(0, (int)camY); }
    public int getLastTileX()  { return Math.min(WorldMap.WIDTH-1,  (int)(camX+viewW+1)); }
    public int getLastTileY()  { return Math.min(WorldMap.HEIGHT-1, (int)(camY+viewH+1)); }
    public int getTileSize()   { return tileSize; }
    public double getCamX()    { return camX; }
    public double getCamY()    { return camY; }
    public void setCam(double x, double y) { camX=x; camY=y; clamp(); }

    public void setLeft(boolean v)  { left=v; }
    public void setRight(boolean v) { right=v; }
    public void setUp(boolean v)    { up=v; }
    public void setDown(boolean v)  { down=v; }
}
