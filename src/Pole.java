/**
 * Pojedyncze pole na mapie gry.
 * Trzyma budynek (Town Hall, pole uprawne itd.) zamiast osobnego obiektu Miasto –
 * wszystkie budynki są tym samym typem z punktu widzenia mapy.
 */
public class Pole {
    private final int row;
    private final int col;
    private final Teren teren;
    private final int numerRegionu;
    private BudynekWMiescie budynek; // null jeśli puste
    private JednostkaNaMapie jednostka;

    public Pole(int row, int col, Teren teren, int numerRegionu) {
        this.row = row;
        this.col = col;
        this.teren = teren;
        this.numerRegionu = numerRegionu;
    }

    public int getRow()               { return row; }
    public int getCol()               { return col; }
    public Teren getTeren()           { return teren; }
    public int getNumerRegionu()      { return numerRegionu; }
    public BudynekWMiescie getBudynek()   { return budynek; }
    public JednostkaNaMapie getJednostka()   { return jednostka; }

    public void setBudynek(BudynekWMiescie budynek)    { this.budynek = budynek; }
    public void setJednostka(JednostkaNaMapie jednostka)      { this.jednostka = jednostka; }

    public boolean czyPrzejezdne()    { return teren.kosztRuchu < 99; }
}
