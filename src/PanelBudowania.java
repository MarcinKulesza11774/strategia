import javax.swing.*;
import java.awt.*;

public class PanelBudowania extends PanelKontekstowy{
    private final Budynek[] dostepneBudynki = {
            Budynek.POLE_UPRAWNE,
            Budynek.UNIWERSYTET,
            Budynek.DZIELNICA_KUPIECKA
    };
    private final JButton[] btnBudynki = new JButton[dostepneBudynki.length];

    public PanelBudowania(SilnikGry silnik, OknoGry oknoGry){
        super(silnik, oknoGry);
        for (int i = 0; i < dostepneBudynki.length; i++) {
            Budynek b = dostepneBudynki[i];
            JButton btn = new JButton(b.nazwa + " (" + b.kosztWZlocie + " zł / " + b.kosztWPopulacji + " pop.)");
            btn.addActionListener(e -> {
                Miasto miasto = silnik.getZaznaczoneMiasto();
                if (miasto != null) {
                    silnik.rozpocznijBudowanie(b, miasto);
                    oknoGry.odswiez();
                }
            });
            btnBudynki[i] = btn;
        }
    }

    @Override
    public void odswiez(Gracz gracz) {
        removeAll();
        Miasto m = silnik.getZaznaczoneMiasto();
        if (m == null) return;

        for (int i = 0; i < dostepneBudynki.length; i++) {
            Budynek b = dostepneBudynki[i];
            boolean mozna = gracz.getZloto() >= b.kosztWZlocie
                    && gracz.moznaWydacPopulacje(b.kosztWPopulacji)
                    && !silnik.czyTrybBudowania();
            dodajPrzycisk(btnBudynki[i], mozna);
        }

        if (silnik.czyTrybBudowania()) {
            add(Box.createVerticalStrut(4));
            add(naglowek("Kliknij podświetlone pole na mapie", new Color(255, 220, 80)));
        }

        revalidate();
        repaint();
    }
}
