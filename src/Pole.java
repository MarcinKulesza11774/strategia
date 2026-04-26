/**
 * Pojedyncze pole na mapie gry.
 */
public class Pole {
    private final int row;
    private final int col;
    private final Teren teren;
    private final int numerRegionu;
    private Miasto miasto;
    private Jednostka jednostka;

    public Pole(int row, int col, Teren teren, int numerRegionu) {
        this.row = row;
        this.col = col;
        this.teren = teren;
        this.numerRegionu = numerRegionu;
    }

    public int getRow()             { return row; }
    public int getCol()             { return col; }
    public Teren getTeren()         { return teren; }
    public int getNumerRegionu()    { return numerRegionu; }
    public Miasto getMiasto()       { return miasto; }
    public Jednostka getJednostka() { return jednostka; }

    public void setMiasto(Miasto miasto)          { this.miasto = miasto; }
    public void setJednostka(Jednostka jednostka) { this.jednostka = jednostka; }

    public boolean czyPrzejezdne() { return teren.kosztRuchu < 99; }
}
