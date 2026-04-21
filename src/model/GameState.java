package model;

import model.building.RoomSystem;
import model.entity.Unit;
import model.entity.UnitClass;
import model.world.ResourceType;
import model.world.TileType;
import model.world.WorldMap;
import util.GameConfig;
import util.JsonParser;

import java.util.*;

/**
 * Centralny stan gry – parametry startowe z game_config.json.
 */
public class GameState {
    private final WorldMap    map;
    private final RoomSystem  roomSystem;
    private final WaveManager waveManager;
    private final List<Unit>  units = new ArrayList<>();
    private final Map<String, Integer> resources = new LinkedHashMap<>();

    private int     storageCapacity;
    private int     unitCap;
    private int     totalTick = 0;
    private boolean paused    = false;
    private boolean gameWon   = false;
    private Unit    selectedUnit = null;
    private String  buildMode    = null;  // TileType id or null

    private final List<String> eventLog = new ArrayList<>();
    private static final int   LOG_MAX  = 60;

    public GameState() {
        GameConfig cfg = GameConfig.get();
        map            = new WorldMap(cfg.mapSeed());
        roomSystem     = new RoomSystem(map);
        waveManager    = new WaveManager(cfg.mapSeed());
        storageCapacity = cfg.startingStorage();
        unitCap         = cfg.startingUnitCap();

        // Zasoby startowe z JSON
        ResourceType.all().forEach(rt -> resources.put(rt.id, 0));
        cfg.startingResources().forEach((id, val) ->
            resources.put(id, JsonParser.asInt(val)));

        // Jednostki startowe z JSON
        for (Object o : cfg.startingUnits()) {
            Map<String,Object> m = JsonParser.asMap(o);
            String uid = JsonParser.asString(m.get("unitId"));
            int ox = JsonParser.getInt(m, "offsetX", 0);
            int oy = JsonParser.getInt(m, "offsetY", 0);
            spawnPlayer(uid, map.fortressX + ox, map.fortressY + oy);
        }

        roomSystem.rescan();
        log("Gra rozpoczęta. Zbuduj twierdzę i odepnij pierwszą falę!");
    }

    public void tick(double dt) {
        if (paused || gameWon) return;
        totalTick++;

        // Fale wrogów
        List<Unit> spawned = waveManager.tick(map, units);
        units.addAll(spawned);
        if (!spawned.isEmpty())
            log("⚔ FALA " + waveManager.getWaveNumber() + " – " + spawned.size() + " wrogów!");

        // Regeneracja w pokojach co healIntervalTicks
        if (totalTick % GameConfig.get().healIntervalTicks() == 0) {
            for (Unit u : units) {
                if (!u.isAlive() || !u.isPlayerOwned()) continue;
                int[] buffs = roomSystem.getBuffsAt(u.getTileX(), u.getTileY());
                if (buffs[3] > 0) u.heal(buffs[3]);
                u.setSpeedBuff(buffs[0]);
                u.setAttackBuff(buffs[1]);
                u.setDefenseBuff(buffs[2]);
            }
        }

        units.removeIf(u -> !u.isAlive());

        // Handel pasywny
        if (totalTick % GameConfig.get().tradeInterval() == 0) {
            long traders = units.stream().filter(u -> u.isPlayerOwned() && u.getUnitClass().canTrade).count();
            if (traders > 0) {
                int gold = (int)(traders * (GameConfig.get().traderGoldBase() + Math.random() * GameConfig.get().traderGoldRandom()));
                addResource(ResourceType.GOLD, gold);
            }
        }

        // Powiadomienia o fali
        if (waveManager.getState() == WaveManager.WaveState.WARNING
                && waveManager.getTicksToWave() % 60 == 0) {
            log("⚠ Fala nadchodzi za " + waveManager.getTicksToWave()/60 + "s!");
        }

        if (!gameWon && waveManager.isFirstWaveCleared()) {
            gameWon = true;
            log("🏆 ZWYCIĘSTWO! Pierwsza fala odparta!");
        }
    }

    public boolean addResource(String typeId, int amount) {
        int cur = resources.getOrDefault(typeId, 0);
        int nv  = Math.min(cur + amount, storageCapacity);
        resources.put(typeId, nv);
        return nv > cur;
    }

    public boolean spendResource(String typeId, int amount) {
        int cur = resources.getOrDefault(typeId, 0);
        if (cur < amount) return false;
        resources.put(typeId, cur - amount);
        return true;
    }

    public boolean canAfford(Map<String,Integer> cost) {
        for (var e : cost.entrySet())
            if (resources.getOrDefault(e.getKey(), 0) < e.getValue()) return false;
        return true;
    }

    public boolean spend(Map<String,Integer> cost) {
        if (!canAfford(cost)) return false;
        cost.forEach((k,v) -> resources.put(k, resources.get(k)-v));
        return true;
    }

    public Unit spawnPlayer(String classId, int x, int y) {
        Unit u = new Unit(classId, x, y, true);
        units.add(u);
        return u;
    }

    public Unit spawnEnemy(String classId, int x, int y) {
        Unit u = new Unit(classId, x, y, false);
        units.add(u);
        return u;
    }

    public List<Unit> getPlayerUnits()  { return units.stream().filter(Unit::isPlayerOwned).toList(); }
    public List<Unit> getEnemyUnits()   { return units.stream().filter(u -> !u.isPlayerOwned()).toList(); }
    public int        playerUnitCount() { return (int)units.stream().filter(Unit::isPlayerOwned).count(); }

    public void log(String msg) {
        eventLog.add(0, msg);
        if (eventLog.size() > LOG_MAX) eventLog.remove(eventLog.size()-1);
    }

    // Gettery
    public WorldMap    getMap()            { return map; }
    public RoomSystem  getRoomSystem()     { return roomSystem; }
    public WaveManager getWaveManager()    { return waveManager; }
    public List<Unit>  getUnits()          { return units; }
    public Map<String,Integer> getResources() { return resources; }
    public int         getResource(String id) { return resources.getOrDefault(id, 0); }
    public int         getStorageCapacity(){ return storageCapacity; }
    public int         getUnitCap()        { return unitCap; }
    public void        setUnitCap(int v)   { unitCap = v; }
    public int         getTotalTick()      { return totalTick; }
    public boolean     isPaused()          { return paused; }
    public void        setPaused(boolean v){ paused = v; }
    public boolean     isGameWon()         { return gameWon; }
    public Unit        getSelectedUnit()   { return selectedUnit; }
    public void        setSelectedUnit(Unit u){ selectedUnit = u; }
    public String      getBuildMode()      { return buildMode; }
    public void        setBuildMode(String t){ buildMode = t; }
    public List<String> getEventLog()      { return eventLog; }
}
