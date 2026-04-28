import java.util.Random;

/**
 * Mapa gry – siatka pól z terenem i regionami.
 * Rozmiar i parametry generowania przekazywane przez UstawieniaGry.
 *
 * Teren i regiony generowane tą samą metodą Voronoi:
 * losuj centra, każde pole należy do najbliższego centrum.
 */
public class Mapa {
    private final int rows;
    private final int cols;
    private final int liczbaRegionow;
    private final Pole[][] pola;

    public Mapa(UstawieniaGry ust, long seed) {
        this.rows = ust.rows;
        this.cols = ust.cols;
        this.liczbaRegionow = ust.liczbaRegionow;
        this.pola = new Pole[rows][cols];
        generuj(ust.liczbaZalazkowTerenu, seed);
    }

    private void generuj(int liczbaZalazkowTerenu, long seed) {
        Random rng = new Random(seed);

        // --- Regiony Voronoi ---
        int[][] centryRegionow = new int[liczbaRegionow][2];
        for (int r = 0; r < liczbaRegionow; r++) {
            centryRegionow[r][0] = rng.nextInt(rows);
            centryRegionow[r][1] = rng.nextInt(cols);
        }

        // --- Teren Voronoi ---
        int[][] centryTerenu = new int[liczbaZalazkowTerenu][2];
        Teren[] typyZalazków = new Teren[liczbaZalazkowTerenu];
        for (int i = 0; i < liczbaZalazkowTerenu; i++) {
            centryTerenu[i][0] = rng.nextInt(rows);
            centryTerenu[i][1] = rng.nextInt(cols);
            typyZalazków[i]    = losujTeren(rng);
        }

        // --- Przypisz pola ---
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int region = najblizszyCentrum(row, col, centryRegionow);
                int zalazek = najblizszyCentrum(row, col, centryTerenu);
                pola[row][col] = new Pole(row, col, typyZalazków[zalazek], region);
            }
        }
    }

    private int najblizszyCentrum(int row, int col, int[][] centra) {
        int najblizszy = 0, minDist = Integer.MAX_VALUE;
        for (int i = 0; i < centra.length; i++) {
            int dr = row - centra[i][0], dc = col - centra[i][1];
            int dist = dr * dr + dc * dc;
            if (dist < minDist) { minDist = dist; najblizszy = i; }
        }
        return najblizszy;
    }

    private Teren losujTeren(Random rng) {
        int[] wagi = {6, 4, 3, 4, 2, 1};
        Teren[] tereny = Teren.values();
        int suma = 0;
        for (int w : wagi) suma += w;
        int wylosowany = rng.nextInt(suma);
        int n = 0;
        for (int i = 0; i < tereny.length; i++) {
            n += wagi[i];
            if (wylosowany < n) return tereny[i];
        }
        return Teren.ROWNINA;
    }

    public Pole getPole(int row, int col) {
        if (!czyWMapie(row, col)) return null;
        return pola[row][col];
    }

    public boolean czyWMapie(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public boolean czyRegionMaMiasto(int numerRegionu) {
        for (int row = 0; row < rows; row++)
            for (int col = 0; col < cols; col++)
                if (pola[row][col].getNumerRegionu() == numerRegionu
                        && pola[row][col].getMiasto() != null)
                    return true;
        return false;
    }

    public int getLiczbaWierszy()  { return rows; }
    public int getLiczbaKolumn()   { return cols; }
    public int getLiczbaRegionow() { return liczbaRegionow; }
}
