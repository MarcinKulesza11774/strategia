package model.entity;

import model.GameState;
import util.GameConfig;
import util.JsonParser;

/**
 * Co jakiś czas spawni nową generyczną jednostkę przy twierdzy.
 * Interwał i max czekających z game_config.json → genericUnit.
 */
public class UnitSpawner {
    private int ticksUntilNext;
    private final int interval;
    private final int maxWaiting;

    public UnitSpawner() {
        var cfg = GameConfig.get();
        // Czytamy z sekcji genericUnit
        java.util.Map<String,Object> gu = JsonParser.asMap(cfg.loader().gameConfig.get("genericUnit"));
        interval   = JsonParser.getInt(gu, "spawnIntervalTicks", 1800);
        maxWaiting = JsonParser.getInt(gu, "maxGenericWaiting",  5);
        ticksUntilNext = interval;
    }

    public void tick(GameState state) {
        if (state.isPaused()) return;
        ticksUntilNext--;
        if (ticksUntilNext <= 0) {
            ticksUntilNext = interval;
            long waiting = state.getPlayerUnits().stream()
                    .filter(u -> u.getUnitClass().isGeneric).count();
            if (waiting < maxWaiting) {
                int fx = state.getMap().fortressX;
                int fy = state.getMap().fortressY;
                Unit u = state.spawnPlayer(UnitClass.GENERIC,
                        fx + (int)(Math.random()*4)-2,
                        fy + (int)(Math.random()*4)-2);
                state.log("Nowy wędrowiec przybył: " + u.getName());
            }
        }
    }

    public int getTicksUntilNext() { return ticksUntilNext; }
}
