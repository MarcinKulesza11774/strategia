package model;

import model.entity.Unit;
import model.entity.UnitClass;
import model.world.WorldMap;
import util.GameConfig;

import java.util.*;

/**
 * Zarządza falami wrogów – parametry z game_config.json.
 */
public class WaveManager {
    public enum WaveState { PEACE, WARNING, ACTIVE, CLEARED }

    private WaveState state       = WaveState.PEACE;
    private int       waveNumber  = 0;
    private int       ticksToWave;
    private int       ticksPeace;
    private boolean   firstWaveCleared = false;
    private final Random rng;

    public WaveManager(long seed) {
        rng        = new Random(seed);
        ticksPeace = GameConfig.get().waveFirstDelay();
    }

    public List<Unit> tick(WorldMap map, List<Unit> currentUnits) {
        List<Unit> spawned = new ArrayList<>();
        switch (state) {
            case PEACE -> {
                if (--ticksPeace <= 0) {
                    state = WaveState.WARNING;
                    ticksToWave = GameConfig.get().waveWarnTicks();
                    waveNumber++;
                }
            }
            case WARNING -> { if (--ticksToWave <= 0) { state = WaveState.ACTIVE; spawned = spawnWave(map); } }
            case ACTIVE -> {
                boolean any = currentUnits.stream().anyMatch(u -> !u.isPlayerOwned() && u.isAlive());
                if (!any) { state = WaveState.CLEARED; ticksPeace = GameConfig.get().waveBetweenTicks(); firstWaveCleared = true; }
            }
            case CLEARED -> { if (--ticksPeace <= 0) state = WaveState.PEACE; }
        }
        return spawned;
    }

    private List<Unit> spawnWave(WorldMap map) {
        List<Unit>  units  = new ArrayList<>();
        GameConfig  cfg    = GameConfig.get();
        int         count  = cfg.waveBaseCount() + (waveNumber-1) * cfg.waveCountPerWave();
        int[][]     edges  = getSpawnEdges(map);

        for (int i = 0; i < count; i++) {
            int[] pos = edges[rng.nextInt(edges.length)];
            boolean isChief = (i == count-1 && waveNumber >= cfg.waveChiefFromWave());
            String uc = isChief ? UnitClass.RAIDER_CHIEF : UnitClass.RAIDER;
            units.add(new Unit(uc, pos[0], pos[1], false));
        }
        return units;
    }

    private int[][] getSpawnEdges(WorldMap map) {
        List<int[]> c = new ArrayList<>();
        for (int x = 0; x < WorldMap.WIDTH;  x+=4) {
            if (map.isPassable(x,0))                c.add(new int[]{x,0});
            if (map.isPassable(x,WorldMap.HEIGHT-1)) c.add(new int[]{x,WorldMap.HEIGHT-1});
        }
        for (int y = 0; y < WorldMap.HEIGHT; y+=4) {
            if (map.isPassable(0,y))               c.add(new int[]{0,y});
            if (map.isPassable(WorldMap.WIDTH-1,y)) c.add(new int[]{WorldMap.WIDTH-1,y});
        }
        return c.isEmpty() ? new int[][]{{0,0}} : c.toArray(new int[0][]);
    }

    public WaveState getState()           { return state; }
    public int       getWaveNumber()      { return waveNumber; }
    public int       getTicksToWave()     { return ticksToWave; }
    public int       getTicksPeace()      { return ticksPeace; }
    public boolean   isFirstWaveCleared() { return firstWaveCleared; }
}
