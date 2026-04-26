import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

/**
 * Panel rysujący mapę gry.
 * Zoom: scroll myszki. Pan: środkowy przycisk + drag.
 */
public class PanelMapy extends JPanel {
    private static final int TILE = 40; // rozmiar pola w pikselach (skala 1:1)

    private final SilnikGry silnik;
    private final OknoGry oknoGry;

    private double zoom = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    private Point dragStart;
    private double offsetXPrzedDragiem;
    private double offsetYPrzedDragiem;

    public PanelMapy(SilnikGry silnik, OknoGry oknoGry) {
        this.silnik = silnik;
        this.oknoGry = oknoGry;
        setPreferredSize(new Dimension(900, 650));
        setBackground(Color.BLACK);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Odwrócenie transformacji: (ekran - offset) / zoom = pozycja w świecie
                    int col = (int) ((e.getX() - offsetX) / (TILE * zoom));
                    int row = (int) ((e.getY() - offsetY) / (TILE * zoom));
                    silnik.kliknijPole(row, col);
                    oknoGry.odswiez();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    dragStart = e.getPoint();
                    offsetXPrzedDragiem = offsetX;
                    offsetYPrzedDragiem = offsetY;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) dragStart = null;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    offsetX = offsetXPrzedDragiem + (e.getX() - dragStart.x);
                    offsetY = offsetYPrzedDragiem + (e.getY() - dragStart.y);
                    repaint();
                }
            }
        });

        addMouseWheelListener(e -> {
            double factor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
            double newZoom = Math.max(0.3, Math.min(4.0, zoom * factor));
            // Zoom wyśrodkowany na kursorze
            offsetX = e.getX() - (e.getX() - offsetX) * (newZoom / zoom);
            offsetY = e.getY() - (e.getY() - offsetY) * (newZoom / zoom);
            zoom = newZoom;
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Zapisz oryginalną transformację, zastosuj zoom+offset, rysuj, przywróć
        AffineTransform originalTransform = g2.getTransform();
        g2.translate(offsetX, offsetY);
        g2.scale(zoom, zoom);

        Mapa mapa = silnik.getMapa();
        Pole zaznaczonePole = silnik.getZaznaczonePole();

        // Pętla 1: tło terenu
        for (int row = 0; row < mapa.getLiczbaWierszy(); row++) {
            for (int col = 0; col < mapa.getLiczbaKolumn(); col++) {
                Pole pole = mapa.getPole(row, col);
                int x = col * TILE, y = row * TILE;
                g2.setColor(pole.getTeren().kolor);
                g2.fillRect(x, y, TILE, TILE);
            }
        }

        // Pętla 2: siatka – rysujemy tylko górną i lewą krawędź każdego pola.
        // drawRect nakładałby prawą/dolną krawędź na obwódki regionów sąsiednich pól.
        g2.setColor(new Color(0, 0, 0, 40));
        for (int row = 0; row < mapa.getLiczbaWierszy(); row++) {
            for (int col = 0; col < mapa.getLiczbaKolumn(); col++) {
                int x = col * TILE, y = row * TILE;
                g2.drawLine(x, y, x + TILE, y);
                g2.drawLine(x, y, x, y + TILE);
            }
        }
        int mapW = mapa.getLiczbaKolumn() * TILE, mapH = mapa.getLiczbaWierszy() * TILE;
        g2.drawLine(mapW, 0, mapW, mapH);
        g2.drawLine(0, mapH, mapW, mapH);

        // Pętla 3: miasta, jednostki, zaznaczenie
        for (int row = 0; row < mapa.getLiczbaWierszy(); row++) {
            for (int col = 0; col < mapa.getLiczbaKolumn(); col++) {
                Pole pole = mapa.getPole(row, col);
                int x = col * TILE, y = row * TILE;
                if (pole.getMiasto() != null)    rysujMiasto(g2, pole.getMiasto(), x, y);
                if (pole.getJednostka() != null) rysujJednostke(g2, pole.getJednostka(), x, y);
                if (pole == zaznaczonePole)      rysujZaznaczenie(g2, x, y);
            }
        }

        // Pętla 4: obwódki regionów rysowane na samej górze
        rysujObwodkiRegionow(g2, mapa);

        g2.setTransform(originalTransform);
    }

    /**
     * Obwódki regionów: gruba kolorowa linia wzdłuż każdej krawędzi
     * gdzie sąsiednie pole należy do innego regionu.
     * Kolor = kolor właściciela miasta w tym regionie, szary jeśli brak właściciela.
     */
    private void rysujObwodkiRegionow(Graphics2D g2, Mapa mapa) {
        // Przygotuj kolory regionów (null = brak miasta)
        Color[] kolorRegionow = new Color[mapa.getLiczbaRegionow()];
        for (int row = 0; row < mapa.getLiczbaWierszy(); row++) {
            for (int col = 0; col < mapa.getLiczbaKolumn(); col++) {
                Pole p = mapa.getPole(row, col);
                if (p.getMiasto() != null) {
                    kolorRegionow[p.getNumerRegionu()] = p.getMiasto().getWlasciciel().getKolor();
                }
            }
        }

        g2.setStroke(new BasicStroke(3f));

        for (int row = 0; row < mapa.getLiczbaWierszy(); row++) {
            for (int col = 0; col < mapa.getLiczbaKolumn(); col++) {
                Pole pole = mapa.getPole(row, col);
                int reg = pole.getNumerRegionu();
                Color kolor = kolorRegionow[reg];
                if (kolor == null) kolor = new Color(180, 180, 180); // neutralny szary

                int x = col * TILE, y = row * TILE;

                // Górna krawędź
                Pole sasiad = mapa.getPole(row - 1, col);
                if (sasiad == null || sasiad.getNumerRegionu() != reg) {
                    g2.setColor(kolor);
                    g2.drawLine(x, y, x + TILE, y);
                }
                // Dolna krawędź
                sasiad = mapa.getPole(row + 1, col);
                if (sasiad == null || sasiad.getNumerRegionu() != reg) {
                    g2.setColor(kolor);
                    g2.drawLine(x, y + TILE, x + TILE, y + TILE);
                }
                // Lewa krawędź
                sasiad = mapa.getPole(row, col - 1);
                if (sasiad == null || sasiad.getNumerRegionu() != reg) {
                    g2.setColor(kolor);
                    g2.drawLine(x, y, x, y + TILE);
                }
                // Prawa krawędź
                sasiad = mapa.getPole(row, col + 1);
                if (sasiad == null || sasiad.getNumerRegionu() != reg) {
                    g2.setColor(kolor);
                    g2.drawLine(x + TILE, y, x + TILE, y + TILE);
                }
            }
        }

        g2.setStroke(new BasicStroke(1f));
    }

    /** Miasto: kolorowy kwadrat z literą M i poziomem. */
    private void rysujMiasto(Graphics2D g2, Miasto miasto, int x, int y) {
        Color kolor = miasto.getWlasciciel().getKolor();
        g2.setColor(kolor);
        g2.fillRect(x + 8, y + 8, 24, 24);
        g2.setColor(kolor.darker());
        g2.drawRect(x + 8, y + 8, 24, 24);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        g2.drawString("M" + miasto.getPoziom(), x + 10, y + 25);
    }

    /** Jednostka: kolorowe kółko z paskiem życia. */
    private void rysujJednostke(Graphics2D g2, Jednostka jednostka, int x, int y) {
        Color kolor = jednostka.getWlasciciel().getKolor();
        g2.setColor(kolor);
        g2.fillOval(x + 10, y + 8, 20, 20);
        g2.setColor(kolor.darker());
        g2.drawOval(x + 10, y + 8, 20, 20);

        // Pasek życia
        int barWidth = TILE - 6;
        int filled = (int) ((double) jednostka.getPunktyZycia() / jednostka.getMaksymalnePunktyZycia() * barWidth);
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(x + 3, y + 31, barWidth, 3);
        g2.setColor(jednostka.getPunktyZycia() > 50 ? new Color(60, 200, 60) : new Color(220, 60, 60));
        g2.fillRect(x + 3, y + 31, filled, 3);
    }

    private void rysujZaznaczenie(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(255, 255, 0, 100));
        g2.fillRect(x, y, TILE, TILE);
        g2.setColor(Color.YELLOW);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRect(x + 1, y + 1, TILE - 2, TILE - 2);
        g2.setStroke(new BasicStroke(1f));
    }
}
