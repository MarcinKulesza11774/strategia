// PanelMiasta.java
import javax.swing.*;
import java.awt.*;

/**
 * Panel kontekstowy wyświetlany gdy zaznaczone jest miasto gracza.
 * Widok główny pokazuje informacje o mieście oraz przyciski "Buduj" i "Szkól",
 * które zamieniają cały widok na odpowiedni panel z pełną listą opcji.
 */
public class PanelMiasta extends PanelKontekstowy {

    private enum Widok { GLOWNY, BUDOWANIE, SZKOLENIE }
    private Widok widok = Widok.GLOWNY;
    private Miasto ostatnieMiasto;

    private final JButton btnBuduj  = new JButton("Buduj");
    private final JButton btnSzkol  = new JButton("Szkól jednostki");
    private final JButton btnWstecz = new JButton("← Wstecz");

    private final PanelBudowania panelBudowania;
    private final PanelSzkolenia panelSzkolenia;

    public PanelMiasta(SilnikGry silnik, OknoGry oknoGry) {
        super(silnik, oknoGry);
        panelBudowania = new PanelBudowania(silnik, oknoGry);
        panelSzkolenia = new PanelSzkolenia(silnik, oknoGry);

        btnBuduj.addActionListener(e  -> { widok = Widok.BUDOWANIE; oknoGry.odswiez(); });
        btnSzkol.addActionListener(e  -> { widok = Widok.SZKOLENIE; oknoGry.odswiez(); });
        btnWstecz.addActionListener(e -> { widok = Widok.GLOWNY;    oknoGry.odswiez(); });
    }

    @Override
    public void odswiez(Gracz gracz) {
        removeAll();
        Miasto m = silnik.getZaznaczoneMiasto();
        if (m == null) { widok = Widok.GLOWNY; return; }
        if (m != ostatnieMiasto) { widok = Widok.GLOWNY; ostatnieMiasto = m; }

        switch (widok) {
            case BUDOWANIE -> pokazBudowanie(gracz, m);
            case SZKOLENIE -> pokazSzkolenie(gracz, m);
            default        -> pokazGlowny(gracz, m);
        }

        revalidate();
        repaint();
    }

    // -------------------------------------------------------------------------

    private void pokazGlowny(Gracz gracz, Miasto m) {
        add(naglowek("MIASTO: " + m.getNazwa(), new Color(200, 180, 80)));
        add(info("Złoto: +" + m.getProdukcjaZlota() + "  Nauka: +" + m.getProdukcjaNauki()
                + "  Żyw: +" + m.getProdukcjaPozywienia()));
        add(info("Budynki: " + m.getPola().size()));

        for (Pole p : m.getPola()) {
            if (p.getBudynek() != null)
                add(info("  • " + p.getBudynek().getBudynek().nazwa));
        }

        add(Box.createVerticalStrut(4));
        add(info("Wolna pop.: " + gracz.getWolnaPopulacja() + "/" + gracz.getPopulacjaCaLkowita()));
        add(Box.createVerticalStrut(2));

        dodajPrzycisk(btnBuduj, true);
        dodajPrzycisk(btnSzkol, true);
    }

    private void pokazBudowanie(Gracz gracz, Miasto m) {
        add(naglowek("BUDOWANIE – " + m.getNazwa(), new Color(150, 210, 150)));
        dodajPrzycisk(btnWstecz, true);
        add(Box.createVerticalStrut(4));

        panelBudowania.odswiez(gracz);
        add(panelBudowania);

        if (silnik.czyTrybBudowania()) {
            add(Box.createVerticalStrut(4));
            add(naglowek("Kliknij podświetlone pole na mapie", new Color(255, 220, 80)));
        }
    }

    private void pokazSzkolenie(Gracz gracz, Miasto m) {
        add(naglowek("SZKOLENIE – " + m.getNazwa(), new Color(150, 210, 150)));
        dodajPrzycisk(btnWstecz, true);
        add(Box.createVerticalStrut(4));

        panelSzkolenia.odswiez(gracz);
        add(panelSzkolenia);
    }
}