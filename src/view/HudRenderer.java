package view;

import model.GameState;
import model.WaveManager;
import model.building.Room;
import model.entity.Unit;
import model.world.ResourceType;

import java.awt.*;
import java.util.List;

/**
 * Rysuje HUD – zasoby, fale, info o jednostce, log.
 */
public class HudRenderer {
    private static final Color  BG     = new Color(20,20,30,210);
    private static final Color  BORDER = new Color(80,80,100,200);
    private static final Font   FSM    = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font   FMD    = new Font("Monospaced", Font.BOLD,  13);
    private static final Font   FLG    = new Font("Monospaced", Font.BOLD,  15);

    public static void render(Graphics2D g, GameState state, int fps, int W, int H, int tileSize) {
        drawResourceBar(g, state, W);
        drawWaveStatus(g, state, W);
        if (state.getSelectedUnit() != null) drawUnitInfo(g, state, H);
        drawEventLog(g, state, W, H);
        drawDebugInfo(g, state, fps, W, tileSize);
        if (state.getBuildMode() != null) drawBuildMode(g, state, W, H);
    }

    private static void drawResourceBar(Graphics2D g, GameState state, int W) {
        g.setColor(BG); g.fillRect(0,0,W,28);
        g.setColor(BORDER); g.drawLine(0,28,W,28);
        g.setFont(FMD);
        int x = 10;
        for (ResourceType rt : ResourceType.all()) {
            int amount = state.getResource(rt.id);
            g.setColor(rt.color); g.fillOval(x,7,12,12);
            g.setColor(Color.WHITE);
            String txt = rt.label+": "+amount;
            g.drawString(txt, x+16, 20);
            x += g.getFontMetrics().stringWidth(txt)+28;
        }
        g.setColor(new Color(200,200,200));
        g.drawString("Jednostki: "+state.playerUnitCount()+"/"+state.getUnitCap(), x+10, 20);
    }

    private static void drawWaveStatus(Graphics2D g, GameState state, int W) {
        WaveManager wm = state.getWaveManager();
        String text; Color color;
        switch (wm.getState()) {
            case PEACE   -> { text="Spokój. Następna fala za "+(wm.getTicksPeace()/60)+"s"; color=new Color(120,220,120); }
            case WARNING -> { text="!! FALA "+wm.getWaveNumber()+" ZA "+(wm.getTicksToWave()/60)+"s !!"; color=new Color(255,180,0); }
            case ACTIVE  -> { text="ATAK! FALA "+wm.getWaveNumber(); color=new Color(255,60,60); }
            case CLEARED -> { text="Fala "+wm.getWaveNumber()+" odparta!"; color=new Color(100,200,255); }
            default      -> { text=""; color=Color.WHITE; }
        }
        if (!text.isEmpty()) {
            g.setFont(FLG); FontMetrics fm=g.getFontMetrics(); int tw=fm.stringWidth(text);
            g.setColor(new Color(20,20,30,200)); g.fillRoundRect((W-tw)/2-8,33,tw+16,22,6,6);
            g.setColor(color); g.drawString(text,(W-tw)/2,50);
        }
    }

