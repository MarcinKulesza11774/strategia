import javax.swing.*;
import java.awt.*;

/**
 * Górny pasek: zasoby gracza, licznik tury oraz przycisk drzewka rozwoju.
 * Implementuje Odswiezalny – OknoGry wola odswiez() na wszystkich komponentach.
 */
public class PanelGorny extends JPanel implements Odswiezalny {
    private final SilnikGry silnik;

    private final JLabel etykietaTury       = new JLabel();
    private final JLabel etykietaZloto      = new JLabel();
    private final JLabel etykietaPopulacja  = new JLabel();
    private final JLabel etykietaNauka      = new JLabel();
    private final JLabel etykietaPozywienie = new JLabel();
    private final JLabel etykietaMiasta     = new JLabel();
    private final JLabel etykietaJednostki  = new JLabel();

    private final JButton btnDrzewkoRozwoju = new JButton("Drzewko rozwoju");

    public PanelGorny(SilnikGry silnik, OknoGry oknoGry) {
        this.silnik = silnik;

        setBackground(new Color(30, 30, 38));
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        setLayout(new BorderLayout());

        JPanel zasoby = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        zasoby.setOpaque(false);
        stylizuj(etykietaTury, Font.BOLD);
        stylizuj(etykietaZloto, Font.PLAIN);
        stylizuj(etykietaPopulacja, Font.PLAIN);
        stylizuj(etykietaNauka, Font.PLAIN);
        stylizuj(etykietaPozywienie, Font.PLAIN);
        stylizuj(etykietaMiasta, Font.PLAIN);
        stylizuj(etykietaJednostki, Font.PLAIN);
        zasoby.add(etykietaTury);
        zasoby.add(etykietaZloto);
        zasoby.add(etykietaPopulacja);
        zasoby.add(etykietaNauka);
        zasoby.add(etykietaPozywienie);
        zasoby.add(etykietaMiasta);
        zasoby.add(etykietaJednostki);
        add(zasoby, BorderLayout.WEST);

        // TODO: podpiąć realne otwieranie panelu drzewka rozwoju, gdy powstanie.
        btnDrzewkoRozwoju.setFocusPainted(false);
        btnDrzewkoRozwoju.setBackground(new Color(90, 70, 140));
        btnDrzewkoRozwoju.setForeground(Color.WHITE);
        btnDrzewkoRozwoju.addActionListener(e -> oknoGry.przelaczDrzewko());
        add(btnDrzewkoRozwoju, BorderLayout.EAST);

        odswiez();
    }

    private void stylizuj(JLabel etykieta, int styl) {
        etykieta.setForeground(Color.WHITE);
        etykieta.setFont(new Font("SansSerif", styl, 12));
    }

    @Override
    public void odswiez() {
        Gracz gracz = silnik.getGraczLudzki();
        etykietaTury.setText("Tura " + silnik.getNumerTury() + " / " + silnik.getLimitTur());
        etykietaZloto.setText("Złoto: " + gracz.getZloto());
        etykietaPopulacja.setText("Populacja: " + gracz.getPopulacjaZagospodarowana()
                + " / " + gracz.getPopulacjaCaLkowita() + " (wolna: " + gracz.getWolnaPopulacja() + ")");
        etykietaNauka.setText("Nauka: " + gracz.getNauka());
        etykietaPozywienie.setText("Pożywienie: " + String.format("%+d", gracz.getBilansPozywienia()) + " / turę");
        etykietaMiasta.setText("Miasta: " + gracz.getMiasta().size());
        etykietaJednostki.setText("Jednostki: " + gracz.getJednostki().size());
    }
}