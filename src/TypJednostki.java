import java.util.HashMap;
import java.util.Map;

public enum TypJednostki {
    PIECHOTA("piechota",
            Map.of(
            "kawaleria", 1.25f,
            "lucznicy", 0.75f)
    ),
    LUCZNICY("lucznicy",
            Map.of(
            "piechota", 1.25f,
            "kawaleria", 0.75f)
    ),
    KAWALERIA("kawaleria",
            Map.of(
            "piechota", 0.75f,
            "lucznicy", 1.25f),
            Map.of(
                    Teren.ROWNINA, 2f,
                    Teren.GORY, 0.25f)
    ),
    OKRET("okret",
            Map.of(
            "piechota", 2f,
            "lucznicy", 2f),
            Map.of(
                    Teren.ROWNINA, 0f,
                    Teren.GORY, 0f,
                    Teren.PUSTYNIA, 0f,
                    Teren.SNIEG, 0f,
                    Teren.WODA, 200f)
    );

    TypJednostki(String nazwa, Map<String, Float> zaleznosciOdInnychJednostek, Map<Teren, Float> zaleznosciOdTerenu){

    }

    TypJednostki(String nazwa, Map<String, Float> zaleznosciOdInnychJednostek){

    }
}
