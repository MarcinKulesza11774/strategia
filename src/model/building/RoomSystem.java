package model.building;

import model.world.TileType;
import model.world.WorldMap;

import java.util.*;

/**
 * Wykrywa pokoje na mapie i oblicza buffy sąsiedztwa.
 */
public class RoomSystem {

    private final WorldMap   map;
    private final List<Room> rooms = new ArrayList<>();

    public RoomSystem(WorldMap map) { this.map = map; }

    public void rescan() {
        rooms.clear();
        boolean[][] visited = new boolean[WorldMap.HEIGHT][WorldMap.WIDTH];

        for (int y = 0; y < WorldMap.HEIGHT; y++) {
            for (int x = 0; x < WorldMap.WIDTH; x++) {
                if (visited[y][x]) continue;
                String tid = map.getTypeId(x, y);
                if (tid == null) continue;
                TileType tt = TileType.get(tid);
                if (!tt.isFloor && !tt.isFurniture()) continue;
                if (!tt.isFloor) continue; // BFS starts only from FLOOR

                Set<long[]>  floorTiles = new LinkedHashSet<>();
                Set<String>  furniture  = new LinkedHashSet<>();
                Queue<int[]> queue      = new LinkedList<>();
                queue.add(new int[]{x, y});
                visited[y][x] = true;
                boolean enclosed = true;

                while (!queue.isEmpty()) {
                    int[] cur = queue.poll();
                    int cx = cur[0], cy = cur[1];
                    floorTiles.add(new long[]{pack(cx, cy)});

                    TileType ct = TileType.get(map.getTypeId(cx, cy));
                    if (ct.isFurniture()) furniture.add(ct.id);

                    for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        int nx = cx+d[0], ny = cy+d[1];
                        if (!map.inBounds(nx, ny)) { enclosed = false; continue; }
                        if (visited[ny][nx]) continue;
                        String ntid = map.getTypeId(nx, ny);
                        if (ntid == null) { enclosed = false; continue; }
                        TileType nt = TileType.get(ntid);
                        if (nt.isWall || ntid.equals(TileType.DOOR) || ntid.equals(TileType.GATE)) continue;
                        if (nt.isFloor || nt.isFurniture()) {
                            visited[ny][nx] = true;
                            queue.add(new int[]{nx, ny});
                        } else {
                            enclosed = false;
                        }
                    }
                }

                if (!enclosed || floorTiles.size() < 2) continue;
                matchRoom(floorTiles, furniture);
            }
        }

        // Reset room IDs on map
        for (int y2 = 0; y2 < WorldMap.HEIGHT; y2++)
            for (int x2 = 0; x2 < WorldMap.WIDTH; x2++)
                if (map.getTile(x2, y2) != null) map.getTile(x2, y2).setRoomId(-1);

        for (Room room : rooms)
            for (long[] t : room.getTilePacked()) {
                int rx = (int)(t[0] >> 20), ry = (int)(t[0] & 0xFFFFF);
                if (map.getTile(rx, ry) != null) map.getTile(rx, ry).setRoomId(room.getId());
            }

        calcAdjacency();
    }

    private void matchRoom(Set<long[]> tiles, Set<String> furniture) {
        for (RoomDef def : RoomDef.all()) {
            if (tiles.size() < def.minSize) continue;
            if (furniture.containsAll(def.requiredFurniture)) {
                rooms.add(new Room(def, tiles, furniture));
                return;
            }
        }
    }

    private void calcAdjacency() {
        for (Room r : rooms) r.resetBuffs();
        for (int i = 0; i < rooms.size(); i++)
            for (int j = i+1; j < rooms.size(); j++)
                if (areAdjacent(rooms.get(i), rooms.get(j))) {
                    rooms.get(i).addAdjacentRoom(rooms.get(j).getId());
                    rooms.get(j).addAdjacentRoom(rooms.get(i).getId());
                    rooms.get(i).applyAdjacencyBonus(rooms.get(j).getDef());
                    rooms.get(j).applyAdjacencyBonus(rooms.get(i).getDef());
                }
    }

    private boolean areAdjacent(Room a, Room b) {
        for (long[] ta : a.getTilePacked()) {
            int ax = (int)(ta[0] >> 20), ay = (int)(ta[0] & 0xFFFFF);
            for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int nx = ax+d[0], ny = ay+d[1];
                String ntid = map.getTypeId(nx, ny);
                if (ntid == null) continue;
                TileType nt = TileType.get(ntid);
                if (!nt.isWall && !ntid.equals(TileType.DOOR) && !ntid.equals(TileType.GATE)) continue;
                if (b.containsTile(nx+d[0], ny+d[1])) return true;
            }
        }
        return false;
    }

    public List<Room> getRooms()        { return rooms; }

    public Room getRoomAt(int x, int y) {
        for (Room r : rooms) if (r.containsTile(x, y)) return r;
        return null;
    }

    public Room getRoomById(int id) {
        return rooms.stream().filter(r -> r.getId() == id).findFirst().orElse(null);
    }

    public int[] getBuffsAt(int x, int y) {
        Room r = getRoomAt(x, y);
        if (r == null) return new int[]{0,0,0,0};
        return new int[]{r.getTotalSpeedBuff(), r.getTotalAttackBuff(),
                         r.getTotalDefenseBuff(), r.getTotalHealPerTick()};
    }

    private static long pack(int x, int y) { return ((long)x << 20) | y; }
}
