// PanelSzkolenia.java
import javax.swing.*;
import java.awt.*;

public class PanelSzkolenia extends PanelKontekstowy {
    private final Jednostka[] dostepneJednostki = {
            Jednostka.WLUCZNICY,
            Jednostka.LUCZNICY,
            Jednostka.KONNI
    };

    private final JButton[] btnJednostki = new JButton[dostepneJednostki.length];

    public PanelSzkolenia(SilnikGry silnik, OknoGry oknoGry) {
        super(silnik, oknoGry);
        for (int i = 0; i < dostepneJednostki.length; i++) {
            Jednostka jednostka = dostepneJednostki[i];
            JButton btn = new JButton(jednostka.nazwa + " (" + jednostka.kosztWZlocie + " zł / " + jednostka.kosztWPopulacji + " pop.)");
            btn.addActionListener(e -> {
                Miasto miasto = silnik.getZaznaczoneMiasto();
                if (miasto != null) {
                    silnik.szkolJednostkeWZaznaczonymMiescie(jednostka);
                    oknoGry.odswiez();
                }
            });
            btnJednostki[i] = btn;
        }
    }

    @Override
    public void odswiez(Gracz gracz) {
        removeAll();
        Miasto m = silnik.getZaznaczoneMiasto();
        if (m == null) return;

        for (int i = 0; i < dostepneJednostki.length; i++) {
            Jednostka j = dostepneJednostki[i];
            boolean mozna = gracz.getZloto() >= j.kosztWZlocie
                    && gracz.moznaWydacPopulacje(j.kosztWPopulacji);
            dodajPrzycisk(btnJednostki[i], mozna);
        }

        revalidate();
        repaint();
    }
}