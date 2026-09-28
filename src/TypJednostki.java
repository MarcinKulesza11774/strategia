import java.util.Map;

public enum TypJednostki {
    PIECHOTA("piechota",
            Map.of("kawaleria", 1.25f, "lucznicy", 0.75f)),
    LUCZNICY("lucznicy",
            Map.of("piechota", 1.25f, "kawaleria", 0.75f)),
    KAWALERIA("kawaleria",
            Map.of("piechota", 0.75f, "lucznicy", 1.25f),
            Map.of(Teren.ROWNINA, 2f, Teren.GORY, 0.25f)),
    OKRET("okret",
            Map.of("piechota", 2f, "lucznicy", 2f),
            Map.of(Teren.ROWNINA, 0f, Teren.GORY, 0f, Teren.PUSTYNIA, 0f,
                    Teren.SNIEG, 0f, Teren.WODA, 200f));

    private final String nazwa;
    private final Map<String, Float> mnoznikiPrzeciwnikow;
    private final Map<Teren, Float> mnoznikiTerenu;

    TypJednostki(String nazwa, Map<String, Float> mnoznikiPrzeciwnikow, Map<Teren, Float> mnoznikiTerenu) {
        this.nazwa = nazwa;
        this.mnoznikiPrzeciwnikow = mnoznikiPrzeciwnikow;
        this.mnoznikiTerenu = mnoznikiTerenu;
    }

    TypJednostki(String nazwa, Map<String, Float> mnoznikiPrzeciwnikow) {
        this(nazwa, mnoznikiPrzeciwnikow, Map.of());
    }

     public float mnoznikPrzeciwko(TypJednostki inny) {
        return mnoznikiPrzeciwnikow.getOrDefault(inny.nazwa, 1f);
    }

     public float mnoznikTerenu(Teren teren) {
        return mnoznikiTerenu.getOrDefault(teren, 1f);
    }

    @Override
    public String toString() { return nazwa; }
}