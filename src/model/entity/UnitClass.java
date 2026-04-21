package model.entity;

import util.ConfigLoader;
import util.GameConfig;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Klasa jednostki – teraz ładowana z data/units.json.
 * Żeby dodać nową jednostkę: dopisz ją do units.json.
 */
public class UnitClass {

    public static final String WORKER        = "WORKER";
    public static final String GUARD         = "GUARD";
    public static final String TRADER        = "TRADER";
    public static final String CRAFTSMAN     = "CRAFTSMAN";
    public static final String SOLDIER       = "SOLDIER";
    public static final String MERCHANT      = "MERCHANT";
    public static final String KNIGHT        = "KNIGHT";
    public static final String ARCHITECT     = "ARCHITECT";
    public static final String LORD          = "LORD";
    public static final String RAIDER        = "RAIDER";
    public static final String RAIDER_CHIEF  = "RAIDER_CHIEF";

    public final String  id;
    public final String  label;
    public final Color   color;
    public final int     tier;
    public final boolean canHarvest;
    public final boolean canBuild;
    public final boolean canFight;
    public final boolean canTrade;
    public final int     maxHp;
    public final int     attack;
    public final int     defense;
    public final double  speed;
    public final String  description;
    public final boolean isEnemy;
    public final String  promotesToId;
    public final int     xpToPromote;

    private UnitClass(ConfigLoader.UnitCfg cfg) {
        this.id           = cfg.id();
        this.label        = cfg.label();
        this.color        = cfg.color();
        this.tier         = cfg.tier();
        this.canHarvest   = cfg.canHarvest();
        this.canBuild     = cfg.canBuild();
        this.canFight     = cfg.canFight();
        this.canTrade     = cfg.canTrade();
        this.maxHp        = cfg.maxHp();
        this.attack       = cfg.attack();
        this.defense      = cfg.defense();
        this.speed        = cfg.speed();
        this.description  = cfg.description();
        this.isEnemy      = cfg.isEnemy();
        this.promotesToId = cfg.promotesTo();
        this.xpToPromote  = cfg.xpToPromote();
    }

    private static final Map<String, UnitClass> REGISTRY = new LinkedHashMap<>();

    public static void loadAll() {
        REGISTRY.clear();
        for (ConfigLoader.UnitCfg cfg : GameConfig.get().allUnits())
            REGISTRY.put(cfg.id(), new UnitClass(cfg));
    }

    public static UnitClass get(String id) {
        UnitClass u = REGISTRY.get(id);
        if (u == null) throw new IllegalArgumentException("Nieznana UnitClass: " + id);
        return u;
    }

    public static java.util.Collection<UnitClass> all() { return REGISTRY.values(); }

    public UnitClass promoteTo() {
        return promotesToId == null ? null : REGISTRY.get(promotesToId);
    }

    @Override public String toString()  { return id; }
    @Override public boolean equals(Object o) { return o instanceof UnitClass u && u.id.equals(id); }
    @Override public int hashCode()     { return id.hashCode(); }
}
