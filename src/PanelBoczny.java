import javax.swing.*;
import java.awt.*;

/**
 * Panel boczny z informacjami o stanie gry i kontekstowymi przyciskami.
 */
public class PanelBoczny extends JPanel {
    private final SilnikGry silnik;
    private final OknoGry oknoGry;

    private final JLabel etykietaTury        = new JLabel();
    private final JLabel etykietaZlotoGracza = new JLabel();
    private final JLabel etykietaMiastaGracza= new JLabel();
    private final JLabel etykietaJednostki   = new JLabel();
    private final JLabel etykietaKomunikat   = new JLabel();

    // Sekcja wrogów – dynamicznie odbudowywana przy odswiez()
    private final JPanel panelWrogow = new JPanel();
    private final JPanel panelAkcji  = new JPanel();

    private final JButton btnSzkolJednostke = new JButton("Szkol jednostkę (20 zł)");
    private final JButton btnBudujMiasto    = new JButton("Buduj miasto");
    private final JButton btnUlepszAtak     = new JButton("+ Atak (30 zł)");
    private final JButton btnUlepszZycie    = new JButton("+ HP (30 zł)");
    private final JButton btnUlepszRuch     = new JButton("+ Ruch (30 zł)");
    private final JButton btnZakonczTure    = new JButton("Zakończ turę →");

    public PanelBoczny(SilnikGry silnik, OknoGry oknoGry) {
        this.silnik = silnik;
        this.oknoGry = oknoGry;

        setPreferredSize(new Dimension(210, 0));
        setBackground(new Color(40, 40, 50));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        dodajEtykiete(etykietaTury, Font.BOLD, 13);
        add(Box.createVerticalStrut(6));
        add(separator());

        add(naglowek("GRACZ", new Color(80, 140, 220)));
        dodajEtykiete(etykietaZlotoGracza, Font.PLAIN, 12);
        dodajEtykiete(etykietaMiastaGracza, Font.PLAIN, 12);
        dodajEtykiete(etykietaJednostki, Font.PLAIN, 12);
        add(Box.createVerticalStrut(4));
        add(separator());

        // Panel wrogów odbudowywany dynamicznie
        panelWrogow.setOpaque(false);
        panelWrogow.setLayout(new BoxLayout(panelWrogow, BoxLayout.Y_AXIS));
        add(panelWrogow);
        add(separator());

        add(naglowek("TEREN", new Color(150, 210, 150)));
        for (Teren t : Teren.values()) add(legendaTerenu(t));
        add(Box.createVerticalStrut(4));
        add(separator());

        etykietaKomunikat.setForeground(new Color(240, 210, 100));
        etykietaKomunikat.setFont(new Font("SansSerif", Font.ITALIC, 11));
        etykietaKomunikat.setAlignmentX(LEFT_ALIGNMENT);
        etykietaKomunikat.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        add(etykietaKomunikat);
        add(separator());

        panelAkcji.setOpaque(false);
        panelAkcji.setLayout(new BoxLayout(panelAkcji, BoxLayout.Y_AXIS));
        add(panelAkcji);

        add(Box.createVerticalGlue());

        styl(btnZakonczTure, new Color(50, 130, 50));
        btnZakonczTure.addActionListener(e -> { silnik.zakonczTure(); oknoGry.odswiez(); });
        add(btnZakonczTure);

        btnSzkolJednostke.addActionListener(e -> { silnik.szkolJednostkeWZaznaczonymMiescie(); oknoGry.odswiez(); });
        btnBudujMiasto.addActionListener(e ->    { silnik.budujMiastoZaznaczonegoGracza();     oknoGry.odswiez(); });
        btnUlepszAtak.addActionListener(e ->     { silnik.ulepszAtakZaznaczonegoGracza();      oknoGry.odswiez(); });
        btnUlepszZycie.addActionListener(e ->    { silnik.ulepszZycieZaznaczonegoGracza();     oknoGry.odswiez(); });
        btnUlepszRuch.addActionListener(e ->     { silnik.ulepszRuchZaznaczonegoGracza();      oknoGry.odswiez(); });

        odswiez();
    }

