import java.awt.Color;

/**
 * Rodzaje terenu na mapie gry.
 * Każdy teren ma kolor wyświetlania oraz koszt wejścia jednostką.
 */
public enum Teren {
    ROWNINA ("Równina",  new Color(160, 210, 110), 1),
    LAS     ("Las",      new Color(60,  130,  60), 2),
    GORY    ("Góry",     new Color(150, 130, 110), 3),
    WODA    ("Woda",     new Color(80,  150, 220), 99),
    PUSTYNIA("Pustynia", new Color(220, 200, 120), 2),
    SNIEG   ("Śnieg",    new Color(230, 240, 250), 2);

    public final String nazwa;
    public final Color kolor;
    public final int kosztRuchu; // 99 = nieprzejezdny

    Teren(String nazwa, Color kolor, int kosztRuchu) {
        this.nazwa = nazwa;
        this.kolor = kolor;
        this.kosztRuchu = kosztRuchu;
    }
}
