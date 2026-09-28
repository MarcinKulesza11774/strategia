import java.util.ArrayList;
import java.util.List;

public class Miasto {
    private final String nazwa;
    private final List<Pole> pola = new ArrayList<>();

    private int populacja = Ekonomia.POPULACJA_STARTOWA;
    private int populacjaZajeta; // zajęta przez budynki i jednostki wyszkolone w tym mieście

    public static final int KOSZT_JEDNOSTKI = 20;

    public Miasto(String nazwa) {
        this.nazwa = nazwa;
    }

    public void dodajPole(Pole pole) { pola.add(pole); }
    public List<Pole> getPola()      { return pola; }

    public int getPopulacja()        { return populacja; }
    public int getPopulacjaZajeta()  { return populacjaZajeta; }
    public int getWolnaPopulacja()   { return populacja - populacjaZajeta; }

    public boolean moznaWydacPopulacje(int ilosc) { return getWolnaPopulacja() >= ilosc; }
    public void wydajPopulacje(int ilosc)         { populacjaZajeta += ilosc; }

    public double getMnoznikZlota() {
        double mnoznik = 1.0;
        for (Pole p : pola)
            if (p.getBudynek() != null) mnoznik += p.getBudynek().getBudynek().bonusDoMnoznikaZlota;
        Gracz wlasciciel = getWlasciciel();
        return wlasciciel != null ? mnoznik * wlasciciel.getMnoznikZlota() : mnoznik;
    }

    public double getMnoznikNauki() {
        double mnoznik = 1.0;
        for (Pole p : pola)
            if (p.getBudynek() != null) mnoznik += p.getBudynek().getBudynek().bonusDoMnoznikaNauki;
        return mnoznik;
    }

    public int getProdukcjaZlota() {
        return (int) Math.round(populacja * Ekonomia.ZLOTO_NA_MIESZKANCA * getMnoznikZlota());
    }

    public int getProdukcjaNauki() {
        return (int) Math.round(populacja * Ekonomia.NAUKA_NA_MIESZKANCA * getMnoznikNauki());
    }

    public int getProdukcjaPozywienia() {
        int suma = 0;
        for (Pole p : pola)
            if (p.getBudynek() != null) suma += p.getBudynek().getBudynek().generowanePozywienie;
        return suma;
    }

    public double getZuzycieJedzenia() { return populacja * Ekonomia.ZUZYCIE_JEDZENIA_NA_MIESZKANCA; }

    public double getBilansPozywienia() { return getProdukcjaPozywienia() - getZuzycieJedzenia(); }

    public void przeliczPopulacje() {
        int zmiana = (int) Math.round(getBilansPozywienia() * Ekonomia.PRZYROST_POPULACJI_NA_BILANS);
        populacja = Math.max(Ekonomia.MIN_POPULACJA, populacja + zmiana);
    }

    public boolean czyMaTownHall() {
        for (Pole p : pola)
            if (p.getBudynek() != null && p.getBudynek().getBudynek() == Budynek.TOWNHALL)
                return true;
        return false;
    }

    public Gracz getWlasciciel() {
        for (Pole p : pola)
            if (p.getBudynek() != null) return p.getBudynek().getWlasciciel();
        return null;
    }

    public String getNazwa() { return nazwa; }

    @Override
    public String toString() { return nazwa; }
}