package util;

import model.world.WorldMap;

import java.util.*;

/**
 * Prosty A* do znajdowania ścieżki na siatce kafelków.
 * Zwraca listę punktów [x,y] od startu do celu (bez startu).
 */
public class Pathfinder {

    private static final int[][] DIRS = {{1,0},{-1,0},{0,1},{0,-1}};

    /**
     * Szuka ścieżki od (sx,sy) do (tx,ty).
     * Jeśli ścieżka nie istnieje, zwraca pustą listę.
     * ignoreFinalPassable: jeśli true, cel nie musi być przejezdny (np. stanie obok).
     */
    public static List<int[]> find(WorldMap map, int sx, int sy, int tx, int ty,
                                   boolean ignoreFinalPassable) {
        if (sx == tx && sy == ty) return Collections.emptyList();

        Map<Long, Long>    cameFrom = new HashMap<>();
        Map<Long, Integer> gScore   = new HashMap<>();
        PriorityQueue<long[]> open  = new PriorityQueue<>(Comparator.comparingLong(a -> a[1]));

        long startKey = key(sx, sy);
        gScore.put(startKey, 0);
        open.add(new long[]{startKey, heuristic(sx, sy, tx, ty)});
        cameFrom.put(startKey, -1L);

        while (!open.isEmpty()) {
            long[] cur     = open.poll();
            long   curKey  = cur[0];
            int    cx      = (int)(curKey >> 20);
            int    cy      = (int)(curKey & 0xFFFFF);

            if (cx == tx && cy == ty) return reconstruct(cameFrom, curKey);

            int curG = gScore.getOrDefault(curKey, Integer.MAX_VALUE);

            for (int[] d : DIRS) {
                int nx = cx+d[0], ny = cy+d[1];
                if (!map.inBounds(nx, ny)) continue;

                boolean isGoal = (nx == tx && ny == ty);
                boolean passable = map.isPassable(nx, ny)
                        || (isGoal && ignoreFinalPassable);
                if (!passable) continue;

                long nKey = key(nx, ny);
                int  ng   = curG + 1;
                if (ng < gScore.getOrDefault(nKey, Integer.MAX_VALUE)) {
                    gScore.put(nKey, ng);
                    cameFrom.put(nKey, curKey);
                    open.add(new long[]{nKey, ng + heuristic(nx, ny, tx, ty)});
                }
            }
        }
        return Collections.emptyList(); // brak ścieżki
    }

    private static List<int[]> reconstruct(Map<Long, Long> cameFrom, long endKey) {
        LinkedList<int[]> path = new LinkedList<>();
        long cur = endKey;
        while (cameFrom.get(cur) != -1L) {
            path.addFirst(new int[]{(int)(cur >> 20), (int)(cur & 0xFFFFF)});
            cur = cameFrom.get(cur);
        }
        return path;
    }

    private static long key(int x, int y)    { return ((long) x << 20) | y; }
    private static long heuristic(int x, int y, int tx, int ty) {
        return Math.abs(x - tx) + Math.abs(y - ty);
    }
}
