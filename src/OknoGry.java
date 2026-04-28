import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Główne okno gry. Zawiera menu startowe (dialog z ustawieniami)
 * oraz właściwy widok gry z mapą i panelem bocznym.
 */
public class OknoGry extends JFrame {
    private SilnikGry silnik;
    private PanelMapy panelMapy;
    private PanelBoczny panelBoczny;

    public OknoGry() {
        setTitle("Gra Strategiczna");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setJMenuBar(stworzMenu());
        pokazMenuStartowe();
    }

    /** Wyświetla dialog z ustawieniami i startuje nową grę. */
    private void pokazMenuStartowe() {
        UstawieniaGry ust = DialogUstawien.pokaz(this);
        if (ust == null) System.exit(0); // użytkownik zamknął dialog
        zaladujGre(ust);
    }

    private void zaladujGre(UstawieniaGry ust) {
        // Usuń stare komponenty jeśli istnieją
        getContentPane().removeAll();

        silnik = new SilnikGry(ust);
        panelMapy   = new PanelMapy(silnik, this);
        panelBoczny = new PanelBoczny(silnik, this);

        add(panelMapy,   BorderLayout.CENTER);
        add(panelBoczny, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        revalidate();
        repaint();
    }

    public void odswiez() {
        panelMapy.repaint();
        panelBoczny.odswiez();
        if (silnik.getStanGry() != SilnikGry.StanGry.TRWA) pokazKoniecGry();
    }

    private void pokazKoniecGry() {
        String tekst = switch (silnik.getStanGry()) {
            case WYGRANA_GRACZA -> "Zwycięstwo!";
            case PRZEGRANA      -> "Przegrana.";
            case REMIS          -> "Remis po " + silnik.getLimitTur() + " turach.";
            default             -> "";
        };
        int wybor = JOptionPane.showOptionDialog(this, tekst + "\n\nNowa gra?",
            "Koniec gry", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
            null, new String[]{"Nowa gra", "Wyjdź"}, "Nowa gra");
        if (wybor == 0) pokazMenuStartowe();
        else System.exit(0);
    }

    private JMenuBar stworzMenu() {
        JMenuBar pasek = new JMenuBar();
        JMenu menuGra  = new JMenu("Gra");

        JMenuItem itemNowaGra = new JMenuItem("Nowa gra...");
        itemNowaGra.addActionListener(e -> pokazMenuStartowe());

        JMenuItem itemWyjdz = new JMenuItem("Wyjdź");
        itemWyjdz.addActionListener(e -> System.exit(0));

        JMenuItem itemPomoc = new JMenuItem("Jak grać?");
        itemPomoc.addActionListener(e -> JOptionPane.showMessageDialog(
            this, INSTRUKCJA, "Pomoc", JOptionPane.PLAIN_MESSAGE));

        menuGra.add(itemNowaGra);
        menuGra.addSeparator();
        menuGra.add(itemWyjdz);
        menuGra.add(itemPomoc);
        pasek.add(menuGra);
        return pasek;
    }

    private static final String INSTRUKCJA = """
        STEROWANIE
        • Lewy przycisk – zaznacz / rusz się / atakuj
        • Scroll – zoom
        • Scroll + przesunięcie – przesuwanie mapy
        """;
}
