package model;

import java.util.*;

/**
 * Model mapy – siatka kafelków.
 * Odpowiada za generowanie mapy i obliczanie zasięgu ruchu (BFS).
 */
public class GameMap {
    public static final int ROWS = 10;
    public static final int COLS = 14;

    private final Tile[][] tiles;

    public GameMap() {
        tiles = new Tile[ROWS][COLS];
        generateMap();
    }

    private void generateMap() {
        Random rng = new Random(42); // stały seed = ta sama mapa każdym razem
        int[]     weights = {55, 20, 10, 15};
        Terrain[] types   = {Terrain.PLAINS, Terrain.FOREST, Terrain.MOUNTAIN, Terrain.WATER};

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int roll = rng.nextInt(100), cum = 0;
                Terrain chosen = Terrain.PLAINS;
                for (int i = 0; i < weights.length; i++) {
                    cum += weights[i];
                    if (roll < cum) { chosen = types[i]; break; }
                }
                tiles[r][c] = new Tile(r, c, chosen);
            }
        }
        for (int[] pos : new int[][]{{0,0},{0,1},{1,0},{ROWS-1,COLS-1},{ROWS-1,COLS-2},{ROWS-2,COLS-1}}) {
            if (tiles[pos[0]][pos[1]].getTerrain() == Terrain.WATER)
                tiles[pos[0]][pos[1]] = new Tile(pos[0], pos[1], Terrain.PLAINS);
        }
    }

    public Tile getTile(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return null;
        return tiles[row][col];
    }

    /**
     * BFS – zwraca siatkę pól osiągalnych przez jednostkę w tej turze.
     */
    public boolean[][] getReachableTiles(Unit unit) {
        boolean[][] reachable = new boolean[ROWS][COLS];
        int[][]     cost      = new int[ROWS][COLS];
        for (int[] row : cost) Arrays.fill(row, Integer.MAX_VALUE);

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{unit.getRow(), unit.getCol(), 0});
        cost[unit.getRow()][unit.getCol()] = 0;

        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int r = cur[0], c = cur[1], curCost = cur[2];
            for (int[] d : dirs) {
                int nr = r+d[0], nc = c+d[1];
                Tile tile = getTile(nr, nc);
                if (tile == null || !tile.isPassable(unit.getOwner())) continue;
                int newCost = curCost + tile.getTerrain().moveCost;
                if (newCost <= unit.getType().moveRange && newCost < cost[nr][nc]) {
                    cost[nr][nc] = newCost;
                    reachable[nr][nc] = true;
                    queue.add(new int[]{nr, nc, newCost});
                }
            }
        }
        reachable[unit.getRow()][unit.getCol()] = false;
        return reachable;
    }

    /**
     * Zwraca pola sąsiadujące z atakującym, na których stoi wróg.
     */
    public boolean[][] getAttackableTiles(Unit attacker) {
        boolean[][] attackable = new boolean[ROWS][COLS];
        for (int[] d : new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
            int nr = attacker.getRow()+d[0], nc = attacker.getCol()+d[1];
            Tile tile = getTile(nr, nc);
            if (tile != null && tile.getUnit() != null && tile.getUnit().getOwner() != attacker.getOwner())
                attackable[nr][nc] = true;
        }
        return attackable;
    }
}
