import java.awt.*;

public enum Jednostka {
    WLUCZNICY         ("włucznicy", 50,   25,  1,  4, 100,    20),
    LUCZNICY         ("łucznicy", 30,   40,  12,  4, 100,    30),
    KONNI         ("KONNI", 30,   40,  1,  10, 80,    100);

    public final String nazwa;
    public final int hp;
    public final int damage;
    public final int zasiegAtaku;
    public final int zasiegRuchu;
    public final int kosztWPopulacji;
    public final int kosztWZlocie;

    Jednostka(String nazwa, int hp, int damage, int zasiegAtaku, int zasiegRuchu, int kosztWPopulacji, int kosztWZlocie) {
        this.nazwa = nazwa;
        this.hp = hp;
        this.damage = damage;
        this.zasiegAtaku = zasiegAtaku;
        this.zasiegRuchu = zasiegRuchu;
        this.kosztWPopulacji = kosztWPopulacji;
        this.kosztWZlocie = kosztWZlocie;
    }
}
