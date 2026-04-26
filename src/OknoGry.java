import javax.swing.*;
import java.awt.*;

/**
 * Główne okno gry (JFrame).
 */
public class OknoGry extends JFrame {
    private final SilnikGry silnik;
    private final PanelMapy panelMapy;
    private final PanelBoczny panelBoczny;

    public OknoGry() {
        silnik = new SilnikGry();

        setTitle("Gra Strategiczna");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setJMenuBar(stworzMenu());

        panelMapy   = new PanelMapy(silnik, this);
        panelBoczny = new PanelBoczny(silnik, this);

        add(panelMapy,   BorderLayout.CENTER);
        add(panelBoczny, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
    }

    public void odswiez() {
        panelMapy.repaint();
        panelBoczny.odswiez();

        if (silnik.getStanGry() != SilnikGry.StanGry.TRWA) {
            pokazKoniecGry();
        }
    }

    private void pokazKoniecGry() {
        String tekst = switch (silnik.getStanGry()) {
            case WYGRANA_GRACZA -> "Zwycięstwo! Pokonałeś przeciwnika.";
            case WYGRANA_AI     -> "Przegrana. AI zdobyła przewagę.";
            case REMIS          -> "Remis po " + SilnikGry.LIMIT_TUR + " turach.";
            default             -> "";
        };
        int wybor = JOptionPane.showOptionDialog(this, tekst + "\n\nNowa gra?",
            "Koniec gry", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
            null, new String[]{"Nowa gra", "Wyjdź"}, "Nowa gra");
        if (wybor == 0) {
            dispose();
            SwingUtilities.invokeLater(() -> new OknoGry().setVisible(true));
        } else {
            System.exit(0);
        }
    }

    private JMenuBar stworzMenu() {
        JMenuBar pasek = new JMenuBar();
        JMenu menuGra  = new JMenu("Gra");

        JMenuItem itemNowaGra = new JMenuItem("Nowa gra");
        itemNowaGra.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new OknoGry().setVisible(true));
        });
        JMenuItem itemWyjdz = new JMenuItem("Wyjdź");
        itemWyjdz.addActionListener(e -> System.exit(0));

        JMenuItem itemPomoc = new JMenuItem("Jak grać?");
        itemPomoc.addActionListener(e -> JOptionPane.showMessageDialog(this, INSTRUKCJA, "Pomoc", JOptionPane.PLAIN_MESSAGE));

        menuGra.add(itemNowaGra);
        menuGra.addSeparator();
        menuGra.add(itemWyjdz);
        menuGra.add(itemPomoc);
        pasek.add(menuGra);
        return pasek;
    }

    private static final String INSTRUKCJA = """
        STEROWANIE
        • Lewy przycisk myszy – zaznacz / przesuń / atakuj
        • Scroll – zoom in/out
        • Środkowy przycisk + drag – przesuwanie mapy

        AKCJE
        • Kliknij swoją jednostkę → zaznacz
        • Kliknij sąsiednie pole → przesuń
        • Kliknij wrogą jednostkę obok → atakuj
        • Kliknij swoje miasto → zaznacz (panel: szkol jednostkę)
        • Zaznaczona jednostka → panel: buduj miasto
        • Jednostka w mieście → panel: ulepszenia

        BUDOWA MIAST
        Każde następne miasto kosztuje więcej.
        W każdym regionie mapy można postawić tylko jedno miasto.
        Region pokazuje obwódkę w kolorze właściciela.

        ULEPSZENIA JEDNOSTKI
        Jednostka musi stać w twoim mieście.
        Koszty: 30 zł za ulepszenie (max poziom 3).

        WARUNKI ZWYCIĘSTWA
        Zniszcz wszystkie jednostki i miasta wroga,
        lub miej więcej miast po """ + SilnikGry.LIMIT_TUR + " turach.";
}