    /**
     * Rysuje panel z informacjami o zaznaczonej jednostce (lewy dół ekranu).
     * Pokazuje: imię, klasę, HP, statystyki VIT/STR/PRE/CHA, XP, stan, pokój.
     */
    private static void drawUnitInfo(Graphics2D g, GameState state, int H) {
        Unit sel = state.getSelectedUnit();
        int bx = 8, by = H - 200, bw = 230, bh = 192;
        g.setColor(BG);     g.fillRoundRect(bx, by, bw, bh, 8, 8);
        g.setColor(BORDER); g.drawRoundRect(bx, by, bw, bh, 8, 8);

        // Imię i klasa
        g.setFont(FLG); g.setColor(sel.getUnitClass().color);
        g.drawString(sel.getName(), bx + 8, by + 20);
        g.setFont(FSM); g.setColor(new Color(180, 180, 200));
        g.drawString(sel.getUnitClass().label, bx + 8, by + 34);

        int y = by + 50;
        g.setFont(FSM); g.setColor(Color.WHITE);

        // Pasek HP
        g.drawString("HP: " + sel.getHp() + "/" + sel.getMaxHp(), bx + 8, y); y += 13;
        float stosunekHp = (float) sel.getHp() / sel.getMaxHp();
        int szerokoscPaska = bw - 16;
        g.setColor(new Color(50, 50, 50)); g.fillRect(bx + 8, y, szerokoscPaska, 5);
        g.setColor(stosunekHp > 0.5f ? new Color(60, 200, 60)
                : stosunekHp > 0.25f ? new Color(220, 180, 0) : new Color(220, 50, 50));
        g.fillRect(bx + 8, y, (int)(szerokoscPaska * stosunekHp), 5); y += 10;

        // Statystyki VIT/STR/PRE/CHA w jednej linii
        g.setColor(new Color(200, 200, 200));
        g.drawString(String.format("VIT:%2d  STR:%2d  PRE:%2d  CHA:%2d",
                sel.getVit(), sel.getStr(), sel.getPre(), sel.getCha()), bx + 8, y); y += 14;

        // XP – pasek
        int xp = sel.getXp(), xpMax = sel.xpForNextTier();
        boolean maxXp = xpMax >= 999999;
        g.drawString("XP: " + (maxXp ? "MAX" : xp + "/" + xpMax), bx + 8, y); y += 13;
        if (!maxXp) {
            g.setColor(new Color(50, 50, 50)); g.fillRect(bx + 8, y, szerokoscPaska, 5);
            g.setColor(new Color(100, 180, 255));
            g.fillRect(bx + 8, y, (int)(szerokoscPaska * Math.min(1f, (float) xp / xpMax)), 5);
        }
        y += 10;

        // Stan i pokój
        g.setColor(new Color(200, 200, 200));
        g.drawString("Stan: " + sel.getState().name().toLowerCase(), bx + 8, y); y += 14;

        Room room = state.getRoomSystem().getRoomAt(sel.getTileX(), sel.getTileY());
        if (room != null) {
            g.setColor(new Color(180, 220, 255));
            g.drawString("Pokój: " + room.getDef().label, bx + 8, y); y += 14;
        }

        // Podpowiedź o awansie
        if (sel.isReadyToPromote()) {
            g.setColor(new Color(255, 210, 30)); g.setFont(FMD);
            g.drawString("★ Gotowy do awansu! (2x klik)", bx + 8, by + bh - 10);
        }
    }

    private static void drawEventLog(Graphics2D g, GameState state, int W, int H) {
        int lines=8,lh=14,bw=340,bh=lines*lh+12,bx=W-bw-8,by=H-bh-8;
        g.setColor(BG); g.fillRoundRect(bx,by,bw,bh,8,8);
        g.setColor(BORDER); g.drawRoundRect(bx,by,bw,bh,8,8);
        g.setFont(FSM);
        List<String> log=state.getEventLog();
        for (int i=0;i<Math.min(lines,log.size());i++) {
            g.setColor(new Color(1f,1f,1f,Math.max(0.2f,1f-i*0.1f)));
            g.drawString(log.get(i), bx+6, by+14+i*lh);
        }
    }

    private static void drawDebugInfo(Graphics2D g, GameState state, int fps, int W, int tileSize) {
        g.setFont(FSM); g.setColor(new Color(180,180,180,180));
        g.drawString("FPS:"+fps+" Zoom:"+tileSize+(state.isPaused()?" [PAUZA]":""), W-130, 14);
    }

    private static void drawBuildMode(Graphics2D g, GameState state, int W, int H) {
        String tid = state.getBuildMode();
        String label = model.world.TileType.get(tid).label;
        String text = "Budowanie: "+label+"  [LPM=1 kafelek  Przeciągnij=obszar  PPM=cofnij  ESC=wyjdź]";
        g.setFont(FMD); FontMetrics fm=g.getFontMetrics(); int tw=fm.stringWidth(text);
        g.setColor(new Color(20,60,120,210)); g.fillRoundRect((W-tw)/2-10,H-36,tw+20,24,6,6);
        g.setColor(new Color(100,180,255)); g.drawString(text,(W-tw)/2,H-18);
    }
}
