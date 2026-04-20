package model;

import java.awt.Color;

/**
 * Reprezentuje gracza (człowieka lub AI).
 */
public class Player {
    private final String  name;
    private final Color   color;
    private final boolean isAI;

    public Player(String name, Color color, boolean isAI) {
        this.name  = name;
        this.color = color;
        this.isAI  = isAI;
    }

    public String  getName()  { return name; }
    public Color   getColor() { return color; }
    public boolean isAI()     { return isAI; }

    @Override
    public String toString() { return name; }
}
