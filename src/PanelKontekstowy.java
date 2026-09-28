import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public abstract class PanelKontekstowy extends JPanel {
    protected final SilnikGry silnik;
    protected final OknoGry oknoGry;

    protected PanelKontekstowy(SilnikGry silnik, OknoGry oknoGry) {
        this.silnik = silnik;
        this.oknoGry = oknoGry;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public abstract void odswiez(Gracz gracz);

    protected JLabel naglowek(String tekst, Color kolor) {
        JLabel l = new JLabel(tekst);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(kolor);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(3, 0, 1, 0));
        return l;
    }

    protected JLabel info(String tekst) {
        JLabel l = new JLabel(tekst);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(Color.LIGHT_GRAY);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    protected void dodajPrzycisk(JButton btn, boolean aktywny) {
        btn.setEnabled(aktywny);
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(200, 26));
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(60, 90, 140));
        add(Box.createVerticalStrut(2));
        add(btn);
    }

    /**
     * Po jednym przycisku na każdą wartość enuma implementującego Kosztowny
     * (np. Jednostka.values(), Budynek.dostepneDoBudowy()) – dodanie nowej
     * stałej do enuma automatycznie daje nowy przycisk, bez zmian tutaj.
     */
    protected <T extends Kosztowny> JButton[] stworzPrzyciskiKosztowne(T[] wartosci, Consumer<T> akcja) {
        JButton[] przyciski = new JButton[wartosci.length];
        for (int i = 0; i < wartosci.length; i++) {
            T wartosc = wartosci[i];
            JButton btn = new JButton(wartosc.getNazwa() + " (" + wartosc.getKosztWZlocie()
                    + " zł / " + wartosc.getKosztWPopulacji() + " pop.)");
            btn.addActionListener(e -> akcja.accept(wartosc));
            przyciski[i] = btn;
        }
        return przyciski;
    }

    protected boolean stacNa(Gracz gracz, Miasto miasto, Kosztowny k) {
        return gracz.getZloto() >= k.getKosztWZlocie() && miasto.moznaWydacPopulacje(k.getKosztWPopulacji());
    }
}