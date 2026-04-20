package model;

/**
 * Pojedyncze pole na mapie gry.
 */
public class Tile {
    private final int     row;
    private final int     col;
    private final Terrain terrain;
    private Player owner;
    private Unit   unit;

    public Tile(int row, int col, Terrain terrain) {
        this.row     = row;
        this.col     = col;
        this.terrain = terrain;
    }

    public int     getRow()     { return row; }
    public int     getCol()     { return col; }
    public Terrain getTerrain() { return terrain; }
    public Player  getOwner()   { return owner; }
    public Unit    getUnit()    { return unit; }

    public void setOwner(Player owner) { this.owner = owner; }
    public void setUnit(Unit unit)     { this.unit  = unit; }

    /** Czy pole jest przejezdne dla danego gracza. */
    public boolean isPassable(Player forPlayer) {
        if (terrain == Terrain.WATER) return false;
        if (unit != null && unit.getOwner() == forPlayer) return false;
        return true;
    }
}
