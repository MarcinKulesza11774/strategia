import javax.swing.*;
import java.awt.*;

public class PanelSzczegolyPerka extends PanelKontekstowy {
    private final JButton btnKup = new JButton();

    public PanelSzczegolyPerka(SilnikGry silnik, OknoGry oknoGry) {
        super(silnik, oknoGry);
        btnKup.addActionListener(e -> { silnik.kupZaznaczonyPerk(); oknoGry.odswiez(); });
    }

    @Override
    public void odswiez(Gracz gracz) {
        removeAll();
        Perk perk = silnik.getZaznaczonyPerk();
        if (perk == null) return;

        add(naglowek("PERK: " + perk.nazwa, new Color(180, 160, 230)));
        add(info("Generacja " + perk.generacja));
        add(Box.createVerticalStrut(4));
        add(info("<html><body style='width:190px'>" + perk.opis + "</body></html>"));
        add(Box.createVerticalStrut(6));

        boolean kupiony     = gracz.czyMaPerk(perk);
        boolean odblokowany = perk.perkNadrzedny == null || gracz.czyMaPerk(perk.perkNadrzedny);

        if (kupiony) {
            add(info("Już kupiony"));
        } else if (!odblokowany) {
            add(info("Wymaga: " + perk.perkNadrzedny.nazwa));
        } else {
            btnKup.setText( perk.koszt + " pkt nauki)");
            dodajPrzycisk(btnKup, gracz.getNauka() >= perk.koszt);
        }

        revalidate();
        repaint();
    }
}