    public void odswiez() {
        Gracz gracz = silnik.getGraczLudzki();

        etykietaTury.setText("Tura " + silnik.getNumerTury() + " / " + silnik.getLimitTur());
        etykietaZlotoGracza.setText("Złoto: " + gracz.getZloto()
            + "  (miasto: " + gracz.getKosztBudowyMiasta() + " zł)");
        etykietaMiastaGracza.setText("Miasta: " + gracz.getMiasta().size());
        etykietaJednostki.setText("Jednostki: " + gracz.getJednostki().size());
        etykietaKomunikat.setText(
            "<html><body style='width:185px'>" + silnik.getKomunikat() + "</body></html>");
        btnZakonczTure.setEnabled(silnik.isTuraNalezyDoGracza());

        // Odbuduj sekcję wrogów
        panelWrogow.removeAll();
        for (Gracz ai : silnik.getGraczeAI()) {
            JLabel nagl = naglowek(ai.getNazwa().toUpperCase(), ai.getKolor());
            panelWrogow.add(nagl);
            panelWrogow.add(info("Złoto: " + ai.getZloto()
                + "  Miasta: " + ai.getMiasta().size()
                + "  Jedn: " + ai.getJednostki().size()));
        }
        panelWrogow.revalidate();
        panelWrogow.repaint();

        odswiezPanelAkcji(gracz);
    }

    private void odswiezPanelAkcji(Gracz gracz) {
        panelAkcji.removeAll();
        if (silnik.getZaznaczoneMiasto() != null) {
            Miasto m = silnik.getZaznaczoneMiasto();
            panelAkcji.add(naglowek("MIASTO: " + m.getNazwa() + " poz." + m.getPoziom(),
                new Color(200, 180, 80)));
            panelAkcji.add(info("+złoto: " + m.getProdukcjaZlota() + " / turę"));
            panelAkcji.add(Box.createVerticalStrut(4));
            dodajPrzycisk(btnSzkolJednostke, gracz.getZloto() >= Miasto.KOSZT_JEDNOSTKI);
        } else if (silnik.getZaznaczonaJednostka() != null) {
            Jednostka j = silnik.getZaznaczonaJednostka();
            panelAkcji.add(naglowek("JEDNOSTKA", new Color(200, 180, 80)));
            panelAkcji.add(info("HP: " + j.getPunktyZycia() + "/" + j.getMaksymalnePunktyZycia()));
            panelAkcji.add(info("ATK: " + j.getObrazenia()
                + "  RUCH: " + j.getPozostalyruch() + "/" + j.getMaksymalnyRuch()));
            btnBudujMiasto.setText("Buduj miasto (" + gracz.getKosztBudowyMiasta() + " zł)");
            dodajPrzycisk(btnBudujMiasto, gracz.getZloto() >= gracz.getKosztBudowyMiasta());
            if (silnik.czyZaznaczonaJednostkaWMiescie()) {
                panelAkcji.add(Box.createVerticalStrut(4));
                panelAkcji.add(info("Ulepszenia (" + j.getPoziomAtaku()
                    + "/" + j.getPoziomZycia() + "/" + j.getPoziomRuchu() + "):"));
                dodajPrzycisk(btnUlepszAtak,  j.moznaUlepszycAtak()  && gracz.getZloto() >= Jednostka.KOSZT_ULEPSZENIA);
                dodajPrzycisk(btnUlepszZycie, j.moznaUlepszycZycie() && gracz.getZloto() >= Jednostka.KOSZT_ULEPSZENIA);
                dodajPrzycisk(btnUlepszRuch,  j.moznaUlepszycRuch()  && gracz.getZloto() >= Jednostka.KOSZT_ULEPSZENIA);
            }
        }
        panelAkcji.revalidate();
        panelAkcji.repaint();
    }
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

    private JLabel info(String tekst) {
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

    private void styl(JButton btn, Color kolor) {
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(200, 26));
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setBackground(kolor);
    }

    private void dodajPrzycisk(JButton btn, boolean aktywny) {
        styl(btn, new Color(60, 90, 140));
        btn.setEnabled(aktywny);
        panelAkcji.add(Box.createVerticalStrut(2));
        panelAkcji.add(btn);
    }

    private JPanel legendaTerenu(Teren teren) {
        JPanel wiersz = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
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
