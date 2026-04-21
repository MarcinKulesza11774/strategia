package model.world;

/**
 * Pojedynczy kafelek mapy.
 * Przechowuje ID typu (string), nie bezpośredni obiekt TileType,
 * żeby uniknąć problemów z kolejnością inicjalizacji.
 */
public class Tile {
    private String  typeId;
    private int     harvestProgress;
    private boolean markedForHarvest;
    private boolean markedForBuild;
    private String  pendingBuildTypeId;
    private int     roomId = -1;

    public Tile(String typeId) { this.typeId = typeId; }

    public TileType getType()           { return TileType.get(typeId); }
    public String   getTypeId()         { return typeId; }
    public void     setType(String id)  { this.typeId = id; harvestProgress = 0; }

    public int  getHarvestProgress()    { return harvestProgress; }
    public void tickHarvest()           { harvestProgress++; }
    public void resetHarvestProgress()  { harvestProgress = 0; }
    public boolean isHarvestDone() {
        TileType t = getType();
        return t.isResource && harvestProgress >= t.harvestTicks;
    }

    public boolean isMarkedForHarvest()           { return markedForHarvest; }
    public void    setMarkedForHarvest(boolean v) { markedForHarvest = v; }

    public boolean isMarkedForBuild()             { return markedForBuild; }
    public String  getPendingBuildTypeId()        { return pendingBuildTypeId; }
    public void    markForBuild(String targetId)  {
        markedForBuild = true; pendingBuildTypeId = targetId;
    }
    public void clearBuildMark() {
        markedForBuild = false; pendingBuildTypeId = null;
    }

    public int  getRoomId()        { return roomId; }
    public void setRoomId(int id)  { this.roomId = id; }
}
