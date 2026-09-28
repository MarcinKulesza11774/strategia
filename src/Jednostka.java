import java.awt.*;

public enum Jednostka implements Kosztowny {BIEDNAPIERDOLONAPIECHOTA("biedna pierdolona piechota", 50, 25, 1, 4, 100, 20, TypJednostki.PIECHOTA),
    WLUCZNICY("włucznicy", 50, 25, 1, 4, 100, 20, TypJednostki.PIECHOTA),
    LUCZNICY("łucznicy", 30, 40, 6, 4, 100, 30, TypJednostki.LUCZNICY),
    KONNI("konni", 30, 40, 1, 10, 80, 100, TypJednostki.KAWALERIA);

    public final String nazwa;
    public final int hp;
    public final int damage;
    public final int zasiegAtaku;
    public final int zasiegRuchu;
    public final int kosztWPopulacji;
    public final int kosztWZlocie;
    private final TypJednostki typ;

    Jednostka(String nazwa, int hp, int damage, int zasiegAtaku, int zasiegRuchu,
              int kosztWPopulacji, int kosztWZlocie, TypJednostki typ) {
        this.nazwa = nazwa;
        this.hp = hp;
        this.damage = damage;
        this.zasiegAtaku = zasiegAtaku;
        this.zasiegRuchu = zasiegRuchu;
        this.kosztWPopulacji = kosztWPopulacji;
        this.kosztWZlocie = kosztWZlocie;
        this.typ = typ;
    }

    public TypJednostki getTyp() { return typ; }

    @Override public String getNazwa()        { return nazwa; }
    @Override public int getKosztWZlocie()    { return kosztWZlocie; }
    @Override public int getKosztWPopulacji() { return kosztWPopulacji; }

    @Override
    public String toString() { return nazwa; }
}