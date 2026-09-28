import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OknoGry extends JFrame {
    private SilnikGry silnik;
    private final List<Odswiezalny> komponenty = new ArrayList<>();

    private JSplitPane split;
    private PanelMapy panelMapy;
    private PanelDrzewkaRozwoju panelDrzewka;

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

        PanelGorny  panelGorny  = new PanelGorny(silnik, this);
        panelMapy    = new PanelMapy(silnik, this);
        panelDrzewka = new PanelDrzewkaRozwoju(silnik, this);
        PanelBoczny panelBoczny = new PanelBoczny(silnik, this);

        JScrollPane scrollBoczny = new JScrollPane(panelBoczny);
        scrollBoczny.setBorder(null);
        scrollBoczny.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollBoczny.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollBoczny.getVerticalScrollBar().setUnitIncrement(16);
        scrollBoczny.getViewport().setBackground(panelBoczny.getBackground());

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelMapy, scrollBoczny);
        split.setResizeWeight(1.0);
        split.setDividerSize(6);
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(true);
        split.setDividerLocation(panelMapy.getPreferredSize().width);

        komponenty.add(panelGorny);
        komponenty.add(panelMapy);
        komponenty.add(panelDrzewka);
        komponenty.add(panelBoczny);

        add(panelGorny, BorderLayout.NORTH);
        add(split,      BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        revalidate();
        repaint();
    }

    public void przelaczDrzewko() {
        if (silnik.czyDrzewkoOtwarte()) {
            silnik.zamknijDrzewkoRozwoju();
            split.setLeftComponent(panelMapy);
        } else {
            silnik.otworzDrzewkoRozwoju();
            split.setLeftComponent(panelDrzewka);
        }
        odswiez();
    }

    public void odswiez() {
        for (Odswiezalny k : komponenty) k.odswiez();
        if (silnik.getStanGry() != SilnikGry.StanGry.TRWA) pokazKoniecGry();
    }

    private void pokazKoniecGry() {
        String tekst = switch (silnik.getStanGry()) {
            case WYGRANA_GRACZA -> "Zwycięstwo";
            case PRZEGRANA      -> "Przegrana";
            case REMIS          -> "Remis";
            default             -> "";
        };
        int wybor = JOptionPane.showOptionDialog(this, tekst + "\n\nNowa gra",
                "Koniec gry", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, new String[]{"Nowa gra", "Wyjdź"}, "Nowa gra");
        if (wybor == 0) pokazMenuStartowe();
        else System.exit(0);
    }

    private JMenuBar stworzMenu() {
        JMenuBar pasek = new JMenuBar();
        JMenu menuGra  = new JMenu("Gra");
        JMenuItem itemNowaGra = new JMenuItem("Nowa gra");
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