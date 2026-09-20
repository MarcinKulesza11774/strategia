import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Główne okno gry. Trzyma listę Odswiezalny i woła odswiez() na wszystkich.
 */
public class OknoGry extends JFrame {
    private SilnikGry silnik;
    private final List<Odswiezalny> komponenty = new ArrayList<>();

    public OknoGry() {
        setTitle("Gra Strategiczna");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setJMenuBar(stworzMenu());
        pokazMenuStartowe();
    }

    private void pokazMenuStartowe() {
        UstawieniaGry ust = DialogUstawien.pokaz(this);
        if (ust == null) System.exit(0);
        zaladujGre(ust);
    }

    private void zaladujGre(UstawieniaGry ust) {
        getContentPane().removeAll();
        komponenty.clear();

        silnik = new SilnikGry(ust);

        PanelMapy panelMapy   = new PanelMapy(silnik, this);
        PanelBoczny panelBoczny = new PanelBoczny(silnik, this);

        komponenty.add(panelMapy);
        komponenty.add(panelBoczny);

        add(panelMapy,   BorderLayout.CENTER);
        add(panelBoczny, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        revalidate();
        repaint();
    }

    /** Odświeża wszystkie komponenty przez interfejs Odswiezalny. */
    public void odswiez() {
        for (Odswiezalny k : komponenty) k.odswiez();
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
        menuGra.add(itemNowaGra);
        menuGra.addSeparator();
        menuGra.add(itemWyjdz);
        pasek.add(menuGra);
        return pasek;
    }
}
