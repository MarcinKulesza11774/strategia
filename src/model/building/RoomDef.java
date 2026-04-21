package model.building;

import util.ConfigLoader;
import util.GameConfig;

import java.awt.Color;
import java.util.*;

/**
 * Definicje pokoi – ładowane z data/rooms.json.
 * Żeby dodać nowy pokój: dopisz go do rooms.json.
 */
public class RoomDef {
    public final String       id;
    public final String       label;
    public final String       description;
    public final List<String> requiredFurniture;
    public final int          minSize;
    public final int          selfSpeedBuff;
    public final int          selfAttackBuff;
    public final int          selfDefenseBuff;
    public final int          selfHealPerTick;
    public final Color        tintColor;

    // neighborId -> [speedBuff, attackBuff, defenseBuff, healBuff]
    public final Map<String, int[]> adjacencyBonus;

    private RoomDef(ConfigLoader.RoomCfg cfg) {
        this.id                = cfg.id();
        this.label             = cfg.label();
        this.description       = cfg.description();
        this.requiredFurniture = cfg.requiredFurniture();
        this.minSize           = cfg.minSize();
        this.selfSpeedBuff     = cfg.selfSpeedBuff();
        this.selfAttackBuff    = cfg.selfAttackBuff();
        this.selfDefenseBuff   = cfg.selfDefenseBuff();
        this.selfHealPerTick   = cfg.selfHealPerTick();
        this.tintColor         = cfg.tintColor();

        this.adjacencyBonus = new LinkedHashMap<>();
        for (ConfigLoader.AdjBonus b : cfg.adjacencyBonuses())
            adjacencyBonus.put(b.neighborId(), new int[]{b.speedBuff(), b.attackBuff(), b.defenseBuff(), b.healBuff()});
    }

    private static final Map<String, RoomDef> REGISTRY = new LinkedHashMap<>();

    public static void loadAll() {
        REGISTRY.clear();
        for (ConfigLoader.RoomCfg cfg : GameConfig.get().allRooms())
            REGISTRY.put(cfg.id(), new RoomDef(cfg));
    }

    public static RoomDef get(String id)            { return REGISTRY.get(id); }
    public static Collection<RoomDef> all()         { return REGISTRY.values(); }
    public static boolean exists(String id)         { return REGISTRY.containsKey(id); }
}
