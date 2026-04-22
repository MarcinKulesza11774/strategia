package model.entity;

import util.ConfigLoader;
import util.GameConfig;
import util.JsonParser;

import java.awt.Color;
import java.util.*;

/**
 * Klasa jednostki ładowana z data/units.json.
 * Teraz zawiera stats bazowe (VIT/STR/PRE/CHA) i listę opcji awansu.
 */
public class UnitClass {
    public static final String GENERIC      = "GENERIC";
    public static final String BUILDER      = "BUILDER";
    public static final String GATHERER     = "GATHERER";
    public static final String GUARD_BASIC  = "GUARD_BASIC";
    public static final String CARPENTER    = "CARPENTER";
    public static final String STONEMASON   = "STONEMASON";
    public static final String LUMBERJACK   = "LUMBERJACK";
    public static final String MINER        = "MINER";
    public static final String WARRIOR      = "WARRIOR";
    public static final String ARCHER       = "ARCHER";
    public static final String CLERIC       = "CLERIC";
    public static final String RAIDER       = "RAIDER";
    public static final String RAIDER_CHIEF = "RAIDER_CHIEF";

    public final String       id;
    public final String       label;
    public final int          tier;
    public final boolean      isGeneric;
    public final boolean      isEnemy;
    public final Color        color;
    public final String       description;
    /** Opcjonalna ścieżka do obrazka, np. "units/robotnik.png". Null = rysuj kółko. */
    public final String       nazwaObrazka;
    public final int          baseVit, baseStr, basePre, baseCha;
    public final boolean      canHarvest, canBuild, canFight, canTrade;
    public final double       speed;
    public final int          xpToPromote;
    public final List<String> promotionOptions; // IDs klas do których można awansować
    public final int          attackRange;      // domyślnie 1, łucznik ma 3

    private UnitClass(Map<String,Object> m) {
        id               = JsonParser.asString(m.get("id"));
        label            = JsonParser.asString(m.get("label"));
        tier             = JsonParser.getInt(m, "tier", 0);
        isGeneric        = JsonParser.getBool(m, "isGeneric", false);
        isEnemy          = JsonParser.getBool(m, "isEnemy", false);
        color            = parseColor(m.get("color"));
        description      = JsonParser.getString(m, "description", "");
        nazwaObrazka     = JsonParser.getString(m, "image", null);
        baseVit          = JsonParser.getInt(m, "baseVit", 10);
        baseStr          = JsonParser.getInt(m, "baseStr", 3);
        basePre          = JsonParser.getInt(m, "basePre", 3);
        baseCha          = JsonParser.getInt(m, "baseCha", 3);
        canHarvest       = JsonParser.getBool(m, "canHarvest", false);
        canBuild         = JsonParser.getBool(m, "canBuild",   false);
        canFight         = JsonParser.getBool(m, "canFight",   false);
        canTrade         = JsonParser.getBool(m, "canTrade",   false);
        speed            = JsonParser.getDouble(m, "speed", 3.0);
        xpToPromote      = JsonParser.getInt(m, "xpToPromote", 999999);
        attackRange      = JsonParser.getInt(m, "attackRange", 1);

        List<String> opts = new ArrayList<>();
        Object optsRaw = m.get("promotionOptions");
        if (optsRaw != null)
            for (Object o : JsonParser.asList(optsRaw)) opts.add(JsonParser.asString(o));
        promotionOptions = Collections.unmodifiableList(opts);
    }

    private static Color parseColor(Object raw) {
        if (raw == null) return Color.GRAY;
        List<Object> a = JsonParser.asList(raw);
        return new Color(JsonParser.asInt(a.get(0)), JsonParser.asInt(a.get(1)), JsonParser.asInt(a.get(2)));
    }

    private static final Map<String, UnitClass> REGISTRY = new LinkedHashMap<>();

    public static void loadAll() {
        REGISTRY.clear();
        Object root = JsonParser.parse(readUnitsJson());
        List<Object> classes = JsonParser.asList(JsonParser.asMap(root).get("classes"));
        for (Object o : classes) {
            UnitClass uc = new UnitClass(JsonParser.asMap(o));
            REGISTRY.put(uc.id, uc);
        }
    }

    private static String readUnitsJson() {
        try {
            java.io.File f = new java.io.File("data/units.json");
            if (f.exists()) return new String(java.nio.file.Files.readAllBytes(f.toPath()));
            java.net.URL url = UnitClass.class.getResource("/data/units.json");
            if (url != null) try (var is = url.openStream()) { return new String(is.readAllBytes()); }
        } catch (Exception e) { throw new RuntimeException("Cannot load units.json", e); }
        throw new RuntimeException("units.json not found");
    }

    public static UnitClass get(String id) {
        UnitClass u = REGISTRY.get(id);
        if (u == null) throw new IllegalArgumentException("Nieznana UnitClass: " + id);
        return u;
    }
    public static Collection<UnitClass> all() { return REGISTRY.values(); }

    /** Czy ta klasa może awansować na daną inną klasę. */
    public boolean canPromoteTo(String targetId) { return promotionOptions.contains(targetId); }

    @Override public String toString()            { return id; }
    @Override public boolean equals(Object o)     { return o instanceof UnitClass u && u.id.equals(id); }
    @Override public int hashCode()               { return id.hashCode(); }
}
