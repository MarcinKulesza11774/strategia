import javax.swing.*;
import java.awt.*;

public class PanelBudowania extends PanelKontekstowy {
    private final JButton[] btnBudynki;

    public PanelBudowania(SilnikGry silnik, OknoGry oknoGry) {
        super(silnik, oknoGry);
        btnBudynki = stworzPrzyciskiKosztowne(Budynek.dostepneDoBudowy(), budynek -> {
            Miasto miasto = silnik.getZaznaczoneMiasto();
            if (miasto != null) {
                silnik.rozpocznijBudowanie(budynek, miasto);
                oknoGry.odswiez();
            }
        });
    }

    @Override
    public void odswiez(Gracz gracz) {
        removeAll();
        Miasto m = silnik.getZaznaczoneMiasto();
        if (m == null) return;

        Budynek[] wartosci = Budynek.dostepneDoBudowy();
        for (int i = 0; i < wartosci.length; i++) {
            boolean mozna = stacNa(gracz, m, wartosci[i]) && !silnik.czyTrybBudowania();
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