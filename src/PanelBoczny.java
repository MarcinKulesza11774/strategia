import javax.swing.*;
import java.awt.*;

public class PanelBoczny extends JPanel implements Odswiezalny {
    private final SilnikGry silnik;
    private final OknoGry oknoGry;

    private final JLabel etykietaKomunikat = new JLabel();
    private final JPanel panelKontenera = new JPanel();

    private final PanelMiasta panelMiasta;
    private final PanelJednostki panelJednostki;

    private final PanelSzczegolyPerka panelSzczegolyPerka;

    private final JButton btnZakonczTure = new JButton("Zakończ turę");

    public PanelBoczny(SilnikGry silnik, OknoGry oknoGry) {
        this.silnik = silnik;
        this.oknoGry = oknoGry;
        panelMiasta    = new PanelMiasta(silnik, oknoGry);
        panelJednostki = new PanelJednostki(silnik, oknoGry);

        setPreferredSize(new Dimension(240, 0));
        setBackground(new Color(40, 40, 50));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        etykietaKomunikat.setForeground(new Color(240, 210, 100));
        etykietaKomunikat.setFont(new Font("SansSerif", Font.ITALIC, 11));
        etykietaKomunikat.setAlignmentX(LEFT_ALIGNMENT);
        etykietaKomunikat.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        add(etykietaKomunikat);
        add(separator());

        panelKontenera.setOpaque(false);
        panelKontenera.setLayout(new BoxLayout(panelKontenera, BoxLayout.Y_AXIS));
        add(panelKontenera);

        panelSzczegolyPerka = new PanelSzczegolyPerka(silnik, oknoGry);

        add(Box.createVerticalGlue());

        btnZakonczTure.setAlignmentX(LEFT_ALIGNMENT);
        btnZakonczTure.setMaximumSize(new Dimension(200, 26));
        btnZakonczTure.setFocusPainted(false);
        btnZakonczTure.setForeground(Color.WHITE);
        btnZakonczTure.setBackground(new Color(50, 130, 50));
        btnZakonczTure.addActionListener(e -> { silnik.zakonczTure(); oknoGry.odswiez(); });
        add(btnZakonczTure);

        odswiez();
    }

    @Override
    public void odswiez() {
        Gracz gracz = silnik.getGraczLudzki();
        etykietaKomunikat.setText(
                "<html><body style='width:200px'>" + silnik.getKomunikat() + "</body></html>");
        btnZakonczTure.setEnabled(silnik.isTuraNalezyDoGracza());

        panelKontenera.removeAll();
        if (silnik.getZaznaczonyPerk() != null) {
            panelSzczegolyPerka.odswiez(gracz);
            panelKontenera.add(panelSzczegolyPerka);
        } else if (silnik.getZaznaczoneMiasto() != null) {
            panelMiasta.odswiez(gracz);
            panelKontenera.add(panelMiasta);
        } else if (silnik.getZaznaczonaJednostka() != null) {
            panelJednostki.odswiez(gracz);
            panelKontenera.add(panelJednostki);
        }
        panelKontenera.revalidate();
        panelKontenera.repaint();
    }

    private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(80, 80, 100));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        return sep;
    }
}