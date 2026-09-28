import java.util.function.Consumer;

public enum Perk {
    EKONOMIA("Podstawy ekonomii", null, 1, 50,
            "ZŁOTO + 15%",
            gracz -> gracz.dodajMnoznikZlota(0.10)),

    EKONOMIA2("EKONOMIA II", EKONOMIA, 2, 80,
            "ZŁOTO + 15%",
            gracz -> gracz.dodajMnoznikZlota(0.15)),

    EKONOMIA3("Ekonomia III", EKONOMIA, 2, 150,
            "ZŁOTO + 15%",
            gracz -> gracz.dodajMnoznikZlota(0.15)),

    EKONOMIA4("Ekonomia IV", null, 2, 80,
            "ZŁOTO + 20%",
            gracz -> gracz.dodajMnoznikZlota(0.20));

    public final String nazwa;
    public final Perk perkNadrzedny;
    public final int generacja;
    public final int koszt;
    public final String opis;
    private final Consumer<Gracz> funkcja;

    Perk(String nazwa, Perk perkNadrzedny, int generacja, int koszt, String opis, Consumer<Gracz> funkcja) {
        this.nazwa = nazwa;
        this.perkNadrzedny = perkNadrzedny;
        this.generacja = generacja;
        this.koszt = koszt;
        this.opis = opis;
        this.funkcja = funkcja;
    }

    public void zastosuj(Gracz gracz) { funkcja.accept(gracz); }

    @Override
    public String toString() { return nazwa; }
}