import javax.swing.*;

public class PanelSzkolenia extends PanelKontekstowy {
    private final JButton[] btnJednostki;

    public PanelSzkolenia(SilnikGry silnik, OknoGry oknoGry) {
        super(silnik, oknoGry);
        btnJednostki = stworzPrzyciskiKosztowne(Jednostka.values(), jednostka -> {
            if (silnik.getZaznaczoneMiasto() != null) {
                silnik.szkolJednostkeWZaznaczonymMiescie(jednostka);
                oknoGry.odswiez();
            }
        });
    }

    @Override
    public void odswiez(Gracz gracz) {
        removeAll();
        Miasto m = silnik.getZaznaczoneMiasto();
        if (m == null) return;

        Jednostka[] wartosci = Jednostka.values();
        for (int i = 0; i < wartosci.length; i++)
            dodajPrzycisk(btnJednostki[i], stacNa(gracz, m, wartosci[i]));

        revalidate();
        repaint();
    }
}