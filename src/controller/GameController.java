package controller;

import ai.UnitAI;
import model.GameState;
import model.entity.Unit;
import model.world.TileType;
import model.world.WorldMap;
import util.GameConfig;
import view.Camera;
import view.GameWindow;
import view.SidePanel;

import javax.swing.*;
import java.util.List;
import java.util.Map;

/**
 * Kontroler – obsługuje input, koordynuje tick logiki i AI.
 */
public class GameController {

    private GameState  state;
    private GameWindow window;
    private UnitAI     unitAI;
    private final Timer sideTimer;

    public GameController() {
        sideTimer = new Timer(200, e -> { if (window != null) window.getSidePanel().update(); });
    }

    public void start() {
        state  = new GameState();
        unitAI = new UnitAI(state);
        window = new GameWindow(state, this);
        sideTimer.start();
        window.getGamePanel().startLoop();
        window.getGamePanel().requestFocusInWindow();
    }

    public void newGame() {
        window.getGamePanel().stopLoop();
        sideTimer.stop();
        state  = new GameState();
        unitAI = new UnitAI(state);
        window.dispose();
        window = new GameWindow(state, this);
        sideTimer.start();
        window.getGamePanel().startLoop();
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    public void tick(double dt) {
        if (state.isPaused()) return;
        window.getGamePanel().getCamera().tick(dt);
        state.tick(dt);
        for (Unit u : List.copyOf(state.getUnits()))
            if (u.isAlive()) unitAI.tick(u, dt);
    }

    // -------------------------------------------------------------------------
    // Klik lewy
    // -------------------------------------------------------------------------

    public void onLeftClick(int tx, int ty) {
        if (!state.getMap().inBounds(tx, ty)) return;

        String bm = state.getBuildMode();
        if (bm != null) {
            placeTile(tx, ty, bm);
            return;
        }

        Unit clicked = getUnitAt(tx, ty);
        if (clicked != null) {
            if (clicked.isPlayerOwned()) {
                state.setSelectedUnit(clicked);
            } else {
                Unit sel = state.getSelectedUnit();
                if (sel != null && sel.getUnitClass().canFight) {
                    sel.setTarget(clicked.getX(), clicked.getY());
                    state.log(sel.getUnitClass().label + " idzie atakować " + clicked.getUnitClass().label);
                }
            }
            return;
        }
        state.setSelectedUnit(null);
    }

    // -------------------------------------------------------------------------
    // Zaznaczanie obszaru
    // -------------------------------------------------------------------------

    /**
     * Wywoływane po przeciągnięciu myszą.
     * W trybie budowania: stawia kafelki na całym obszarze.
     * Normalnie: zaznacza wszystkie jednostki gracza w obszarze.
     */
    public void onAreaSelect(int x1, int y1, int x2, int y2) {
        String bm = state.getBuildMode();
        if (bm != null) {
            // Buduj cały obszar
            for (int ty = y1; ty <= y2; ty++)
                for (int tx = x1; tx <= x2; tx++)
                    placeTile(tx, ty, bm);
        } else {
            // Zaznacz wszystkie jednostki gracza w obszarze
            List<Unit> inArea = state.getPlayerUnits().stream()
                    .filter(u -> u.getTileX()>=x1 && u.getTileX()<=x2
                              && u.getTileY()>=y1 && u.getTileY()<=y2)
                    .toList();
            if (!inArea.isEmpty()) {
                state.setSelectedUnit(inArea.get(0)); // zaznacz pierwszą
                if (inArea.size() > 1) state.log("Zaznaczono " + inArea.size() + " jednostek");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Klik prawy
    // -------------------------------------------------------------------------

    public void onRightClick(int tx, int ty) {
        String bm = state.getBuildMode();
        if (bm != null) {
            // Cofnij znacznik budowy
            var tile = state.getMap().getTile(tx, ty);
            if (tile != null && tile.isMarkedForBuild()) {
                SidePanel.getCost(bm).forEach((k,v) -> state.addResource(k, v));
                tile.clearBuildMark();
            }
            return;
        }
        Unit sel = state.getSelectedUnit();
        if (sel != null && state.getMap().isPassable(tx, ty)) {
            sel.setTarget(tx, ty);
            state.log(sel.getUnitClass().label + " → ["+tx+","+ty+"]");
        }
    }

    // -------------------------------------------------------------------------
    // Budowanie
    // -------------------------------------------------------------------------

    private void placeTile(int tx, int ty, String tileId) {
        WorldMap map = state.getMap();
        if (!map.inBounds(tx, ty)) return;
        var tile = map.getTile(tx, ty);
        if (tile.isMarkedForBuild()) return;

        Map<String,Integer> cost = SidePanel.getCost(tileId);
        if (!state.canAfford(cost)) { state.log("Za mało surowców: " + TileType.get(tileId).label); return; }
        state.spend(cost);
        tile.markForBuild(tileId);
    }

    // -------------------------------------------------------------------------
    // Rekrutacja i awans
    // -------------------------------------------------------------------------

    public void recruit(String unitId, Map<String,Integer> cost) {
        if (state.playerUnitCount() >= state.getUnitCap()) {
            state.log("Osiągnięto limit jednostek (" + state.getUnitCap() + ")!"); return;
        }
        if (!state.canAfford(cost)) { state.log("Za mało surowców!"); return; }
        state.spend(cost);
        int fx = state.getMap().fortressX, fy = state.getMap().fortressY;
        state.spawnPlayer(unitId,
                fx + (int)(Math.random()*4)-2,
                fy + (int)(Math.random()*4)-2);
        state.log("Zwerbowano: " + model.entity.UnitClass.get(unitId).label);
    }

    public void promoteUnit(Unit u) {
        if (!u.canPromote()) return;
        model.entity.UnitClass next = u.getUnitClass().promoteTo();
        if (next == null) return;
        int cost = u.getUnitClass().xpToPromote / 2; // koszt w złocie
        if (!state.spendResource(model.world.ResourceType.GOLD, cost)) {
            state.log("Potrzeba " + cost + " złota na awans!"); return;
        }
        String before = u.getUnitClass().label;
        u.promote();
        state.log(before + " awansował na " + u.getUnitClass().label + "!");
    }

    // -------------------------------------------------------------------------
    // Pomocnicze
    // -------------------------------------------------------------------------

    public void centerCameraOn(Unit u) {
        Camera cam = window.getGamePanel().getCamera();
        cam.setCam(u.getX() - 30, u.getY() - 20);
    }

    private Unit getUnitAt(int tx, int ty) {
        for (Unit u : state.getUnits())
            if (u.isAlive() && u.getTileX()==tx && u.getTileY()==ty) return u;
        return null;
    }
}
