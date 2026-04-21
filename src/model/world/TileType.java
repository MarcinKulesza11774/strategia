package model.world;

import util.ConfigLoader;
import util.GameConfig;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typ kafelka mapy – teraz ładowany z data/tiles.json.
 * Żeby dodać nowy kafelek: dopisz go do data/tiles.json i zrestartuj grę.
 */
public class TileType {

    public static final String GRASS          = "GRASS";
    public static final String DIRT           = "DIRT";
    public static final String WATER          = "WATER";
    public static final String FOREST         = "FOREST";
    public static final String MOUNTAIN       = "MOUNTAIN";
    public static final String STONE_FLOOR    = "STONE_FLOOR";
    public static final String IRON_ORE       = "IRON_ORE";
    public static final String WOOD_LOG       = "WOOD_LOG";
    public static final String STONE_ROCK     = "STONE_ROCK";
    public static final String FOOD_PLANT     = "FOOD_PLANT";
    public static final String WALL           = "WALL";
    public static final String FLOOR          = "FLOOR";
    public static final String DOOR           = "DOOR";
    public static final String GATE           = "GATE";
    public static final String CAMPFIRE       = "CAMPFIRE";
    public static final String STOCKPILE      = "STOCKPILE";
    public static final String BED            = "BED";
    public static final String TABLE          = "TABLE";
    public static final String CHAIR          = "CHAIR";
    public static final String ANVIL          = "ANVIL";
    public static final String FORGE          = "FORGE";
    public static final String BOOKSHELF      = "BOOKSHELF";
    public static final String TRAINING_DUMMY = "TRAINING_DUMMY";
    public static final String THRONE         = "THRONE";
    public static final String WATCHTOWER     = "WATCHTOWER";
    public static final String SPAWN_POINT    = "SPAWN_POINT";

    public final String  id;
    public final String  label;
    public final Color   color;
    public final boolean passable;
    public final boolean isWall;
    public final boolean isFloor;
    public final boolean isResource;
    public final String  resourceType;
    public final int     harvestTicks;
    public final int     harvestYieldMin;
    public final int     harvestYieldMax;
    public final String  harvestRemnantId;

    private TileType(ConfigLoader.TileCfg cfg) {
        this.id               = cfg.id();
        this.label            = cfg.label();
        this.color            = cfg.color();
        this.passable         = cfg.passable();
        this.isWall           = cfg.isWall();
        this.isFloor          = cfg.isFloor();
        this.isResource       = cfg.isResource();
        this.resourceType     = cfg.resourceType();
        this.harvestTicks     = cfg.harvestTicks();
        this.harvestYieldMin  = cfg.harvestYieldMin();
        this.harvestYieldMax  = cfg.harvestYieldMax();
        this.harvestRemnantId = cfg.harvestRemnant();
    }

    private static final Map<String, TileType> REGISTRY = new LinkedHashMap<>();

    public static void loadAll() {
        REGISTRY.clear();
        for (ConfigLoader.TileCfg cfg : GameConfig.get().allTiles())
            REGISTRY.put(cfg.id(), new TileType(cfg));
    }

    public static TileType get(String id) {
        TileType t = REGISTRY.get(id);
        if (t == null) throw new IllegalArgumentException("Nieznany TileType: " + id);
        return t;
    }

    public static boolean exists(String id) { return REGISTRY.containsKey(id); }
    public static java.util.Collection<TileType> all() { return REGISTRY.values(); }

    public ResourceType harvestResource() {
        return resourceType == null ? null : ResourceType.get(resourceType);
    }

    public TileType harvestRemnant() {
        return get(harvestRemnantId != null ? harvestRemnantId : GRASS);
    }

    public boolean isFurniture() {
        return !isWall && !isFloor && !isResource
            && !id.equals(GRASS) && !id.equals(DIRT) && !id.equals(WATER)
            && !id.equals(FOREST) && !id.equals(MOUNTAIN) && !id.equals(STONE_FLOOR)
            && !id.equals(DOOR) && !id.equals(GATE) && !id.equals(SPAWN_POINT);
    }

    @Override public String toString()  { return id; }
    @Override public boolean equals(Object o) { return o instanceof TileType t && t.id.equals(id); }
    @Override public int hashCode()     { return id.hashCode(); }
}
