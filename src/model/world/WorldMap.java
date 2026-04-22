package model.world;

import util.GameConfig;
import util.JsonParser;

import java.util.*;

/**
 * Mapa świata – rozmiar i seed ładowane z game_config.json.
 */
public class WorldMap {
    public static int WIDTH;
    public static int HEIGHT;

    private final Tile[][] tiles;
    public  final int fortressX;
    public  final int fortressY;

    public WorldMap(long seed) {
        GameConfig cfg = GameConfig.get();
        WIDTH    = cfg.mapWidth();
        HEIGHT   = cfg.mapHeight();
        tiles    = new Tile[HEIGHT][WIDTH];
        fortressX = WIDTH  / 2;
        fortressY = HEIGHT / 2;
        generate(seed, cfg);
    }

    private void generate(long seed, GameConfig cfg) {
        Random rng = new Random(seed);

        // Wypełnij trawą
        for (int y = 0; y < HEIGHT; y++)
            for (int x = 0; x < WIDTH; x++)
                tiles[y][x] = new Tile(TileType.GRASS);

        // Rzeka
        placeRiver(rng);

        // Klastry z konfiguracji
        Map<String,Object> counts = cfg.mapClusterCounts();
        counts.forEach((tileId, countObj) -> {
            int count = JsonParser.asInt(countObj);
            for (int i = 0; i < count; i++) placeCluster(rng, tileId, 1, 8);
        });

        // Góry na obrzeżach
        for (int i = 0; i < 8; i++) placeCluster(rng, TileType.MOUNTAIN, 3, 10);

        // Wyczyść obszar startowy twierdzy
        int r = cfg.fortressClearRadius();
        for (int y = fortressY-r; y <= fortressY+r; y++)
            for (int x = fortressX-r; x <= fortressX+r; x++)
                if (inBounds(x,y)) tiles[y][x] = new Tile(TileType.DIRT);

        setTile(fortressX,     fortressY,     TileType.STOCKPILE);
        setTile(fortressX + 1, fortressY,     TileType.CAMPFIRE);
        setTile(fortressX - 1, fortressY,     TileType.FLOOR);
        setTile(fortressX,     fortressY + 1, TileType.FLOOR);
        setTile(fortressX,     fortressY - 1, TileType.FLOOR);
    }

    private void placeCluster(Random rng, String tileId, int minR, int maxR) {
        int cx = rng.nextInt(WIDTH);
        int cy = rng.nextInt(HEIGHT);
        int r  = minR + rng.nextInt(Math.max(1, maxR - minR + 1));
        for (int y = cy-r; y <= cy+r; y++)
            for (int x = cx-r; x <= cx+r; x++) {
                if (!inBounds(x,y)) continue;
                if (Math.hypot(x-cx, y-cy) <= r*(0.6+rng.nextDouble()*0.5))
                    if (!tiles[y][x].getTypeId().equals(TileType.WATER))
                        tiles[y][x].setType(tileId);
            }
    }

    private void placeRiver(Random rng) {
        int y = HEIGHT/4 + rng.nextInt(HEIGHT/2);
        for (int x = 0; x < WIDTH; x++) {
            y = Math.max(2, Math.min(HEIGHT-3, y + rng.nextInt(3)-1));
            for (int w = -1; w <= 1; w++)
                if (inBounds(x, y+w)) tiles[y+w][x].setType(TileType.WATER);
        }
    }

    public Tile   getTile(int x, int y)       { return inBounds(x,y) ? tiles[y][x] : null; }
    public String getTypeId(int x, int y)     { Tile t=getTile(x,y); return t==null?null:t.getTypeId(); }
    public void   setTile(int x, int y, String id) { if (inBounds(x,y)) tiles[y][x].setType(id); }
    public boolean inBounds(int x, int y)     { return x>=0&&x<WIDTH&&y>=0&&y<HEIGHT; }
    public boolean isPassable(int x, int y)   { Tile t=getTile(x,y); return t!=null&&TileType.get(t.getTypeId()).passable; }

    /**
     * Szuka najbliższego wolnego zasobu (niezarezerwowanego przez inną jednostkę).
     * @param fx      X szukającego w kafelkach
     * @param fy      Y szukającego w kafelkach
     * @param radius  promień szukania w kafelkach
     * @return [x, y] wolnego zasobu lub null
     */
    public int[] findNearestFreeResource(int fx, int fy, int radius) {
        int najblX = -1, najblY = -1;
        double najblOdl = Double.MAX_VALUE;
        for (int y = fy - radius; y <= fy + radius; y++) {
            for (int x = fx - radius; x <= fx + radius; x++) {
                if (!inBounds(x, y)) continue;
                Tile kafelek = tiles[y][x];
                if (kafelek.isMarkedForHarvest()) continue;
                if (!TileType.get(kafelek.getTypeId()).isResource) continue;
                double odl = Math.hypot(x - fx, y - fy);
                if (odl < najblOdl) { najblOdl = odl; najblX = x; najblY = y; }
            }
        }
        return najblX == -1 ? null : new int[]{najblX, najblY};
    }

    /** Alias dla findNearestFreeResource – zachowany dla kompatybilności. */
    public int[] findNearestAnyResource(int fx, int fy, int radius) {
        return findNearestFreeResource(fx, fy, radius);
    }


    /**
     * Szuka najbliższego wolnego zasobu w podanym prostokącie mapy.
     * Używana gdy jednostka ma rozkaz ZBIERAJ z wyznaczonym obszarem.
     *
     * @param startX  X jednostki szukającej
     * @param startY  Y jednostki szukającej
     * @param x1,y1   lewy górny róg obszaru
     * @param x2,y2   prawy dolny róg obszaru
     * @return [x, y] wolnego zasobu lub null
     */
    public int[] findNearestFreeResourceInArea(int startX, int startY,
                                                int x1, int y1, int x2, int y2) {
        int najblX = -1, najblY = -1;
        double najblOdl = Double.MAX_VALUE;
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                if (!inBounds(x, y)) continue;
                Tile kafelek = tiles[y][x];
                if (kafelek.isMarkedForHarvest()) continue;
                if (!TileType.get(kafelek.getTypeId()).isResource) continue;
                double odl = Math.hypot(x - startX, y - startY);
                if (odl < najblOdl) { najblOdl = odl; najblX = x; najblY = y; }
            }
        }
        return najblX == -1 ? null : new int[]{najblX, najblY};
    }

    public int[] findNearestBuildTask(int fx, int fy) {
        int bx=-1,by=-1; double bd=Double.MAX_VALUE;
        for (int y=0;y<HEIGHT;y++)
            for (int x=0;x<WIDTH;x++) {
                Tile t=tiles[y][x];
                if (t.isMarkedForBuild()) {
                    double d=Math.hypot(x-fx,y-fy);
                    if (d<bd){bd=d;bx=x;by=y;}
                }
            }
        return bx==-1?null:new int[]{bx,by};
    }
}
