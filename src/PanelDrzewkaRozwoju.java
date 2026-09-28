import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;

public class PanelDrzewkaRozwoju extends JPanel implements Odswiezalny {
    private final Canvas canvas;

    public PanelDrzewkaRozwoju(SilnikGry silnik, OknoGry oknoGry) {
        setLayout(new BorderLayout());
        setBackground(new Color(25, 25, 32));

        JButton btnWstecz = new JButton("Powrót do mapy");
        btnWstecz.setFocusPainted(false);
        btnWstecz.setBackground(new Color(60, 90, 140));
        btnWstecz.setForeground(Color.WHITE);
        btnWstecz.addActionListener(e -> oknoGry.przelaczDrzewko());

        JPanel gorny = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        gorny.setOpaque(false);
        gorny.add(btnWstecz);
        add(gorny, BorderLayout.NORTH);

        canvas = new Canvas(silnik, oknoGry);
        add(canvas, BorderLayout.CENTER);
    }

    @Override
    public void odswiez() { canvas.repaint(); }

    // -------------------------------------------------------------------------

    private static class Canvas extends JPanel {
        private static final int SZEROKOSC_WEZLA = 150;
        private static final int WYSOKOSC_WEZLA  = 50;
        private static final int ODSTEP_X = 40;
        private static final int ODSTEP_Y = 90;

        private final SilnikGry silnik;
        private final OknoGry oknoGry;

        private double zoom = 1.0;
        private double offsetX = 40;
        private double offsetY = 40;
        private Point dragStart;
        private double offsetXPrzedDragiem, offsetYPrzedDragiem;

        private final Map<Perk, Rectangle> hitboxy = new HashMap<>();

        Canvas(SilnikGry silnik, OknoGry oknoGry) {
            this.silnik = silnik;
            this.oknoGry = oknoGry;
            setBackground(new Color(25, 25, 32));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() != MouseEvent.BUTTON1) return;
                    Point logiczny = doWspolrzednychLogicznych(e.getPoint());
                    for (Map.Entry<Perk, Rectangle> wpis : hitboxy.entrySet()) {
                        if (wpis.getValue().contains(logiczny)) {
                            silnik.zaznaczPerk(wpis.getKey());
                            oknoGry.odswiez();
                            return;
                        }
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
                double newZoom = Math.max(0.3, Math.min(2.5, zoom * factor));
                offsetX = e.getX() - (e.getX() - offsetX) * (newZoom / zoom);
                offsetY = e.getY() - (e.getY() - offsetY) * (newZoom / zoom);
                zoom = newZoom;
                repaint();
            });
        }

        private Point doWspolrzednychLogicznych(Point ekran) {
            int x = (int) ((ekran.x - offsetX) / zoom);
            int y = (int) ((ekran.y - offsetY) / zoom);
            return new Point(x, y);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            AffineTransform oryginalna = g2.getTransform();
            g2.translate(offsetX, offsetY);
            g2.scale(zoom, zoom);

            hitboxy.clear();
            Map<Integer, List<Perk>> generacje = new TreeMap<>();
            for (Perk p : Perk.values())
                generacje.computeIfAbsent(p.generacja, k -> new ArrayList<>()).add(p);

            Map<Perk, Point> pozycje = new HashMap<>();
            for (Map.Entry<Integer, List<Perk>> wiersz : generacje.entrySet()) {
                int generacja = wiersz.getKey();
                List<Perk> perki = wiersz.getValue();
                int y = (generacja - 1) * (WYSOKOSC_WEZLA + ODSTEP_Y);
                for (int i = 0; i < perki.size(); i++) {
                    int x = i * (SZEROKOSC_WEZLA + ODSTEP_X);
                    pozycje.put(perki.get(i), new Point(x, y));
                }
            }

            Gracz gracz = silnik.getGraczLudzki();

            g2.setStroke(new BasicStroke(2f));
            for (Perk p : Perk.values()) {
                if (p.perkNadrzedny == null) continue;
                Point dziecko = pozycje.get(p);
                Point rodzic  = pozycje.get(p.perkNadrzedny);
                int x1 = rodzic.x + SZEROKOSC_WEZLA / 2, y1 = rodzic.y + WYSOKOSC_WEZLA;
                int x2 = dziecko.x + SZEROKOSC_WEZLA / 2, y2 = dziecko.y;
                g2.setColor(gracz.czyMaPerk(p.perkNadrzedny) ? new Color(140, 200, 140) : new Color(90, 90, 100));
                g2.drawLine(x1, y1, x2, y2);
            }

            for (Perk p : Perk.values()) {
                Point pos = pozycje.get(p);
                Rectangle prostokat = new Rectangle(pos.x, pos.y, SZEROKOSC_WEZLA, WYSOKOSC_WEZLA);
                hitboxy.put(p, prostokat);
                rysujWezel(g2, p, prostokat, gracz);
            }

            g2.setTransform(oryginalna);
        }

        private void rysujWezel(Graphics2D g2, Perk p, Rectangle r, Gracz gracz) {
            boolean kupiony     = gracz.czyMaPerk(p);
            boolean odblokowany = p.perkNadrzedny == null || gracz.czyMaPerk(p.perkNadrzedny);
            boolean staca       = gracz.getNauka() >= p.koszt;

            Color tlo;
            if (kupiony)             tlo = new Color(60, 140, 70);
            else if (!odblokowany)   tlo = new Color(60, 60, 68);
            else if (staca)          tlo = new Color(60, 90, 150);
            else                     tlo = new Color(120, 90, 40);

            g2.setColor(tlo);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);

            boolean zaznaczony = p == silnik.getZaznaczonyPerk();
            g2.setColor(zaznaczony ? Color.YELLOW : new Color(20, 20, 25));
            g2.setStroke(new BasicStroke(zaznaczony ? 3f : 1.5f));
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString(p.nazwa, r.x + 10, r.y + 20);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.drawString(kupiony ? "Kupiony" : p.koszt + " pkt nauki", r.x + 10, r.y + 38);
        }

        @Override
        public Dimension getPreferredSize() { return new Dimension(900, 650); }
    }
}