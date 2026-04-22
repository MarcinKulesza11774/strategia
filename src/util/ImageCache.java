package util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Przechowuje załadowane obrazki w pamięci podręcznej.
 *
 * Obrazki powinny być w folderze images/ obok folderu data/.
 * Obsługiwane formaty: PNG, JPG.
 * Jeśli obrazek nie istnieje, metoda getObrazek() zwraca null –
 * renderer wtedy rysuje zwykły kolor kafelka/jednostki.
 *
 * Użycie:
 *   ImageCache.get().getObrazek("tiles/trawa.png", 16)
 *   – zwraca obrazek przeskalowany do 16x16 px
 */
public class ImageCache {

    private static ImageCache instancja;

    /** Klucz w mapie: ścieżka + "@" + rozmiar, np. "tiles/trawa.png@16" */
    private final Map<String, BufferedImage> pamiec = new HashMap<>();

    private ImageCache() {}

    public static ImageCache get() {
        if (instancja == null) instancja = new ImageCache();
        return instancja;
    }

    /**
     * Zwraca obrazek przeskalowany do podanego rozmiaru (rozmiar x rozmiar px).
     *
     * @param sciezka  relatywna ścieżka obrazka, np. "tiles/trawa.png"
     * @param rozmiar  docelowy rozmiar w pikselach (szerokość = wysokość)
     * @return         przeskalowany BufferedImage, lub null jeśli nie znaleziono
     */
    public BufferedImage getObrazek(String sciezka, int rozmiar) {
        if (sciezka == null || sciezka.isEmpty()) return null;

        String klucz = sciezka + "@" + rozmiar;
        if (pamiec.containsKey(klucz)) return pamiec.get(klucz);

        BufferedImage oryginal = zaladuj(sciezka);
        if (oryginal == null) {
            pamiec.put(klucz, null); // zapamiętaj brak, żeby nie szukać ponownie
            return null;
        }

        BufferedImage przeskalowany = skaluj(oryginal, rozmiar);
        pamiec.put(klucz, przeskalowany);
        return przeskalowany;
    }

    /**
     * Ładuje surowy obrazek z dysku.
     * Szuka najpierw w ./images/<sciezka>, potem na classpath /images/<sciezka>.
     */
    private BufferedImage zaladuj(String sciezka) {
        try {
            File plik = new File("images/" + sciezka);
            if (plik.exists()) return ImageIO.read(plik);

            InputStream strumien = getClass().getResourceAsStream("/images/" + sciezka);
            if (strumien != null) return ImageIO.read(strumien);
        } catch (Exception e) {
            System.err.println("Nie udało się załadować obrazka: " + sciezka + " (" + e.getMessage() + ")");
        }
        return null;
    }

    /**
     * Skaluje obrazek do rozmiaru x rozmiar px, zachowując jakość.
     */
    private BufferedImage skaluj(BufferedImage zrodlo, int rozmiar) {
        BufferedImage cel = new BufferedImage(rozmiar, rozmiar, BufferedImage.TYPE_INT_ARGB);
        Graphics2D grafika = cel.createGraphics();
        grafika.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                  RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        grafika.drawImage(zrodlo, 0, 0, rozmiar, rozmiar, null);
        grafika.dispose();
        return cel;
    }

    /** Czyści pamięć podręczną (np. po zmianie rozmiaru kafelka przy zoomie). */
    public void wyczysc() {
        pamiec.clear();
    }
}
