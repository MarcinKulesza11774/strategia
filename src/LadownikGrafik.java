import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LadownikGrafik {
    private static final String KATALOG = "grafiki";

    private final Map<Teren, Image> grafikaTerenu = new HashMap<>();
    private Image grafikaMiasta;
    private Image grafikaJednostki;

    public LadownikGrafik() {
        for (Teren t : Teren.values()) {
            Image img = wczytaj(KATALOG + "/teren/" + t.name() + ".png");
            if (img != null) grafikaTerenu.put(t, img);
        }
        grafikaMiasta   = wczytaj(KATALOG + "/miasto.png");
        grafikaJednostki = wczytaj(KATALOG + "/jednostka.png");
    }

    private Image wczytaj(String sciezka) {
        try {
            File plik = new File(sciezka);
            if (!plik.exists()) return null;
            return ImageIO.read(plik);
        } catch (IOException e) {
            return null;
        }
    }

    public boolean maGrafikeTerenu(Teren teren) {
        return grafikaTerenu.containsKey(teren);
    }

    public Image getGrafikaTerenu(Teren teren) { return grafikaTerenu.get(teren); }
    public Image getGrafikaMiasta()             { return grafikaMiasta; }
    public Image getGrafikaJednostki()          { return grafikaJednostki; }
}
