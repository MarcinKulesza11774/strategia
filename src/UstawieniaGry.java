/**
 * Parametry konfiguracyjne gry wybierane przez gracza w menu startowym.
 */
public class UstawieniaGry {
    public final int rows;
    public final int cols;
    public final int liczbaRegionow;
    public final int liczbaZalazkowTerenu;
    public final int liczbaAI;
    public final int liczbaTur;

    public UstawieniaGry(int rows, int cols, int liczbaRegionow,
                         int liczbaZalazkowTerenu, int liczbaAI, int liczbaTur) {
        this.rows = rows;
        this.cols = cols;
        this.liczbaRegionow = liczbaRegionow;
        this.liczbaZalazkowTerenu = liczbaZalazkowTerenu;
        this.liczbaAI = liczbaAI;
        this.liczbaTur = liczbaTur;
    }
}
