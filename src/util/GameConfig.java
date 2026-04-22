package util;

import java.util.Map;

/**
 * Singleton z załadowaną konfiguracją gry.
 * Wywołaj GameConfig.init() raz przy starcie, potem używaj GameConfig.get().
 *
 * Przykład użycia:
 *   int w = GameConfig.get().mapWidth();
 *   ConfigLoader.UnitCfg cfg = GameConfig.get().unit("WORKER");
 */
public class GameConfig {

    private static GameConfig instance;

    public final ConfigLoader loader;

    private GameConfig() {
        loader = new ConfigLoader();
        loader.loadAll();
    }

    public static void init() {
        instance = new GameConfig();
    }

    public ConfigLoader loader() { return loader; }

    public static GameConfig get() {
        if (instance == null) throw new IllegalStateException("GameConfig.init() nie został wywołany!");
        return instance;
    }

    // -------------------------------------------------------------------------
    // Dostęp do konfiguracji typów
    // -------------------------------------------------------------------------

    public ConfigLoader.TileCfg    tile(String id)     { return loader.tiles.get(id); }
    public ConfigLoader.UnitCfg    unit(String id)     { return loader.units.get(id); }
    public ConfigLoader.RoomCfg    room(String id)     { return loader.rooms.get(id); }
    public java.util.Collection<ConfigLoader.TileCfg>  allTiles() { return loader.tiles.values(); }
    public java.util.Collection<ConfigLoader.UnitCfg>  allUnits() { return loader.units.values(); }
    public java.util.Collection<ConfigLoader.RoomCfg>  allRooms() { return loader.rooms.values(); }
    public java.util.List<ConfigLoader.BuildingCfg>    buildings(){ return loader.buildings; }
    public java.util.List<ConfigLoader.RecruitCfg>     recruit()  { return loader.recruit; }

    // -------------------------------------------------------------------------
    // Szybki dostęp do game_config.json
    // -------------------------------------------------------------------------

    private Map<String, Object> cfg()                  { return loader.gameConfig; }
    private Map<String, Object> section(String key)    { return JsonParser.asMap(cfg().get(key)); }

    public int    mapWidth()           { return JsonParser.getInt(section("map"), "width",  120); }
    public int    mapHeight()          { return JsonParser.getInt(section("map"), "height",  80); }
    public long   mapSeed()            { return JsonParser.getInt(section("map"), "seed",    42); }
    public int    fortressClearRadius(){ return JsonParser.getInt(section("map"), "fortressClearRadius", 8); }

    public int    startingUnitCap()    { return JsonParser.getInt(section("limits"), "startingUnitCap", 6); }
    public int    startingStorage()    { return JsonParser.getInt(section("limits"), "startingStorageCapacity", 200); }

    public int    waveFirstDelay()     { return JsonParser.getInt(section("waves"), "firstWaveDelayTicks", 3600); }
    public int    waveWarnTicks()      { return JsonParser.getInt(section("waves"), "warnBeforeTicks",     600); }
    public int    waveBetweenTicks()   { return JsonParser.getInt(section("waves"), "betweenWavesTicks",   3600); }
    public int    waveBaseCount()      { return JsonParser.getInt(section("waves"), "baseEnemyCount",      3); }
    public int    waveCountPerWave()   { return JsonParser.getInt(section("waves"), "enemyCountPerWave",   2); }
    public int    waveChiefFromWave()  { return JsonParser.getInt(section("waves"), "chiefAppearsFromWave",2); }

    public int    traderGoldBase()     { return JsonParser.getInt(section("economy"), "traderGoldPerTickBase",   5); }
    public int    traderGoldRandom()   { return JsonParser.getInt(section("economy"), "traderGoldPerTickRandom", 5); }
    public int    tradeInterval()      { return JsonParser.getInt(section("economy"), "tradeIntervalTicks",    300); }

    public int    attackCooldown()     { return JsonParser.getInt(section("combat"), "attackCooldownTicks",  45); }
    public int    fightRange()         { return JsonParser.getInt(section("combat"), "fightDetectionRange",  20); }
    public int    attackRange()        { return JsonParser.getInt(section("combat"), "attackRange",           1); }
    public int    xpPerAttack()        { return JsonParser.getInt(section("combat"), "xpPerAttack",           5); }
    public int    xpPerKill()          { return JsonParser.getInt(section("combat"), "xpPerKill",            20); }
    public int    xpPerHarvest()       { return JsonParser.getInt(section("combat"), "xpPerHarvest",         10); }

    public int    workerSearchRadius() { return JsonParser.getInt(section("ai"), "workerSearchRadius",  40); }
    public int    buildTicksRequired() { return JsonParser.getInt(section("ai"), "buildTicksRequired",  60); }
    public int    healIntervalTicks()  { return JsonParser.getInt(section("ai"), "healIntervalTicks",   30); }

    public int    tileSize()           { return JsonParser.getInt(section("camera"), "tileSize",    14); }
    public int    minTileSize()        { return JsonParser.getInt(section("camera"), "minTileSize",   6); }
    public int    maxTileSize()        { return JsonParser.getInt(section("camera"), "maxTileSize",  32); }
    public double scrollSpeed()        { return JsonParser.getDouble(section("camera"), "scrollSpeed", 20.0); }
    public int    zoomStep()           { return JsonParser.getInt(section("camera"), "zoomStep",      2); }

    public Map<String, Object> startingResources() {
        return JsonParser.asMap(cfg().get("startingResources"));
    }

    public java.util.List<Object> startingUnits() {
        return JsonParser.asList(cfg().get("startingUnits"));
    }

    public Map<String, Object> mapGenWeights() {
        return JsonParser.asMap(section("map").get("generationWeights"));
    }

    public Map<String, Object> mapClusterCounts() {
        return JsonParser.asMap(section("map").get("clusterCounts"));
    }
}
