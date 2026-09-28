public class JednostkaNaMapie extends ObiektNaMapie {
    private Jednostka rodzajJednostki;

    private int punktyZycia;
    private int maksymalnePunktyZycia;
    private int obrazenia;
    private int maksymalnyRuch;
    private int pozostalyRuch;
    private int zasiegAtaku;
    private boolean zaatakowalaWTejTurze;

    private int poziomAtaku;
    private int poziomZycia;
    private int poziomRuchu;

    public static final int KOSZT_ULEPSZENIA     = 30;
    public static final int MAX_POZIOM_ULEPSZENIA = 3;

    public JednostkaNaMapie(int row, int col, Gracz wlasciciel) {
        this.row = row;
        this.col = col;
        this.wlasciciel = wlasciciel;
        this.maksymalnePunktyZycia = 100;
        this.punktyZycia = 100;
        this.obrazenia = 30;
        this.maksymalnyRuch = 3;
        this.pozostalyRuch = 3;
        this.zasiegAtaku = 1;
    }

    public JednostkaNaMapie(int row, int col, Gracz wlasciciel, Jednostka rodzajJednostki){
        this.row = row;
        this.col = col;
        this.wlasciciel = wlasciciel;
        this.rodzajJednostki = rodzajJednostki;
        this.maksymalnePunktyZycia = rodzajJednostki.hp;
        this.punktyZycia = rodzajJednostki.hp;
        this.obrazenia = rodzajJednostki.damage;
        this.maksymalnyRuch = rodzajJednostki.zasiegRuchu;
        this.pozostalyRuch = rodzajJednostki.zasiegRuchu;
        this.zasiegAtaku = rodzajJednostki.zasiegAtaku;
    }

    public void rozpocznijNowaTure() { pozostalyRuch = maksymalnyRuch; zaatakowalaWTejTurze = false;}

    public boolean przesun(int newRow, int newCol, int kosztRuchu) {
        if (pozostalyRuch < kosztRuchu) return false;
        row = newRow;
        col = newCol;
        pozostalyRuch -= kosztRuchu;
        return true;
    }

    public boolean atakuj(JednostkaNaMapie cel) {
        cel.otrzymajObrazenia(obrazenia);
        zaatakowalaWTejTurze = true;
        return !cel.czyZyje();
    }

    public boolean czyMozeAtakowac() { return !zaatakowalaWTejTurze; }

    public void otrzymajObrazenia(int ilosc) {
        punktyZycia = Math.max(0, punktyZycia - ilosc);
    }

    public boolean moznaUlepszycAtak()  { return poziomAtaku < MAX_POZIOM_ULEPSZENIA; }
    public boolean moznaUlepszycZycie() { return poziomZycia < MAX_POZIOM_ULEPSZENIA; }
    public boolean moznaUlepszycRuch()  { return poziomRuchu < MAX_POZIOM_ULEPSZENIA; }

    public void ulepszAtak()  { poziomAtaku++; obrazenia += 15; }
    public void ulepszZycie() { poziomZycia++; maksymalnePunktyZycia += 50; punktyZycia += 50; }
    public void ulepszRuch()  { poziomRuchu++; maksymalnyRuch++; }

    public boolean czyZyje()   { return punktyZycia > 0; }
    public boolean czyCzynna() { return pozostalyRuch > 0; }

    public int getPunktyZycia()            { return punktyZycia; }
    public int getMaksymalnePunktyZycia()  { return maksymalnePunktyZycia; }
    public int getObrazenia()              { return obrazenia; }
    public int getMaksymalnyRuch()         { return maksymalnyRuch; }
    public int getPozostalyruch()          { return pozostalyRuch; }
    public int getZasiegAtaku()            { return zasiegAtaku; }
    public int getPoziomAtaku()            { return poziomAtaku; }
    public int getPoziomZycia()            { return poziomZycia; }
    public int getPoziomRuchu()            { return poziomRuchu; }
    public Jednostka getRodzajJednostki()  { return rodzajJednostki; }
}