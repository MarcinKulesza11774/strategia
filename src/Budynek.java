import java.awt.*;

public enum Budynek implements Kosztowny {

    //               nazwa                kolor                        bonusZłoto bonusNauka jedzenie  pop   złoto
    TOWNHALL          ("Town Hall",          new Color(160, 130, 80),  0.0,       0.0,       10,       0,    0),
    POLE_UPRAWNE      ("Pole uprawne",       new Color(120, 200, 80),  0.0,       0.0,       20,       100,  250),
    UNIWERSYTET       ("Uniwersytet",        new Color(80,  120, 220), 0.0,       0.5,       0,        200,  750),
    DZIELNICA_KUPIECKA("Dzielnica kupiecka", new Color(220, 180, 60),  0.5,       0.0,       2,        150,  250);

    @Override public String getNazwa()        { return nazwa; }
    @Override public int getKosztWZlocie()    { return kosztWZlocie; }
    @Override public int getKosztWPopulacji() { return kosztWPopulacji; }

    public final String nazwa;
    public final Color kolor;
    public final double bonusDoMnoznikaZlota;
    public final double bonusDoMnoznikaNauki;
    public final int generowanePozywienie;
    public final int kosztWPopulacji;
    public final int kosztWZlocie;

    Budynek(String nazwa, Color kolor, double bonusDoMnoznikaZlota, double bonusDoMnoznikaNauki,
            int generowanePozywienie, int kosztWPopulacji, int kosztWZlocie) {
        this.nazwa = nazwa;
        this.kolor = kolor;
        this.bonusDoMnoznikaZlota = bonusDoMnoznikaZlota;
        this.bonusDoMnoznikaNauki = bonusDoMnoznikaNauki;
        this.generowanePozywienie = generowanePozywienie;
        this.kosztWPopulacji = kosztWPopulacji;
        this.kosztWZlocie = kosztWZlocie;
    }

    public static Budynek[] dostepneDoBudowy() {
        return java.util.Arrays.stream(values())
                .filter(b -> b != TOWNHALL)
                .toArray(Budynek[]::new);
    }
}