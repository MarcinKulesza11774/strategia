/**
 * Miasto na mapie gry.
 */
public class Miasto {
    private final String nazwa;
    private final int row;
    private final int col;
    private Gracz wlasciciel;
    private int poziom;
    private int punktyRozwoju;

    private static final int PUNKTY_DO_AWANSU = 30;
    public static final int KOSZT_JEDNOSTKI = 20;

    public Miasto(String nazwa, int row, int col, Gracz wlasciciel) {
        this.nazwa = nazwa;
        this.row = row;
        this.col = col;
        this.wlasciciel = wlasciciel;
        this.poziom = 1;
    }

    public int getProdukcjaZlota() { return 5 * poziom; }

    public boolean dodajPunktyRozwoju(int punkty) {
        punktyRozwoju += punkty;
        if (punktyRozwoju >= PUNKTY_DO_AWANSU) {
            poziom++;
            punktyRozwoju -= PUNKTY_DO_AWANSU;
            return true;
        }
        return false;
    }

    public String getNazwa()      { return nazwa; }
    public int getRow()           { return row; }
    public int getCol()           { return col; }
    public Gracz getWlasciciel()  { return wlasciciel; }
    public int getPoziom()        { return poziom; }

    public void setWlasciciel(Gracz gracz) { this.wlasciciel = gracz; }

    @Override
    public String toString() { return nazwa + " (poz." + poziom + ")"; }
}
