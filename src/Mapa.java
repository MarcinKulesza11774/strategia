import java.util.Random;

/**
 * Mapa gry.
 *
 * Generowanie terenu: losujemy N "zalążków" terenu (jak centra regionów),
 * każdemu przypisujemy losowy typ terenu. Następnie każde pole dostaje
 * typ terenu swojego najbliższego zalążka – identyczna metoda co Voronoi
 * dla regionów administracyjnych. Zalążki tego samego typu mogą leżeć
 * obok siebie, więc strefy terenu naturalnie się łączą i tworzą skupiska.
 *
 * Regiony (administracyjne): osobny zestaw centrów Voronoi.
 * W każdym regionie można postawić jedno miasto.
 */
public class Mapa {
    public static final int ROWS = 100;
    public static final int COLS = 100;
    public static final int LICZBA_REGIONOW = 50;

    private static final int LICZBA_ZALĄŻKÓW_TERENU = 150;

    private final Pole[][] pola = new Pole[ROWS][COLS];
    private final int[][] centryRegionow = new int[LICZBA_REGIONOW][2]; // [r][0]=row, [r][1]=col

    public Mapa(long seed) {
        Random rng = new Random(seed);
        generujRegiony(rng);
        generujTeren(rng);
    }

    // -------------------------------------------------------------------------
    // Regiony Voronoi
    // -------------------------------------------------------------------------

    private void generujRegiony(Random rng) {
        for (int r = 0; r < LICZBA_REGIONOW; r++) {
            centryRegionow[r][0] = rng.nextInt(ROWS);
            centryRegionow[r][1] = rng.nextInt(COLS);
        }
    }

    private int regionDlaPola(int row, int col) {
        int najblizszy = 0;
        int minDist = Integer.MAX_VALUE;
        for (int r = 0; r < LICZBA_REGIONOW; r++) {
            int dr = row - centryRegionow[r][0];
            int dc = col - centryRegionow[r][1];
            int dist = dr * dr + dc * dc;
            if (dist < minDist) { minDist = dist; najblizszy = r; }
        }
        return najblizszy;
    }

    // -------------------------------------------------------------------------
    // Teren Voronoi
    // -------------------------------------------------------------------------

    private void generujTeren(Random rng) {
        // Losuj centra zalążków i przypisz każdemu typ terenu
        int[][] centryTerenu = new int[LICZBA_ZALĄŻKÓW_TERENU][2];
        Teren[] typyZalążków = new Teren[LICZBA_ZALĄŻKÓW_TERENU];

        for (int i = 0; i < LICZBA_ZALĄŻKÓW_TERENU; i++) {
            centryTerenu[i][0] = rng.nextInt(ROWS);
            centryTerenu[i][1] = rng.nextInt(COLS);
            typyZalążków[i]    = losujTeren(rng);
        }

        // Każde pole dostaje typ terenu najbliższego zalążka
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int najblizszy = 0;
                int minDist = Integer.MAX_VALUE;
                for (int i = 0; i < LICZBA_ZALĄŻKÓW_TERENU; i++) {
                    int dr = row - centryTerenu[i][0];
                    int dc = col - centryTerenu[i][1];
                    int dist = dr * dr + dc * dc;
                    if (dist < minDist) { minDist = dist; najblizszy = i; }
                }
                pola[row][col] = new Pole(row, col, typyZalążków[najblizszy], regionDlaPola(row, col));
            }
        }
    }

    private Teren losujTeren(Random rng) {
        // Wagi szans bazowych
        int[] wagi = {6, 4, 3, 4, 2, 1}; // ROWNINA, LAS, GORY, WODA, PUSTYNIA, SNIEG
        Teren[] tereny = Teren.values();
        int suma = 0;
        for (int w : wagi) suma += w;
        int wylosowany = rng.nextInt(suma);
        int narastajaca = 0;
        for (int i = 0; i < tereny.length; i++) {
            narastajaca += wagi[i];
            if (wylosowany < narastajaca) return tereny[i];
        }
        return Teren.ROWNINA;
    }

    // -------------------------------------------------------------------------
    // Dostęp
    // -------------------------------------------------------------------------

    public Pole getPole(int row, int col) {
        if (!czyWMapie(row, col)) return null;
        return pola[row][col];
    }

    public boolean czyWMapie(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    public boolean czyRegionMaMiasto(int numerRegionu) {
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++)
                if (pola[row][col].getNumerRegionu() == numerRegionu
                        && pola[row][col].getMiasto() != null)
                    return true;
        return false;
    }

    public int getLiczbaWierszy() { return ROWS; }
    public int getLiczbaKolumn()  { return COLS; }
    public int getLiczbaRegionow(){ return LICZBA_REGIONOW; }
}
