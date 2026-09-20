import java.awt.*;

public enum Budynek {
    TOWNHALL ("Town Hall",  new Color(160, 210, 110), 40, 10, 10, 1000, 1000),
    POLE ("pole",  new Color(160, 210, 110), 5, 1, 20, 250, 100),
    UNIWERSYTET ("uniwersytet",  new Color(160, 210, 110), 3, 50, 0, 250, 750),
    DZIELNICAKUPIECKA ("dzielnica kupiecka",  new Color(160, 210, 110), 100, 3,2, 150, 250);

    public final String nazwa;
    public final Color kolor;
    public int generowaneZloto;
    public int generowanaNauka;
    public int generowanePozywienie;
    public int kosztWPopulacji;
    public int kosztWZlocie;

    Budynek(String nazwa, Color kolor, int generowaneZloto, int generowanaNauka, int generowanePozywienie, int kosztWPopulacji, int kosztWZlocie) {
        this.nazwa = nazwa;
        this.kolor = kolor;
        this.generowaneZloto = generowaneZloto;
        this.generowanaNauka = generowanaNauka;
        this.generowanePozywienie = generowanePozywienie;
        this.kosztWPopulacji = kosztWPopulacji;
        this.kosztWZlocie = kosztWZlocie;
    }
}
