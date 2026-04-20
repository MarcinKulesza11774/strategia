package ai;

import model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Prosta AI: każda jednostka zbliża się do najbliższego wroga i atakuje.
 */
public class SimpleAI {
    private final GameState state;

    public SimpleAI(GameState state) {
        this.state = state;
    }

    /** Wykonuje całą turę AI. Zwraca log akcji. */
    public List<String> playTurn() {
        List<String> log     = new ArrayList<>();
        List<Unit>   aiUnits = new ArrayList<>(state.getUnits());
        aiUnits.removeIf(u -> u.getOwner() != state.getCurrentPlayer());

        for (Unit unit : aiUnits) {
            if (!unit.isAlive()) continue;

            // Próbuj zaatakować sąsiada
            Unit target = findTarget(state.getMap().getAttackableTiles(unit));
            if (target != null) {
                log.add("[AI] " + state.attack(unit, target));
                continue;
            }

            // Przesuń się w kierunku wroga
            if (!unit.hasMovedThisTurn()) {
                int[] best = findBestMove(unit, state.getMap().getReachableTiles(unit));
                if (best != null) {
                    state.moveUnit(unit, best[0], best[1]);
                    log.add("[AI] " + unit.getType().displayName +
                            " przesuwa się na [" + best[0] + "," + best[1] + "]");

                    // Spróbuj zaatakować po ruchu
                    target = findTarget(state.getMap().getAttackableTiles(unit));
                    if (target != null) log.add("[AI] " + state.attack(unit, target));
                }
            }
        }
        return log;
    }

    private Unit findTarget(boolean[][] attackable) {
        for (int r = 0; r < GameMap.ROWS; r++)
            for (int c = 0; c < GameMap.COLS; c++) {
                if (!attackable[r][c]) continue;
                Tile tile = state.getMap().getTile(r, c);
                if (tile != null && tile.getUnit() != null) return tile.getUnit();
            }
        return null;
    }

    private int[] findBestMove(Unit unit, boolean[][] reachable) {
        List<Unit> enemies = new ArrayList<>(state.getUnits());
        enemies.removeIf(u -> u.getOwner() == unit.getOwner() || !u.isAlive());
        if (enemies.isEmpty()) return null;

        int[]  best     = null;
        double bestDist = Double.MAX_VALUE;
        for (int r = 0; r < GameMap.ROWS; r++)
            for (int c = 0; c < GameMap.COLS; c++) {
                if (!reachable[r][c]) continue;
                for (Unit e : enemies) {
                    double dist = Math.hypot(r - e.getRow(), c - e.getCol());
                    if (dist < bestDist) { bestDist = dist; best = new int[]{r, c}; }
                }
            }
        return best;
    }
}
