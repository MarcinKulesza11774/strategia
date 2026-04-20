package model;

/**
 * Jednostka na planszy – instancja UnitType należąca do gracza.
 */
public class Unit {
    private static int nextId = 1;

    private final int      id;
    private final UnitType type;
    private final Player   owner;

    private int     currentHp;
    private int     row;
    private int     col;
    private boolean movedThisTurn;
    private boolean attackedThisTurn;

    public Unit(UnitType type, Player owner, int row, int col) {
        this.id        = nextId++;
        this.type      = type;
        this.owner     = owner;
        this.currentHp = type.maxHp;
        this.row       = row;
        this.col       = col;
    }

    public int      getId()               { return id; }
    public UnitType getType()             { return type; }
    public Player   getOwner()            { return owner; }
    public int      getCurrentHp()        { return currentHp; }
    public int      getRow()              { return row; }
    public int      getCol()              { return col; }
    public boolean  hasMovedThisTurn()    { return movedThisTurn; }
    public boolean  hasAttackedThisTurn() { return attackedThisTurn; }
    public boolean  isAlive()             { return currentHp > 0; }

    public void setPosition(int row, int col)         { this.row = row; this.col = col; }
    public void setMovedThisTurn(boolean v)           { this.movedThisTurn    = v; }
    public void setAttackedThisTurn(boolean v)        { this.attackedThisTurn = v; }

    /** Resetuje flagi na początku nowej tury właściciela. */
    public void resetTurn() {
        movedThisTurn    = false;
        attackedThisTurn = false;
    }

    /**
     * Oblicza obrażenia zadane celowi.
     * Formuła: max(1, atak - obrona/2) ± 20% losowe wahanie.
     */
    public int calculateDamage(Unit target) {
        int base     = Math.max(1, type.attack - target.type.defense / 2);
        int variance = (int)(base * 0.2);
        return base + (int)(Math.random() * (variance * 2 + 1)) - variance;
    }

    /** Zadaje obrażenia. Zwraca true jeśli jednostka przeżyła. */
    public boolean takeDamage(int damage) {
        currentHp = Math.max(0, currentHp - damage);
        return currentHp > 0;
    }

    @Override
    public String toString() {
        return String.format("%s(%s) HP:%d/%d", type.displayName, owner.getName(), currentHp, type.maxHp);
    }
}
