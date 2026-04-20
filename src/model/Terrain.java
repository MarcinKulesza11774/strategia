package model;

import java.awt.Color;

/**
 * Typy terenu dostępne na mapie.
 * Każdy teren ma swoją nazwę, kolor wyświetlania oraz koszt ruchu.
 */
public enum Terrain {
    PLAINS  ("Równina", new Color(144, 200, 100), 1),
    FOREST  ("Las",     new Color(34,  120,  34),  2),
    MOUNTAIN("Góry",    new Color(140, 120,  90),  3),
    WATER   ("Woda",    new Color(64,  164, 223),  99);

    public final String displayName;
    public final Color  color;
    public final int    moveCost;

    Terrain(String displayName, Color color, int moveCost) {
        this.displayName = displayName;
        this.color       = color;
        this.moveCost    = moveCost;
    }
}
