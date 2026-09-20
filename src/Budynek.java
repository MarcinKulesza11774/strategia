import java.awt.*;

public enum Budynek {
    TOWNHALL         ("Town Hall",          new Color(160, 130, 80),  40,  10,  10, 0,    0),
    POLE_UPRAWNE     ("Pole uprawne",       new Color(120, 200, 80),  5,   1,   20, 100,  250),
    UNIWERSYTET      ("Uniwersytet",        new Color(80,  120, 220), 3,   50,  0,  200,  750),
    DZIELNICA_KUPIECKA("Dzielnica kupiecka",new Color(220, 180, 60),  100, 3,   2,  150,  250);

    public final String nazwa;
    public final Color kolor;
    public final int generowaneZloto;
    public final int generowanaNauka;
    public final int generowanePozywienie;
    public final int kosztWPopulacji;
    public final int kosztWZlocie;

    Budynek(String nazwa, Color kolor, int generowaneZloto, int generowanaNauka,
            int generowanePozywienie, int kosztWPopulacji, int kosztWZlocie) {
        this.nazwa = nazwa;
        this.kolor = kolor;
        this.generowaneZloto = generowaneZloto;
        this.generowanaNauka = generowanaNauka;
        this.generowanePozywienie = generowanePozywienie;
        this.kosztWPopulacji = kosztWPopulacji;
        this.kosztWZlocie = kosztWZlocie;
    }
}
