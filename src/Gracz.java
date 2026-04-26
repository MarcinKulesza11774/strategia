import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Reprezentuje gracza (człowieka lub AI).
 */
public class Gracz {
    private final String nazwa;
    private final Color kolor;
    private final boolean czyAI;
    private int zloto;

    private final List<Miasto> miasta = new ArrayList<>();
    private final List<Jednostka> jednostki = new ArrayList<>();

    public Gracz(String nazwa, Color kolor, boolean czyAI) {
        this.nazwa = nazwa;
        this.kolor = kolor;
        this.czyAI = czyAI;
        this.zloto = 50;
    }

    /** Koszt budowy kolejnego miasta rośnie z ich liczbą. */
    public int getKosztBudowyMiasta() {
        return 50 + miasta.size() * 30;
    }

    public void zbierzZasobyZMiast() {
        for (Miasto miasto : miasta) {
            zloto += miasto.getProdukcjaZlota();
        }
    }

    public String getNazwa()    { return nazwa; }
    public Color getKolor()     { return kolor; }
    public boolean isCzyAI()    { return czyAI; }
    public int getZloto()       { return zloto; }

    public void dodajZloto(int n)   { zloto += n; }
    public void odejmijZloto(int n) { zloto -= n; }

    public List<Miasto> getMiasta()       { return miasta; }
    public List<Jednostka> getJednostki() { return jednostki; }

    public void dodajMiasto(Miasto m)    { miasta.add(m); }
    public void usunMiasto(Miasto m)     { miasta.remove(m); }
    public void dodajJednostke(Jednostka j) { jednostki.add(j); }
    public void usunJednostke(Jednostka j)  { jednostki.remove(j); }
}
