import javax.swing.*;
import java.awt.*;

/**
 * Panel kontekstowy wyświetlany gdy zaznaczone jest miasto gracza.
 * Pokazuje produkcję budynków i przyciski budowania nowych.
 */
public class PanelMiasta extends PanelKontekstowy {
    private final JButton btnSzkolJednostke = new JButton("Szkól jednostki");
    private final JButton btnBuduj = new JButton("Buduj");
//    private final PanelBudowania panelBudowania = new PanelBudowania(silnik, oknoGry);

    // Przyciski budynków – jeden na każdy typ (pomijamy TOWNHALL, bo buduje się przez jednostkę)
    private final Budynek[] dostepneBudynki = {
        Budynek.POLE_UPRAWNE,
        Budynek.UNIWERSYTET,
        Budynek.DZIELNICA_KUPIECKA
    };
    private final JButton[] btnBudynki = new JButton[dostepneBudynki.length];
    private PanelBudowania panelBudowania;

    public PanelMiasta(SilnikGry silnik, OknoGry oknoGry) {
        super(silnik, oknoGry);
        btnSzkolJednostke.addActionListener(e -> { silnik.szkolJednostkeWZaznaczonymMiescie(); oknoGry.odswiez(); });
        panelBudowania = new PanelBudowania(silnik, oknoGry);

//        for (int i = 0; i < dostepneBudynki.length; i++) {
//            Budynek b = dostepneBudynki[i];
//            JButton btn = new JButton(b.nazwa + " (" + b.kosztWZlocie + " zł / " + b.kosztWPopulacji + " pop.)");
//            btn.addActionListener(e -> {
//                Miasto miasto = silnik.getZaznaczoneMiasto();
//                if (miasto != null) {
//                    silnik.rozpocznijBudowanie(b, miasto);
//                    oknoGry.odswiez();
//                }
//            });
//            btnBudynki[i] = btn;
//        }
    }

    @Override
    public void odswiez(Gracz gracz) {
        removeAll();
        Miasto m = silnik.getZaznaczoneMiasto();
        if (m == null) return;

        add(naglowek("MIASTO: " + m.getNazwa(), new Color(200, 180, 80)));
        add(info("Złoto: +" + m.getProdukcjaZlota() + "  Nauka: +" + m.getProdukcjaNauki()
            + "  Żyw: +" + m.getProdukcjaPozywienia()));
        add(info("Budynki: " + m.getPola().size()));

        // Lista istniejących budynków
        for (Pole p : m.getPola()) {
            if (p.getBudynek() != null)
                add(info("  • " + p.getBudynek().getBudynek().nazwa));
        }

        add(Box.createVerticalStrut(4));
        dodajPrzycisk(btnSzkolJednostke, gracz.getZloto() >= Miasto.KOSZT_JEDNOSTKI);

        add(Box.createVerticalStrut(4));
        add(naglowek("BUDUJ", new Color(150, 210, 150)));
        add(info("Wolna pop.: " + gracz.getWolnaPopulacja() + "/" + gracz.getPopulacjaCaLkowita()));

//        for (int i = 0; i < dostepneBudynki.length; i++) {
//            Budynek b = dostepneBudynki[i];
//            boolean mozna = gracz.getZloto() >= b.kosztWZlocie
//                         && gracz.moznaWydacPopulacje(b.kosztWPopulacji)
//                         && !silnik.czyTrybBudowania();
//            dodajPrzycisk(btnBudynki[i], mozna);
//        }

        panelBudowania.odswiez(gracz);

        add(panelBudowania);

        if (silnik.czyTrybBudowania()) {
            add(Box.createVerticalStrut(4));
            add(naglowek("Kliknij podświetlone pole na mapie", new Color(255, 220, 80)));
        }

        revalidate();
        repaint();
    }
}
