/**
 * Jednostka wojskowa na mapie.
 * Statystyki można ulepszać za złoto.
 */
public class Jednostka {
    private int row;
    private int col;
    private Gracz wlasciciel;

    private int punktyZycia;
    private int maksymalnePunktyZycia;
    private int obrazenia;
    private int maksymalnyRuch;
    private int pozostalyRuch;

    private int poziomAtaku;
    private int poziomZycia;
    private int poziomRuchu;

    public static final int KOSZT_ULEPSZENIA    = 30;
    public static final int MAX_POZIOM_ULEPSZENIA = 10;

    public Jednostka(int row, int col, Gracz wlasciciel) {
        this.row = row;
        this.col = col;
        this.wlasciciel = wlasciciel;
        this.maksymalnePunktyZycia = 100;
        this.punktyZycia = 100;
        this.obrazenia = 30;
        this.maksymalnyRuch = 10;
        this.pozostalyRuch = 10;
    }

    public void rozpocznijNowaTure() { pozostalyRuch = maksymalnyRuch; }

    public boolean przesun(int newRow, int newCol, int kosztRuchu) {
        if (pozostalyRuch < kosztRuchu) return false;
        row = newRow;
        col = newCol;
        pozostalyRuch -= kosztRuchu;
        return true;
    }

    /** Atakuje cel; zwraca true jeśli cel ginie. */
    public boolean atakuj(Jednostka cel) {
        cel.otrzymajObrazenia(obrazenia);
        if (cel.czyZyje()) this.otrzymajObrazenia(cel.obrazenia / 2);
        pozostalyRuch = 0;
        return !cel.czyZyje();
    }

    public void otrzymajObrazenia(int ilosc) {
        punktyZycia = Math.max(0, punktyZycia - ilosc);
    }

    public boolean moznaUlepszycAtak()  { return poziomAtaku < MAX_POZIOM_ULEPSZENIA; }
    public boolean moznaUlepszycZycie() { return poziomZycia < MAX_POZIOM_ULEPSZENIA; }
    public boolean moznaUlepszycRuch()  { return poziomRuchu < MAX_POZIOM_ULEPSZENIA; }

    public void ulepszAtak()  { poziomAtaku++; obrazenia += 15; }
    public void ulepszZycie() { poziomZycia++; maksymalnePunktyZycia += 50; punktyZycia += 50; }
    public void ulepszRuch()  { poziomRuchu+=3; maksymalnyRuch+=3; }

    public boolean czyZyje()   { return punktyZycia > 0; }
    public boolean czyCzynna() { return pozostalyRuch > 0; }

    public int getRow()                    { return row; }
    public int getCol()                    { return col; }
    public Gracz getWlasciciel()           { return wlasciciel; }
    public int getPunktyZycia()            { return punktyZycia; }
    public int getMaksymalnePunktyZycia()  { return maksymalnePunktyZycia; }
    public int getObrazenia()              { return obrazenia; }
    public int getMaksymalnyRuch()         { return maksymalnyRuch; }
    public int getPozostalyruch()          { return pozostalyRuch; }
    public int getPoziomAtaku()            { return poziomAtaku; }
    public int getPoziomZycia()            { return poziomZycia; }
    public int getPoziomRuchu()            { return poziomRuchu; }
}
