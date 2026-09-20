import javax.swing.*;
import java.awt.*;

/**
 * Panel kontekstowy wyświetlany gdy zaznaczona jest jednostka gracza.
 * Pokazuje statystyki i przyciski akcji (budowa miasta, ulepszenia).
 */
public class PanelJednostki extends PanelKontekstowy {
    private final JButton btnBudujMiasto  = new JButton();
    private final JButton btnUlepszAtak   = new JButton("+ Atak (30 zł)");
    private final JButton btnUlepszZycie  = new JButton("+ HP (30 zł)");
    private final JButton btnUlepszRuch   = new JButton("+ Ruch (30 zł)");

    public PanelJednostki(SilnikGry silnik, OknoGry oknoGry) {
        super(silnik, oknoGry);
        btnBudujMiasto.addActionListener(e -> { silnik.budujMiastoZaznaczonegoGracza(); oknoGry.odswiez(); });
        btnUlepszAtak.addActionListener(e ->  { silnik.ulepszAtakZaznaczonegoGracza();  oknoGry.odswiez(); });
        btnUlepszZycie.addActionListener(e -> { silnik.ulepszZycieZaznaczonegoGracza(); oknoGry.odswiez(); });
        btnUlepszRuch.addActionListener(e ->  { silnik.ulepszRuchZaznaczonegoGracza();  oknoGry.odswiez(); });
    }

    @Override
    public void odswiez(Gracz gracz) {
        removeAll();
        Jednostka j = silnik.getZaznaczonaJednostka();
        if (j == null) return;

        add(naglowek("JEDNOSTKA", new Color(200, 180, 80)));
        add(info("HP: " + j.getPunktyZycia() + "/" + j.getMaksymalnePunktyZycia()));
        add(info("ATK: " + j.getObrazenia()
            + "  RUCH: " + j.getPozostalyruch() + "/" + j.getMaksymalnyRuch()));

        btnBudujMiasto.setText("Buduj miasto (" + gracz.getKosztBudowyMiasta() + " zł)");
        dodajPrzycisk(btnBudujMiasto, gracz.getZloto() >= gracz.getKosztBudowyMiasta());

        if (silnik.czyZaznaczonaJednostkaWMiescie()) {
            add(Box.createVerticalStrut(4));
            add(info("Ulepszenia (" + j.getPoziomAtaku()
                + "/" + j.getPoziomZycia() + "/" + j.getPoziomRuchu() + "):"));
            dodajPrzycisk(btnUlepszAtak,  j.moznaUlepszycAtak()  && gracz.getZloto() >= Jednostka.KOSZT_ULEPSZENIA);
            dodajPrzycisk(btnUlepszZycie, j.moznaUlepszycZycie() && gracz.getZloto() >= Jednostka.KOSZT_ULEPSZENIA);
            dodajPrzycisk(btnUlepszRuch,  j.moznaUlepszycRuch()  && gracz.getZloto() >= Jednostka.KOSZT_ULEPSZENIA);
        }

        revalidate();
        repaint();
    }
}
