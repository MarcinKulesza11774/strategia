import java.util.ArrayList;
import java.util.List;

/**
 * Miasto – logiczny kontener grupujący budynki należące do jednego osiedla.
 * Nie ma fizycznej reprezentacji na mapie; zamiast tego każdy budynek
 * (w tym Town Hall) to osobny BudynekWMiescie na konkretnym polu.
 */
public class Miasto {
    private final String nazwa;
    private final List<Pole> pola = new ArrayList<>(); // pola na których stoją budynki tego miasta

    public static final int KOSZT_JEDNOSTKI = 20;

    public Miasto(String nazwa) {
        this.nazwa = nazwa;
    }

    public void dodajPole(Pole pole) {
        pola.add(pole);
    }

    public List<Pole> getPola() { return pola; }

    /** Suma złota ze wszystkich budynków miasta. */
    public int getProdukcjaZlota() {
        int suma = 0;
        for (Pole p : pola)
            if (p.getBudynek() != null) suma += p.getBudynek().getBudynek().generowaneZloto;
        return suma;
    }

    /** Suma nauki ze wszystkich budynków miasta. */
    public int getProdukcjaNauki() {
        int suma = 0;
        for (Pole p : pola)
            if (p.getBudynek() != null) suma += p.getBudynek().getBudynek().generowanaNauka;
        return suma;
    }

    /** Suma pożywienia ze wszystkich budynków miasta. */
    public int getProdukcjaPozywienia() {
        int suma = 0;
        for (Pole p : pola)
            if (p.getBudynek() != null) suma += p.getBudynek().getBudynek().generowanePozywienie;
        return suma;
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
