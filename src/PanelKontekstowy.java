import javax.swing.*;
import java.awt.*;

/**
 * Abstrakcyjna klasa bazowa dla paneli kontekstowych (miasto / jednostka).
 * Zawiera wspólne metody pomocnicze do budowania UI w BoxLayout.
 * Podklasy implementują metodę odswiez(Gracz) wypełniając panel treścią.
 */
public abstract class PanelKontekstowy extends JPanel {
    protected final SilnikGry silnik;
    protected final OknoGry oknoGry;

    protected PanelKontekstowy(SilnikGry silnik, OknoGry oknoGry) {
        this.silnik = silnik;
        this.oknoGry = oknoGry;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    /** Odświeża zawartość panelu do aktualnego stanu gry. */
    public abstract void odswiez(Gracz gracz);

    // -------------------------------------------------------------------------
    // Wspólne metody pomocnicze UI
    // -------------------------------------------------------------------------

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
}
