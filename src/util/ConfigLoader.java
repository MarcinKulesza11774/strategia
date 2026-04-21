package util;

import java.awt.Color;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Ładuje pliki JSON z katalogu /data i udostępnia dane w typowany sposób.
 *
 * Szuka plików w:
 *   1. katalogu ./data/ (względem katalogu roboczego – dla IntelliJ uruchomionego z root projektu)
 *   2. /data/ jako zasób na classpath (gdy spakowane do JAR)
 *
 * Wywoływany raz przy starcie gry przez GameConfig.init().
 */
public class ConfigLoader {

    // -------------------------------------------------------------------------
    // Wewnętrzne struktury danych
    // -------------------------------------------------------------------------

    public record TileCfg(
        String id, String label, Color color,
        boolean passable, boolean isWall, boolean isFloor, boolean isResource,
        String resourceType, int harvestTicks, int harvestYieldMin, int harvestYieldMax,
        String harvestRemnant
    ) {}

    public record UnitCfg(
        String id, String label, Color color, int tier,
        boolean canHarvest, boolean canBuild, boolean canFight, boolean canTrade,
        int maxHp, int attack, int defense, double speed, String description,
        boolean isEnemy, String promotesTo, int xpToPromote
    ) {}

    public record AdjBonus(String neighborId, int speedBuff, int attackBuff, int defenseBuff, int healBuff) {}

    public record RoomCfg(
        String id, String label, String description,
        List<String> requiredFurniture, int minSize,
        int selfSpeedBuff, int selfAttackBuff, int selfDefenseBuff, int selfHealPerTick,
        Color tintColor, List<AdjBonus> adjacencyBonuses
    ) {}

    public record BuildingCfg(String tileId, String label, Map<String, Integer> cost) {}
    public record RecruitCfg(String unitId, Map<String, Integer> cost) {}

    // -------------------------------------------------------------------------
    // Publiczne dane po załadowaniu
    // -------------------------------------------------------------------------

    public final Map<String, TileCfg>     tiles     = new LinkedHashMap<>();
    public final Map<String, UnitCfg>     units     = new LinkedHashMap<>();
    public final Map<String, RoomCfg>     rooms     = new LinkedHashMap<>();
    public final List<BuildingCfg>        buildings = new ArrayList<>();
    public final List<RecruitCfg>         recruit   = new ArrayList<>();
    public       Map<String, Object>      gameConfig;

    // -------------------------------------------------------------------------
    // Ładowanie
    // -------------------------------------------------------------------------

