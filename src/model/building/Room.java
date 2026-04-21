package model.building;

import java.util.*;

/**
 * Instancja wykrytego pokoju na mapie.
 */
public class Room {
    private static int nextId = 1;

    private final int         id;
    private final RoomDef     def;
    private final Set<long[]> tiles;       // packed (x<<20)|y
    private final Set<String> furniture;
    private final List<Integer> adjacentRoomIds = new ArrayList<>();

    private int totalSpeedBuff, totalAttackBuff, totalDefenseBuff, totalHealPerTick;

    public Room(RoomDef def, Set<long[]> tiles, Set<String> furniture) {
        this.id        = nextId++;
        this.def       = def;
        this.tiles     = tiles;
        this.furniture = furniture;
        recalcBase();
    }

    private void recalcBase() {
        totalSpeedBuff   = def.selfSpeedBuff;
        totalAttackBuff  = def.selfAttackBuff;
        totalDefenseBuff = def.selfDefenseBuff;
        totalHealPerTick = def.selfHealPerTick;
    }

    public void applyAdjacencyBonus(RoomDef neighbor) {
        int[] b = def.adjacencyBonus.get(neighbor.id);
        if (b == null) return;
        totalSpeedBuff   += b[0];
        totalAttackBuff  += b[1];
        totalDefenseBuff += b[2];
        totalHealPerTick += b[3];
    }

    public void resetBuffs() { recalcBase(); adjacentRoomIds.clear(); }

    public boolean containsTile(int x, int y) {
        long k = pack(x, y);
        for (long[] t : tiles) if (t[0] == k) return true;
        return false;
    }

    private static long pack(int x, int y) { return ((long)x << 20) | y; }

    public int           getId()               { return id; }
    public RoomDef       getDef()              { return def; }
    public Set<long[]>   getTilePacked()       { return tiles; }
    public Set<String>   getFurniture()        { return furniture; }
    public int           getTileCount()        { return tiles.size(); }
    public List<Integer> getAdjacentRoomIds()  { return adjacentRoomIds; }
    public void          addAdjacentRoom(int r){ adjacentRoomIds.add(r); }
    public int getTotalSpeedBuff()    { return totalSpeedBuff; }
    public int getTotalAttackBuff()   { return totalAttackBuff; }
    public int getTotalDefenseBuff()  { return totalDefenseBuff; }
    public int getTotalHealPerTick()  { return totalHealPerTick; }

    public int[] getCenter() {
        long sx = 0, sy = 0;
        for (long[] t : tiles) { sx += (t[0] >> 20); sy += (t[0] & 0xFFFFF); }
        int n = tiles.size();
        return new int[]{(int)(sx/n), (int)(sy/n)};
    }
}
