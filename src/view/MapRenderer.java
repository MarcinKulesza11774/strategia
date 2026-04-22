package view;

import model.GameState;
import model.building.Room;
import model.entity.Unit;
import model.world.Tile;
import model.world.TileType;
import model.world.WorldMap;
import util.ImageCache;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Rysuje widoczny wycinek mapy (culling – tylko kafelki w obrębie kamery).
 *
 * Dla każdego kafelka:
 *   1. Jeśli kafelek ma przypisany obrazek w tiles.json → narysuj obrazek
 *   2. Jeśli nie → narysuj kolor z tiles.json
 *   3. Na wierzch nałóż podświetlenia (zaznaczenie do budowy / zbierania)
 *   4. Nałóż kolor pokoju (lekkie tło)
 *   5. Narysuj siatkę
 *
 * Dla każdej jednostki:
 *   1. Jeśli klasa ma obrazek w units.json → narysuj obrazek
 *   2. Jeśli nie → narysuj kółko z literą
 *   3. Pasek HP zawsze
 *
 * Argumenty metody render:
 *   @param grafika       kontekst rysowania Swing
 *   @param stan          stan gry (mapa, jednostki, pokoje)
 *   @param kamera        pozycja i zoom kamery
 *   @param zaznaczona    jednostka zaznaczona przez gracza (lub null)
 *   @param zaznaczX1..Y2 obszar zaznaczenia myszą (w kafelkach)
 *   @param czyPrzeciagamy czy gracz aktualnie przeciąga zaznaczenie
 */
public class MapRenderer {

    public void render(Graphics2D grafika, GameState stan, Camera kamera,
                       Unit zaznaczona,
                       int zaznaczX1, int zaznaczY1, int zaznaczX2, int zaznaczY2,
                       boolean czyPrzeciagamy) {
        WorldMap mapa = stan.getMap();
        int rozmiarKafelka = kamera.getTileSize();

        // Rysuj tylko kafelki widoczne na ekranie
        for (int ty = kamera.getFirstTileY(); ty <= kamera.getLastTileY(); ty++) {
            for (int tx = kamera.getFirstTileX(); tx <= kamera.getLastTileX(); tx++) {
                Tile kafelek = mapa.getTile(tx, ty);
                if (kafelek == null) continue;
                int ekranX = kamera.tileToScreenX(tx);
                int ekranY = kamera.tileToScreenY(ty);
                rysujKafelek(grafika, kafelek, tx, ty, ekranX, ekranY, rozmiarKafelka, stan, kamera);
            }
        }

        // Rysuj jednostki ponad kafelkami.
        // Kopiujemy listę przed iteracją żeby uniknąć ConcurrentModificationException –
        // wątek gry może modyfikować listę jednocześnie gdy EDT ją renderuje.
        for (Unit jednostka : new java.util.ArrayList<>(stan.getUnits())) {
            if (!jednostka.isAlive()) continue;
            if (!kamera.isVisible(jednostka.getTileX(), jednostka.getTileY())) continue;
            int ekranX = kamera.tileToScreenX(jednostka.getX());
            int ekranY = kamera.tileToScreenY(jednostka.getY());
            rysujJednostke(grafika, jednostka, ekranX, ekranY, jednostka == zaznaczona, rozmiarKafelka);
        }

        // Prostokąt przeciągania zaznaczenia
        if (czyPrzeciagamy) {
            int px1 = kamera.tileToScreenX(zaznaczX1);
            int py1 = kamera.tileToScreenY(zaznaczY1);
            int px2 = kamera.tileToScreenX(zaznaczX2 + 1);
            int py2 = kamera.tileToScreenY(zaznaczY2 + 1);
            int rx = Math.min(px1, px2), ry = Math.min(py1, py2);
            int rw = Math.abs(px2 - px1), rh = Math.abs(py2 - py1);
            grafika.setColor(new Color(100, 200, 255, 40));
            grafika.fillRect(rx, ry, rw, rh);
            grafika.setColor(new Color(100, 200, 255, 180));
            grafika.setStroke(new BasicStroke(1.5f));
            grafika.drawRect(rx, ry, rw, rh);
            grafika.setStroke(new BasicStroke(1));
        }
    }

