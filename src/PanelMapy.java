import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

/**
 * Panel rysujący mapę. Implementuje Odswiezalny.
 * Rysuje podświetlenie dostępnych pól podczas trybu budowania budynku.
 */
public class PanelMapy extends JPanel implements Odswiezalny {
    private static final int TILE = 40;

    private final SilnikGry silnik;
    private final OknoGry oknoGry;
    private final LadownikGrafik grafiki;

    private double zoom = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private Point dragStart;
    private double offsetXPrzedDragiem, offsetYPrzedDragiem;

    public PanelMapy(SilnikGry silnik, OknoGry oknoGry) {
        this.silnik = silnik;
        this.oknoGry = oknoGry;
        this.grafiki = new LadownikGrafik();
        setPreferredSize(new Dimension(900, 650));
        setBackground(Color.BLACK);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
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
            offsetX = e.getX() - (e.getX() - offsetX) * (newZoom / zoom);
            offsetY = e.getY() - (e.getY() - offsetY) * (newZoom / zoom);
            zoom = newZoom;
            repaint();
        });
    }

    @Override
    public void odswiez() {
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform originalTransform = g2.getTransform();
        g2.translate(offsetX, offsetY);
        g2.scale(zoom, zoom);

        Mapa mapa = silnik.getMapa();
        Pole zaznaczonePole = silnik.getZaznaczonePole();

        // 1. Tło terenu
        for (int row = 0; row < mapa.getLiczbaWierszy(); row++) {
            for (int col = 0; col < mapa.getLiczbaKolumn(); col++) {
                Pole pole = mapa.getPole(row, col);
                int x = col * TILE, y = row * TILE;
                if (grafiki.maGrafikeTerenu(pole.getTeren())) {
                    g2.drawImage(grafiki.getGrafikaTerenu(pole.getTeren()), x, y, TILE, TILE, null);
                } else {
                    g2.setColor(pole.getTeren().kolor);
                    g2.fillRect(x, y, TILE, TILE);
                }
            }
        }

        // 2. Siatka
        g2.setColor(new Color(0, 0, 0, 40));
        g2.setStroke(new BasicStroke(1f));
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

        // 3. Budynki, jednostki, zaznaczenie
        for (int row = 0; row < mapa.getLiczbaWierszy(); row++) {
            for (int col = 0; col < mapa.getLiczbaKolumn(); col++) {
                Pole pole = mapa.getPole(row, col);
                int x = col * TILE, y = row * TILE;
                if (pole.getBudynek() != null)   rysujBudynek(g2, pole.getBudynek(), x, y);
                if (pole.getJednostka() != null) rysujJednostke(g2, pole.getJednostka(), x, y);
                if (pole == zaznaczonePole)      rysujZaznaczenie(g2, x, y);
            }
        }

        // 4. Podświetlenie pól dostępnych pod budynek
        for (Pole p : silnik.getPodswietlonePola()) {
            int x = p.getCol() * TILE, y = p.getRow() * TILE;
            g2.setColor(new Color(80, 255, 80, 90));
            g2.fillRect(x, y, TILE, TILE);
            g2.setColor(new Color(80, 255, 80, 220));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(x + 1, y + 1, TILE - 2, TILE - 2);
            g2.setStroke(new BasicStroke(1f));
        }

        // 5. Obwódki regionów
        rysujObwodkiRegionow(g2, mapa);

        g2.setTransform(originalTransform);
    }

    private void rysujObwodkiRegionow(Graphics2D g2, Mapa mapa) {
        Color[] kolorRegionow = new Color[mapa.getLiczbaRegionow()];
        for (int row = 0; row < mapa.getLiczbaWierszy(); row++) {
            for (int col = 0; col < mapa.getLiczbaKolumn(); col++) {
                Pole p = mapa.getPole(row, col);
                if (p.getBudynek() != null)
                    kolorRegionow[p.getNumerRegionu()] = p.getBudynek().getWlasciciel().getKolor();
            }
        }

        g2.setStroke(new BasicStroke(3f));
        for (int row = 0; row < mapa.getLiczbaWierszy(); row++) {
            for (int col = 0; col < mapa.getLiczbaKolumn(); col++) {
                Pole pole = mapa.getPole(row, col);
                int reg = pole.getNumerRegionu();
                int x = col * TILE, y = row * TILE;

                Pole gore = mapa.getPole(row - 1, col);
                if (gore == null || gore.getNumerRegionu() != reg) {
                    Color kolor = (gore != null && kolorRegionow[gore.getNumerRegionu()] != null)
                        ? kolorRegionow[gore.getNumerRegionu()] : kolorRegionow[reg];
                    g2.setColor(kolor != null ? kolor : new Color(100, 100, 100));
                    g2.drawLine(x, y, x + TILE, y);
                }

                Pole lewo = mapa.getPole(row, col - 1);
                if (lewo == null || lewo.getNumerRegionu() != reg) {
                    Color kolor = (lewo != null && kolorRegionow[lewo.getNumerRegionu()] != null)
                        ? kolorRegionow[lewo.getNumerRegionu()] : kolorRegionow[reg];
                    g2.setColor(kolor != null ? kolor : new Color(100, 100, 100));
                    g2.drawLine(x, y, x, y + TILE);
                }
            }
        }
        int mapW = mapa.getLiczbaKolumn() * TILE, mapH = mapa.getLiczbaWierszy() * TILE;
        g2.setColor(new Color(100, 100, 100));
        g2.drawLine(mapW, 0, mapW, mapH);
        g2.drawLine(0, mapH, mapW, mapH);
        g2.setStroke(new BasicStroke(1f));
    }

    private void rysujBudynek(Graphics2D g2, BudynekWMiescie bwm, int x, int y) {
        if (grafiki.getGrafikaMiasta() != null) {
            g2.drawImage(grafiki.getGrafikaMiasta(), x, y, TILE, TILE, null);
            g2.setColor(bwm.getWlasciciel().getKolor());
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(x + 1, y + 1, TILE - 2, TILE - 2);
            g2.setStroke(new BasicStroke(1f));
        } else {
            Color kolor = bwm.getWlasciciel().getKolor();
            g2.setColor(kolor);
            g2.fillRect(x + 6, y + 6, 28, 28);
            g2.setColor(kolor.darker());
            g2.drawRect(x + 6, y + 6, 28, 28);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.BOLD, 9));
            // Skrót nazwy budynku
            String skrot = bwm.getBudynek() == Budynek.TOWNHALL ? "TH"
                : bwm.getBudynek().nazwa.substring(0, Math.min(2, bwm.getBudynek().nazwa.length())).toUpperCase();
            g2.drawString(skrot, x + 10, y + 25);
        }
    }

    private void rysujJednostke(Graphics2D g2, JednostkaNaMapie jednostka, int x, int y) {
        if (grafiki.getGrafikaJednostki() != null) {
            g2.drawImage(grafiki.getGrafikaJednostki(), x + 5, y + 5, TILE - 10, TILE - 15, null);
            g2.setColor(jednostka.getWlasciciel().getKolor());
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(x + 5, y + 5, TILE - 10, TILE - 15);
            g2.setStroke(new BasicStroke(1f));
        } else {
            Color kolor = jednostka.getWlasciciel().getKolor();
            g2.setColor(kolor);
            g2.fillOval(x + 10, y + 8, 20, 20);
            g2.setColor(kolor.darker());
            g2.drawOval(x + 10, y + 8, 20, 20);
        }
        int barWidth = TILE - 6;
        int filled = (int) ((double) jednostka.getPunktyZycia()
                / jednostka.getMaksymalnePunktyZycia() * barWidth);
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
