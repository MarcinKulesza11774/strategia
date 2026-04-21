package view;

import model.GameState;
import model.building.Room;
import model.entity.Unit;
import model.world.Tile;
import model.world.TileType;
import model.world.WorldMap;

import java.awt.*;

/**
 * Renderuje widoczny fragment mapy z cullingiem.
 * Obsługuje dynamiczny rozmiar kafelka (zoom).
 */
public class MapRenderer {

    public void render(Graphics2D g, GameState state, Camera cam, Unit selectedUnit,
                       int selX1, int selY1, int selX2, int selY2, boolean dragging) {
        WorldMap map = state.getMap();
        int T = cam.getTileSize();
        int x0 = cam.getFirstTileX(), y0 = cam.getFirstTileY();
        int x1 = cam.getLastTileX(),  y1 = cam.getLastTileY();

        for (int ty = y0; ty <= y1; ty++)
            for (int tx = x0; tx <= x1; tx++) {
                Tile tile = map.getTile(tx, ty);
                if (tile == null) continue;
                int sx = cam.tileToScreenX(tx);
                int sy = cam.tileToScreenY(ty);
                drawTile(g, tile, tx, ty, sx, sy, T, state, cam);
            }

        // Jednostki
        for (Unit u : state.getUnits()) {
            if (!u.isAlive() || !cam.isVisible(u.getTileX(), u.getTileY())) continue;
            drawUnit(g, u, cam.tileToScreenX(u.getX()), cam.tileToScreenY(u.getY()),
                     u == selectedUnit, T);
        }

        // Zaznaczenie obszaru myszą
        if (dragging) {
            int px1 = cam.tileToScreenX(selX1), py1 = cam.tileToScreenY(selY1);
            int px2 = cam.tileToScreenX(selX2+1), py2 = cam.tileToScreenY(selY2+1);
            int rx = Math.min(px1,px2), ry = Math.min(py1,py2);
            int rw = Math.abs(px2-px1), rh = Math.abs(py2-py1);
            g.setColor(new Color(100, 200, 255, 40));
            g.fillRect(rx, ry, rw, rh);
            g.setColor(new Color(100, 200, 255, 180));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRect(rx, ry, rw, rh);
            g.setStroke(new BasicStroke(1));
        }
    }

    private void drawTile(Graphics2D g, Tile tile, int tx, int ty,
                          int sx, int sy, int T, GameState state, Camera cam) {
        TileType type = TileType.get(tile.getTypeId());

        g.setColor(type.color);
        g.fillRect(sx, sy, T, T);

        // Podświetlenie oznaczonych do zebrania
        if (tile.isMarkedForHarvest() && type.isResource) {
            g.setColor(new Color(255,255,0,55));
            g.fillRect(sx, sy, T, T);
            float prog = (float)tile.getHarvestProgress() / Math.max(1, type.harvestTicks);
            g.setColor(new Color(255,200,0,180));
            g.fillRect(sx, sy+T-3, (int)(T*prog), 3);
        }

        // Podświetlenie oznaczonych do budowy
        if (tile.isMarkedForBuild()) {
            g.setColor(new Color(100,180,255,70));
            g.fillRect(sx, sy, T, T);
            g.setColor(new Color(100,180,255,180));
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER,
                    1, new float[]{3,2}, 0));
            g.drawRect(sx, sy, T-1, T-1);
            g.setStroke(new BasicStroke(1));
        }

        // Ikona kafelka
        if (T >= 10) drawTileIcon(g, type.id, sx, sy, T);

        // Pokój – kolorowe tło
        if (tile.getRoomId() >= 0) {
            Room room = state.getRoomSystem().getRoomById(tile.getRoomId());
            if (room != null) {
                Color tc = room.getDef().tintColor;
                g.setColor(new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), 30));
                g.fillRect(sx+1, sy+1, T-2, T-2);
            }
        }

        // Siatka
        if (T >= 8) {
            g.setColor(new Color(0,0,0,35));
            g.drawRect(sx, sy, T, T);
        }
    }

    private void drawTileIcon(Graphics2D g, String id, int sx, int sy, int T) {
        String icon = switch (id) {
            case TileType.FOREST         -> "T";
            case TileType.MOUNTAIN       -> "^";
            case TileType.WATER          -> "~";
            case TileType.IRON_ORE       -> "Fe";
            case TileType.WOOD_LOG       -> "W";
            case TileType.STONE_ROCK     -> "S";
            case TileType.FOOD_PLANT     -> "F";
            case TileType.WALL           -> "#";
            case TileType.DOOR           -> "D";
            case TileType.GATE           -> "G";
            case TileType.CAMPFIRE       -> "*";
            case TileType.STOCKPILE      -> "[S]";
            case TileType.BED            -> "z";
            case TileType.TABLE          -> "=";
            case TileType.CHAIR          -> "c";
            case TileType.ANVIL          -> "A";
            case TileType.FORGE          -> "FG";
            case TileType.BOOKSHELF      -> "B";
            case TileType.TRAINING_DUMMY -> "TD";
            case TileType.THRONE         -> "TH";
            case TileType.WATCHTOWER     -> "WT";
            default -> "";
        };
        if (icon.isEmpty()) return;
        int fs = Math.max(7, T/2);
        g.setFont(new Font("Monospaced", Font.BOLD, fs));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(0,0,0,150));
        g.drawString(icon, sx+(T-fm.stringWidth(icon))/2,
                          sy+(T+fm.getAscent()-fm.getDescent())/2-1);
    }

    private void drawUnit(Graphics2D g, Unit unit, int sx, int sy, boolean selected, int T) {
        int pad = Math.max(1, T/7);
        int size = T - pad*2;
        int ux = sx+pad, uy = sy+pad;

        g.setColor(unit.getUnitClass().color);
        g.fillOval(ux, uy, size, size);

        g.setColor(selected ? Color.YELLOW : unit.isPlayerOwned() ? new Color(200,230,255) : new Color(255,180,180));
        g.setStroke(new BasicStroke(selected ? 2.5f : 1f));
        g.drawOval(ux, uy, size, size);
        g.setStroke(new BasicStroke(1));

        if (T >= 10) {
            int fs = Math.max(7, T-6);
            g.setFont(new Font("Monospaced", Font.BOLD, fs));
            g.setColor(Color.WHITE);
            String sym = String.valueOf(unit.getUnitClass().label.charAt(0));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(sym, ux+(size-fm.stringWidth(sym))/2,
                              uy+(size+fm.getAscent()-fm.getDescent())/2-1);
        }

        // Pasek HP
        int barW = T-2;
        float ratio = (float)unit.getHp()/unit.getMaxHp();
        g.setColor(new Color(40,40,40));
        g.fillRect(sx+1, sy+T-4, barW, 3);
        g.setColor(ratio>0.5f ? new Color(60,200,60) : ratio>0.25f ? new Color(220,180,0) : new Color(220,50,50));
        g.fillRect(sx+1, sy+T-4, (int)(barW*ratio), 3);
    }
}
