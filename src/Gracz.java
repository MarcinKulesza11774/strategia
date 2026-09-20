import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Gracz {
    private final String nazwa;
    private final Color kolor;
    private final boolean czyAI;

    private int zloto;
    private int nauka;
    private int populacjaCaLkowita;
    private int populacjaZagospodarowana; // zajęta przez budynki i jednostki
    private int pozywienie;

    private static final int POZYWIENIE_NA_POPULACJE = 10; // ile pożywienia = +1 populacja/turę
    private static final int KOSZT_BUDOWY_MIASTA = 50;

    private final List<Miasto> miasta = new ArrayList<>();
    private final List<JednostkaNaMapie> jednostki = new ArrayList<>();

    public Gracz(String nazwa, Color kolor, boolean czyAI) {
        this.nazwa = nazwa;
        this.kolor = kolor;
        this.czyAI = czyAI;
        this.zloto = 50;
        this.populacjaCaLkowita = 10;
    }

    /** Zbiera złoto, naukę i pożywienie ze wszystkich miast. Przyrost populacji z pożywienia. */
    public void zbierzZasobyZMiast() {
        for (Miasto m : miasta) {
            zloto += m.getProdukcjaZlota();
            nauka += m.getProdukcjaNauki();
            pozywienie += m.getProdukcjaPozywienia();
        }
        populacjaCaLkowita += pozywienie / POZYWIENIE_NA_POPULACJE;
    }

    public int getWolnaPopulacja() {
        return populacjaCaLkowita - populacjaZagospodarowana;
    }

    public boolean moznaWydacPopulacje(int ilosc) {
        return getWolnaPopulacja() >= ilosc;
    }

    public void wydajPopulacje(int ilosc)    { populacjaZagospodarowana += ilosc; }
    public void zwrocPopulacje(int ilosc)    { populacjaZagospodarowana -= ilosc; }

    public int getKosztBudowyMiasta()        { return KOSZT_BUDOWY_MIASTA; }

    public String getNazwa()                 { return nazwa; }
    public Color getKolor()                  { return kolor; }
    public boolean isCzyAI()                 { return czyAI; }
    public int getZloto()                    { return zloto; }
    public int getNauka()                    { return nauka; }
    public int getPopulacjaCaLkowita()       { return populacjaCaLkowita; }
    public int getPopulacjaZagospodarowana() { return populacjaZagospodarowana; }
    public int getPozywienie()               { return pozywienie; }

    public void dodajZloto(int n)   { zloto += n; }
    public void odejmijZloto(int n) { zloto -= n; }

    public List<Miasto> getMiasta()         { return miasta; }
    public List<JednostkaNaMapie> getJednostki()   { return jednostki; }

    public void dodajMiasto(Miasto m)       { miasta.add(m); }
    public void usunMiasto(Miasto m)        { miasta.remove(m); }
    public void dodajJednostke(JednostkaNaMapie j) { jednostki.add(j); }
    public void usunJednostke(JednostkaNaMapie j)  { jednostki.remove(j); }
}
