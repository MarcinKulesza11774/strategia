import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Gracz {
    private final String nazwa;
    private final Color kolor;
    private final boolean czyAI;

    private int zloto;
    private int nauka;
    private double mnoznikZlota = 1.0;

    private static final int KOSZT_BUDOWY_MIASTA = 50;

    private final List<Miasto> miasta = new ArrayList<>();
    private final List<JednostkaNaMapie> jednostki = new ArrayList<>();
    private final Set<Perk> zakupionePerki = new HashSet<>();

    public Gracz(String nazwa, Color kolor, boolean czyAI) {
        this.nazwa = nazwa;
        this.kolor = kolor;
        this.czyAI = czyAI;
        this.zloto = 50;
    }

    public void zbierzZasobyZMiast() {
        for (Miasto m : miasta) {
            zloto += m.getProdukcjaZlota();
            nauka += m.getProdukcjaNauki();
            m.przeliczPopulacje();
        }
    }

    public int getPopulacjaCaLkowita() {
        int suma = 0;
        for (Miasto m : miasta) suma += m.getPopulacja();
        return suma;
    }

    public int getPopulacjaZagospodarowana() {
        int suma = 0;
        for (Miasto m : miasta) suma += m.getPopulacjaZajeta();
        return suma;
    }

    public int getWolnaPopulacja() { return getPopulacjaCaLkowita() - getPopulacjaZagospodarowana(); }

    public int getBilansPozywienia() {
        double suma = 0;
        for (Miasto m : miasta) suma += m.getBilansPozywienia();
        return (int) Math.round(suma);
    }

    public int getKosztBudowyMiasta()        { return KOSZT_BUDOWY_MIASTA; }

    public String getNazwa()                 { return nazwa; }
    public Color getKolor()                  { return kolor; }
    public boolean isCzyAI()                 { return czyAI; }
    public int getZloto()                    { return zloto; }
    public int getNauka()                    { return nauka; }
    public double getMnoznikZlota()          { return mnoznikZlota; }

    public void dodajZloto(int n)   { zloto += n; }
    public void odejmijZloto(int n) { zloto -= n; }
    public void dodajMnoznikZlota(double delta) { mnoznikZlota += delta; }

    public List<Miasto> getMiasta()         { return miasta; }
    public List<JednostkaNaMapie> getJednostki()   { return jednostki; }

    public void dodajMiasto(Miasto m)       { miasta.add(m); }
    public void usunMiasto(Miasto m)        { miasta.remove(m); }
    public void dodajJednostke(JednostkaNaMapie j) { jednostki.add(j); }
    public void usunJednostke(JednostkaNaMapie j)  { jednostki.remove(j); }

    // Drzewko rozwoju

    public boolean czyMaPerk(Perk perk)        { return zakupionePerki.contains(perk); }
    public Set<Perk> getZakupionePerki()       { return zakupionePerki; }

    public boolean kupPerk(Perk perk) {
        if (czyMaPerk(perk)) return false;
        if (perk.perkNadrzedny != null && !czyMaPerk(perk.perkNadrzedny)) return false;
        if (nauka < perk.koszt) return false;
        nauka -= perk.koszt;
        zakupionePerki.add(perk);
        perk.zastosuj(this);
        return true;
    }
}