    // -------------------------------------------------------------------------
    // Rysowanie kafelka
    // -------------------------------------------------------------------------

    private void rysujKafelek(Graphics2D grafika, Tile kafelek, int tx, int ty,
                               int ekranX, int ekranY, int rozmiar,
                               GameState stan, Camera kamera) {
        TileType typ = TileType.get(kafelek.getTypeId());

        // Tło: obrazek lub kolor
        BufferedImage obrazek = ImageCache.get().getObrazek(typ.nazwaObrazka, rozmiar);
        if (obrazek != null) {
            grafika.drawImage(obrazek, ekranX, ekranY, null);
        } else {
            grafika.setColor(typ.color);
            grafika.fillRect(ekranX, ekranY, rozmiar, rozmiar);
        }

        // Podświetlenie: oznaczony do zbierania
        if (kafelek.isMarkedForHarvest() && typ.isResource) {
            grafika.setColor(new Color(255, 255, 0, 55));
            grafika.fillRect(ekranX, ekranY, rozmiar, rozmiar);
            // Pasek postępu zbierania
            float postep = (float) kafelek.getHarvestProgress() / Math.max(1, typ.harvestTicks);
            grafika.setColor(new Color(255, 200, 0, 200));
            grafika.fillRect(ekranX, ekranY + rozmiar - 3, (int)(rozmiar * postep), 3);
        }

        // Podświetlenie: oznaczony do budowy (przerywana linia)
        if (kafelek.isMarkedForBuild()) {
            grafika.setColor(new Color(100, 180, 255, 70));
            grafika.fillRect(ekranX, ekranY, rozmiar, rozmiar);
            grafika.setColor(new Color(100, 180, 255, 200));
            grafika.setStroke(new BasicStroke(1f, BasicStroke.CAP_SQUARE,
                    BasicStroke.JOIN_MITER, 1, new float[]{3, 2}, 0));
            grafika.drawRect(ekranX, ekranY, rozmiar - 1, rozmiar - 1);
            grafika.setStroke(new BasicStroke(1));
        }

        // Ikona tekstowa (tylko gdy brak obrazka i kafelek wystarczająco duży)
        if (obrazek == null && rozmiar >= 10) {
            rysujIkoneKafelka(grafika, typ.id, ekranX, ekranY, rozmiar);
        }

        // Tło pokoju – kolorowy odcień
        if (kafelek.getRoomId() >= 0) {
            Room pokoj = stan.getRoomSystem().getRoomById(kafelek.getRoomId());
            if (pokoj != null) {
                Color tint = pokoj.getDef().tintColor;
                grafika.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 30));
                grafika.fillRect(ekranX + 1, ekranY + 1, rozmiar - 2, rozmiar - 2);
            }
        }

        // Siatka
        if (rozmiar >= 8) {
            grafika.setColor(new Color(0, 0, 0, 35));
            grafika.drawRect(ekranX, ekranY, rozmiar, rozmiar);
        }
    }

    private void rysujIkoneKafelka(Graphics2D grafika, String id, int x, int y, int rozmiar) {
        String ikona = switch (id) {
            case TileType.FOREST         -> "T";
            case TileType.MOUNTAIN       -> "^";
            case TileType.WATER          -> "~";
            case TileType.IRON_ORE       -> "Fe";
            case TileType.WOOD_LOG       -> "W";
            case TileType.STONE_ROCK     -> "S";
            case TileType.FOOD_PLANT     -> "F";
            case TileType.WALL           -> "#";
            case TileType.DOOR           -> "D";
            case TileType.GATE           -> "G";
            case TileType.CAMPFIRE       -> "*";
            case TileType.STOCKPILE      -> "[S]";
            case TileType.BED            -> "z";
            case TileType.TABLE          -> "=";
            case TileType.CHAIR          -> "c";
            case TileType.ANVIL          -> "A";
            case TileType.FORGE          -> "FG";
            case TileType.BOOKSHELF      -> "B";
            case TileType.TRAINING_DUMMY -> "TD";
            case TileType.THRONE         -> "TH";
            case TileType.WATCHTOWER     -> "WT";
            default -> "";
        };
        if (ikona.isEmpty()) return;
        int rozmiarCzcionki = Math.max(7, rozmiar / 2);
        grafika.setFont(new Font("Monospaced", Font.BOLD, rozmiarCzcionki));
        FontMetrics fm = grafika.getFontMetrics();
        grafika.setColor(new Color(0, 0, 0, 150));
        grafika.drawString(ikona,
                x + (rozmiar - fm.stringWidth(ikona)) / 2,
                y + (rozmiar + fm.getAscent() - fm.getDescent()) / 2 - 1);
    }

    // -------------------------------------------------------------------------
    // Rysowanie jednostki
    // -------------------------------------------------------------------------

    private void rysujJednostke(Graphics2D grafika, Unit jednostka,
                                 int ekranX, int ekranY, boolean zaznaczona, int rozmiar) {
        int odstep = Math.max(1, rozmiar / 7);
        int rozmiarSylwetki = rozmiar - odstep * 2;
        int ux = ekranX + odstep;
        int uy = ekranY + odstep;

        // Sprawdź czy jest obrazek dla tej klasy
        BufferedImage obrazek = ImageCache.get().getObrazek(
                jednostka.getUnitClass().nazwaObrazka, rozmiarSylwetki);

        if (obrazek != null) {
            // Rysuj obrazek z zaokrąglonym tłem w kolorze gracza
            grafika.setColor(new Color(
                    jednostka.getUnitClass().color.getRed(),
                    jednostka.getUnitClass().color.getGreen(),
                    jednostka.getUnitClass().color.getBlue(), 120));
            grafika.fillOval(ux, uy, rozmiarSylwetki, rozmiarSylwetki);
            // Przytnij do kółka
            Shape staryKlip = grafika.getClip();
            grafika.setClip(new java.awt.geom.Ellipse2D.Float(ux, uy, rozmiarSylwetki, rozmiarSylwetki));
            grafika.drawImage(obrazek, ux, uy, null);
            grafika.setClip(staryKlip);
        } else {
            // Fallback: kółko z literą
            grafika.setColor(jednostka.getUnitClass().color);
            grafika.fillOval(ux, uy, rozmiarSylwetki, rozmiarSylwetki);

            if (rozmiar >= 10) {
                int rozmiarCzcionki = Math.max(7, rozmiar - 6);
                grafika.setFont(new Font("Monospaced", Font.BOLD, rozmiarCzcionki));
                grafika.setColor(Color.WHITE);
                String litera = String.valueOf(jednostka.getUnitClass().label.charAt(0));
                FontMetrics fm = grafika.getFontMetrics();
                grafika.drawString(litera,
                        ux + (rozmiarSylwetki - fm.stringWidth(litera)) / 2,
                        uy + (rozmiarSylwetki + fm.getAscent() - fm.getDescent()) / 2 - 1);
            }
        }

        // Obramowanie (żółte = zaznaczona, białe = gracz, czerwone = wróg)
        grafika.setColor(zaznaczona ? Color.YELLOW
                : jednostka.isPlayerOwned() ? new Color(200, 230, 255) : new Color(255, 180, 180));
        grafika.setStroke(new BasicStroke(zaznaczona ? 2.5f : 1f));
        grafika.drawOval(ux, uy, rozmiarSylwetki, rozmiarSylwetki);
        grafika.setStroke(new BasicStroke(1));

        // Pasek HP
        int szerokoscPaska = rozmiar - 2;
        float stosunekHp = (float) jednostka.getHp() / jednostka.getMaxHp();
        grafika.setColor(new Color(40, 40, 40));
        grafika.fillRect(ekranX + 1, ekranY + rozmiar - 4, szerokoscPaska, 3);
        grafika.setColor(stosunekHp > 0.5f ? new Color(60, 200, 60)
                : stosunekHp > 0.25f ? new Color(220, 180, 0)
                : new Color(220, 50, 50));
        grafika.fillRect(ekranX + 1, ekranY + rozmiar - 4,
                (int)(szerokoscPaska * stosunekHp), 3);
    }
}
