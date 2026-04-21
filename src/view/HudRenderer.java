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

    private static void drawUnitInfo(Graphics2D g, GameState state, int H) {
        Unit sel = state.getSelectedUnit();
        int bx=8,by=H-175,bw=225,bh=167;
        g.setColor(BG); g.fillRoundRect(bx,by,bw,bh,8,8);
        g.setColor(BORDER); g.drawRoundRect(bx,by,bw,bh,8,8);

        g.setFont(FLG); g.setColor(sel.getUnitClass().color);
        g.drawString(sel.getUnitClass().label+" #"+sel.getId(), bx+8, by+20);

        int y=by+36; g.setFont(FSM); g.setColor(Color.WHITE);
        g.drawString("HP: "+sel.getHp()+"/"+sel.getMaxHp(), bx+8, y); y+=14;
        float ratio=(float)sel.getHp()/sel.getMaxHp();
        int bw2=bw-16;
        g.setColor(new Color(50,50,50)); g.fillRect(bx+8,y,bw2,6);
        g.setColor(ratio>0.5f?new Color(60,200,60):ratio>0.25f?new Color(220,180,0):new Color(220,50,50));
        g.fillRect(bx+8,y,(int)(bw2*ratio),6); y+=10;

        g.setColor(new Color(200,200,200));
        g.drawString("XP: "+sel.getXp()+"/"+sel.xpForNextTier(), bx+8, y); y+=14;
        g.drawString("ATK:"+sel.getEffectiveAttack()+"  DEF:"+sel.getEffectiveDefense(), bx+8, y); y+=14;
        g.drawString("Stan: "+sel.getState(), bx+8, y); y+=14;

        Room room = state.getRoomSystem().getRoomAt(sel.getTileX(), sel.getTileY());
        if (room!=null) {
            g.setColor(new Color(180,220,255));
            g.drawString("Pokój: "+room.getDef().label, bx+8, y); y+=14;
            g.setColor(new Color(150,200,150));
            g.drawString("Buffy: SPD+"+room.getTotalSpeedBuff()+" ATK+"+room.getTotalAttackBuff()+" DEF+"+room.getTotalDefenseBuff(), bx+8, y); y+=14;
        }
        if (sel.canPromote()) {
            g.setColor(new Color(255,210,30)); g.setFont(FMD);
            g.drawString(">>> Gotowy do awansu! <<<", bx+8, by+bh-10);
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
