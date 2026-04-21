package model.world;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typ surowca – teraz stałe string ID zamiast enum, żeby można dodawać z JSON.
 * Wartości są rejestrowane automatycznie gdy TileType.loadAll() odczytuje kafelki.
 */
public class ResourceType {
    public static final String WOOD  = "WOOD";
    public static final String STONE = "STONE";
    public static final String IRON  = "IRON";
    public static final String FOOD  = "FOOD";
    public static final String GOLD  = "GOLD";

    public final String id;
    public final String label;
    public final Color  color;

    private ResourceType(String id, String label, Color color) {
        this.id = id; this.label = label; this.color = color;
    }

    private static final Map<String, ResourceType> REGISTRY = new LinkedHashMap<>();

    static {
        register(WOOD,  "Drewno",   new Color(120,  80,  30));
        register(STONE, "Kamień",   new Color(150, 140, 120));
        register(IRON,  "Żelazo",   new Color(160,  90,  60));
        register(FOOD,  "Jedzenie", new Color( 80, 180,  60));
        register(GOLD,  "Złoto",    new Color(210, 180,  30));
    }

    public static void register(String id, String label, Color color) {
        REGISTRY.put(id, new ResourceType(id, label, color));
    }

    public static ResourceType get(String id) {
        ResourceType r = REGISTRY.get(id);
        if (r == null) throw new IllegalArgumentException("Nieznany ResourceType: " + id);
        return r;
    }

    public static java.util.Collection<ResourceType> all() { return REGISTRY.values(); }

    @Override public String toString()  { return id; }
    @Override public boolean equals(Object o) { return o instanceof ResourceType r && r.id.equals(id); }
    @Override public int hashCode()     { return id.hashCode(); }
}
