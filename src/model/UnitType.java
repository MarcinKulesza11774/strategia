package model;

/**
 * Typy jednostek z bazowymi statystykami.
 */
public enum UnitType {
    WARRIOR("Jednostka typu 1", 30,  8, 3, 3, "1"),
    ARCHER ("Jednostka typu 2",  20,  6, 1, 4, "2"),
    KNIGHT ("Jednostka typu 3",   50, 10, 5, 2, "3");

    public final String displayName;
    public final int    maxHp;
    public final int    attack;
    public final int    defense;
    public final int    moveRange;
    public final String symbol;

    UnitType(String displayName, int maxHp, int attack, int defense, int moveRange, String symbol) {
        this.displayName = displayName;
        this.maxHp       = maxHp;
        this.attack      = attack;
        this.defense     = defense;
        this.moveRange   = moveRange;
        this.symbol      = symbol;
    }
}
