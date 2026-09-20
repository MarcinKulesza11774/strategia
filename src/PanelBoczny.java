import javax.swing.*;
import java.awt.*;

/**
 * Główny panel boczny. Zawiera statystyki gracza i wrogów, legendę terenu,
 * komunikat zdarzeń oraz wymienialny panel kontekstowy (miasto lub jednostka).
 * Implementuje Odswiezalny – OknoGry wola odswiez() na wszystkich komponentach przez interfejs.
 */
public class PanelBoczny extends JPanel implements Odswiezalny {
    private final SilnikGry silnik;
    private final OknoGry oknoGry;

    private final JLabel etykietaTury        = new JLabel();
    private final JLabel etykietaZloto       = new JLabel();
    private final JLabel etykietaPopulacja   = new JLabel();
    private final JLabel etykietaNauka       = new JLabel();
    private final JLabel etykietaPozywienie  = new JLabel();
    private final JLabel etykietaMiasta      = new JLabel();
    private final JLabel etykietaJednostki   = new JLabel();
    private final JLabel etykietaKomunikat   = new JLabel();

    private final JPanel panelWrogow    = new JPanel();
    private final JPanel panelKontenera = new JPanel(); // tu trafia aktywny PanelKontekstowy

    private final PanelMiasta panelMiasta;
    private final PanelJednostki panelJednostki;

    private final JButton btnZakonczTure = new JButton("Zakończ turę →");

    public PanelBoczny(SilnikGry silnik, OknoGry oknoGry) {
        this.silnik = silnik;
        this.oknoGry = oknoGry;
        panelMiasta    = new PanelMiasta(silnik, oknoGry);
        panelJednostki = new PanelJednostki(silnik, oknoGry);

        setPreferredSize(new Dimension(220, 0));
        setBackground(new Color(40, 40, 50));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        // Tura
        dodajEtykiete(etykietaTury, Font.BOLD, 13);
        add(Box.createVerticalStrut(6));
        add(separator());

        // Zasoby gracza
        add(naglowek("GRACZ", new Color(80, 140, 220)));
        dodajEtykiete(etykietaZloto,      Font.PLAIN, 11);
        dodajEtykiete(etykietaPopulacja,  Font.PLAIN, 11);
        dodajEtykiete(etykietaNauka,      Font.PLAIN, 11);
        dodajEtykiete(etykietaPozywienie, Font.PLAIN, 11);
        dodajEtykiete(etykietaMiasta,     Font.PLAIN, 11);
        dodajEtykiete(etykietaJednostki,  Font.PLAIN, 11);
        add(Box.createVerticalStrut(4));
        add(separator());

        // Wrogowie
        panelWrogow.setOpaque(false);
        panelWrogow.setLayout(new BoxLayout(panelWrogow, BoxLayout.Y_AXIS));
        add(panelWrogow);
        add(separator());

        // Legenda terenu
        add(naglowek("TEREN", new Color(150, 210, 150)));
        for (Teren t : Teren.values()) add(legendaTerenu(t));
        add(Box.createVerticalStrut(4));
        add(separator());

        // Komunikat
        etykietaKomunikat.setForeground(new Color(240, 210, 100));
        etykietaKomunikat.setFont(new Font("SansSerif", Font.ITALIC, 11));
        etykietaKomunikat.setAlignmentX(LEFT_ALIGNMENT);
        etykietaKomunikat.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        add(etykietaKomunikat);
        add(separator());

        // Kontener na PanelKontekstowy
        panelKontenera.setOpaque(false);
        panelKontenera.setLayout(new BoxLayout(panelKontenera, BoxLayout.Y_AXIS));
        add(panelKontenera);

        add(Box.createVerticalGlue());

        // Zakończ turę
        btnZakonczTure.setAlignmentX(LEFT_ALIGNMENT);
        btnZakonczTure.setMaximumSize(new Dimension(200, 26));
        btnZakonczTure.setFocusPainted(false);
        btnZakonczTure.setForeground(Color.WHITE);
        btnZakonczTure.setBackground(new Color(50, 130, 50));
        btnZakonczTure.addActionListener(e -> { silnik.zakonczTure(); oknoGry.odswiez(); });
        add(btnZakonczTure);

        odswiez();
    }