    public void loadAll() {
        try {
            parseTiles(readFile("tiles.json"));
            parseUnits(readFile("units.json"));
            parseRooms(readFile("rooms.json"));
            parseBuildings(readFile("buildings.json"));
            parseRecruitment(readFile("recruitment.json"));
            gameConfig = JsonParser.asMap(JsonParser.parse(readFile("game_config.json")));
        } catch (Exception e) {
            throw new RuntimeException("Błąd ładowania konfiguracji: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Parsery poszczególnych plików
    // -------------------------------------------------------------------------

    private void parseTiles(String json) {
        Map<String, Object> root = JsonParser.asMap(JsonParser.parse(json));
        for (Object o : JsonParser.asList(root.get("tiles"))) {
            Map<String, Object> m = JsonParser.asMap(o);
            String id = JsonParser.asString(m.get("id"));
            tiles.put(id, new TileCfg(
                id,
                JsonParser.asString(m.get("label")),
                parseColor(m.get("color")),
                JsonParser.getBool(m, "passable", false),
                JsonParser.getBool(m, "isWall",   false),
                JsonParser.getBool(m, "isFloor",  false),
                JsonParser.getBool(m, "isResource", false),
                JsonParser.getString(m, "resourceType", null),
                JsonParser.getInt(m, "harvestTicks", 0),
                JsonParser.getInt(m, "harvestYieldMin", 5),
                JsonParser.getInt(m, "harvestYieldMax", 12),
                JsonParser.getString(m, "harvestRemnant", "GRASS")
            ));
        }
    }

    private void parseUnits(String json) {
        Map<String, Object> root = JsonParser.asMap(JsonParser.parse(json));
        for (Object o : JsonParser.asList(root.get("units"))) {
            Map<String, Object> m = JsonParser.asMap(o);
            String id = JsonParser.asString(m.get("id"));
            units.put(id, new UnitCfg(
                id,
                JsonParser.asString(m.get("label")),
                parseColor(m.get("color")),
                JsonParser.getInt(m, "tier", 0),
                JsonParser.getBool(m, "canHarvest", false),
                JsonParser.getBool(m, "canBuild",   false),
                JsonParser.getBool(m, "canFight",   false),
                JsonParser.getBool(m, "canTrade",   false),
                JsonParser.getInt(m, "maxHp",    10),
                JsonParser.getInt(m, "attack",    1),
                JsonParser.getInt(m, "defense",   0),
                JsonParser.getDouble(m, "speed",  3.0),
                JsonParser.getString(m, "description", ""),
                JsonParser.getBool(m, "isEnemy", false),
                JsonParser.getString(m, "promotesTo", null),
                JsonParser.getInt(m, "xpToPromote", 999999)
            ));
        }
    }

    private void parseRooms(String json) {
        Map<String, Object> root = JsonParser.asMap(JsonParser.parse(json));
        for (Object o : JsonParser.asList(root.get("rooms"))) {
            Map<String, Object> m = JsonParser.asMap(o);
            String id = JsonParser.asString(m.get("id"));

            List<String> furniture = new ArrayList<>();
            for (Object f : JsonParser.asList(m.get("requiredFurniture")))
                furniture.add(JsonParser.asString(f));

            List<AdjBonus> bonuses = new ArrayList<>();
            Object adjRaw = m.get("adjacencyBonuses");
            if (adjRaw != null) {
                for (Object b : JsonParser.asList(adjRaw)) {
                    Map<String, Object> bm = JsonParser.asMap(b);
                    bonuses.add(new AdjBonus(
                        JsonParser.asString(bm.get("neighborId")),
                        JsonParser.getInt(bm, "speedBuff",   0),
                        JsonParser.getInt(bm, "attackBuff",  0),
                        JsonParser.getInt(bm, "defenseBuff", 0),
                        JsonParser.getInt(bm, "healBuff",    0)
                    ));
                }
            }

            rooms.put(id, new RoomCfg(
                id,
                JsonParser.asString(m.get("label")),
                JsonParser.asString(m.get("description")),
                furniture,
                JsonParser.getInt(m, "minSize", 2),
                JsonParser.getInt(m, "selfSpeedBuff",   0),
                JsonParser.getInt(m, "selfAttackBuff",  0),
                JsonParser.getInt(m, "selfDefenseBuff", 0),
                JsonParser.getInt(m, "selfHealPerTick", 0),
                parseColor(m.get("tintColor")),
                bonuses
            ));
        }
    }

    private void parseBuildings(String json) {
        Map<String, Object> root = JsonParser.asMap(JsonParser.parse(json));
        for (Object o : JsonParser.asList(root.get("buildings"))) {
            Map<String, Object> m = JsonParser.asMap(o);
            Map<String, Integer> cost = parseCost(m.get("cost"));
            buildings.add(new BuildingCfg(
                JsonParser.asString(m.get("tileId")),
                JsonParser.asString(m.get("label")),
                cost
            ));
        }
    }

    private void parseRecruitment(String json) {
        Map<String, Object> root = JsonParser.asMap(JsonParser.parse(json));
        for (Object o : JsonParser.asList(root.get("recruitment"))) {
            Map<String, Object> m = JsonParser.asMap(o);
            recruit.add(new RecruitCfg(
                JsonParser.asString(m.get("unitId")),
                parseCost(m.get("cost"))
            ));
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, Integer> parseCost(Object raw) {
        Map<String, Integer> cost = new LinkedHashMap<>();
        if (raw == null) return cost;
        Map<String, Object> m = JsonParser.asMap(raw);
        m.forEach((k, v) -> cost.put(k, JsonParser.asInt(v)));
        return cost;
    }

    private Color parseColor(Object raw) {
        if (raw == null) return Color.GRAY;
        List<Object> arr = JsonParser.asList(raw);
        return new Color(
            JsonParser.asInt(arr.get(0)),
            JsonParser.asInt(arr.get(1)),
            JsonParser.asInt(arr.get(2))
        );
    }

    private String readFile(String name) throws IOException {
        // 1. Spróbuj ./data/<name> (katalog roboczy projektu)
        File f = new File("data/" + name);
        if (f.exists()) return readFileContent(f);

        // 2. Spróbuj jako zasób classpath /data/<name>
        URL url = getClass().getResource("/data/" + name);
        if (url != null) {
            try (InputStream is = url.openStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        throw new FileNotFoundException("Nie znaleziono pliku konfiguracji: " + name +
                "\nSzukano w: " + f.getAbsolutePath() + " i na classpath /data/" + name);
    }

    private String readFileContent(File f) throws IOException {
        try (InputStream is = new FileInputStream(f)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