    @Override
    public void odswiez() {
        Gracz gracz = silnik.getGraczLudzki();

        etykietaTury.setText("Tura " + silnik.getNumerTury() + " / " + silnik.getLimitTur());
        etykietaZloto.setText("Złoto: " + gracz.getZloto());
        etykietaPopulacja.setText("Populacja: " + gracz.getPopulacjaZagospodarowana()
            + " / " + gracz.getPopulacjaCaLkowita()
            + "  (wolna: " + gracz.getWolnaPopulacja() + ")");
        etykietaNauka.setText("Nauka: " + gracz.getNauka());
        etykietaPozywienie.setText("Pożywienie: " + gracz.getPozywienie());
        etykietaMiasta.setText("Miasta: " + gracz.getMiasta().size());
        etykietaJednostki.setText("Jednostki: " + gracz.getJednostki().size());
        etykietaKomunikat.setText(
            "<html><body style='width:200px'>" + silnik.getKomunikat() + "</body></html>");
        btnZakonczTure.setEnabled(silnik.isTuraNalezyDoGracza());

        // Wrogowie
        panelWrogow.removeAll();
        for (Gracz ai : silnik.getGraczeAI()) {
            JLabel nagl = naglowek(ai.getNazwa().toUpperCase(), ai.getKolor());
            panelWrogow.add(nagl);
            panelWrogow.add(infoLabel("Złoto: " + ai.getZloto()
                + "  Miasta: " + ai.getMiasta().size()
                + "  Jedn: " + ai.getJednostki().size()));
        }
        panelWrogow.revalidate();
        panelWrogow.repaint();

        // Podmień panel kontekstowy
        panelKontenera.removeAll();
        if (silnik.getZaznaczoneMiasto() != null) {
            panelMiasta.odswiez(gracz);
            panelKontenera.add(panelMiasta);
        } else if (silnik.getZaznaczonaJednostka() != null) {
            panelJednostki.odswiez(gracz);
            panelKontenera.add(panelJednostki);
        }
        panelKontenera.revalidate();
        panelKontenera.repaint();
    }

    // -------------------------------------------------------------------------
    // Pomocnicze
    // -------------------------------------------------------------------------

    private void dodajEtykiete(JLabel etykieta, int styl, int rozmiar) {
        etykieta.setForeground(Color.WHITE);
        etykieta.setFont(new Font("SansSerif", styl, rozmiar));
        etykieta.setAlignmentX(LEFT_ALIGNMENT);
        add(etykieta);
    }

    private JLabel naglowek(String tekst, Color kolor) {
        JLabel l = new JLabel(tekst);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(kolor);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(3, 0, 1, 0));
        return l;
    }

    private JLabel infoLabel(String tekst) {
        JLabel l = new JLabel(tekst);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(Color.LIGHT_GRAY);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(80, 80, 100));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        return sep;
    }

    private JPanel legendaTerenu(Teren teren) {
        JPanel wiersz = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 3, 0));
        wiersz.setOpaque(false);
        wiersz.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        JLabel kwadrat = new JLabel("  ");
        kwadrat.setOpaque(true);
        kwadrat.setBackground(teren.kolor);
        kwadrat.setPreferredSize(new Dimension(12, 12));
        wiersz.add(kwadrat);
        String koszt = teren.kosztRuchu >= 99 ? "∞" : String.valueOf(teren.kosztRuchu);
        JLabel nazwa = new JLabel(teren.nazwa + " (" + koszt + ")");
        nazwa.setFont(new Font("SansSerif", Font.PLAIN, 10));
        nazwa.setForeground(Color.LIGHT_GRAY);
        wiersz.add(nazwa);
        return wiersz;
    }
}
