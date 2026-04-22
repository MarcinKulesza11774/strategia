package view;

import controller.GameController;
import model.entity.Unit;
import model.entity.UnitClass;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Okno zarządzania jednostką.
 *
 * Otwiera się po dwukrotnym kliknięciu jednostki lub kliknięciu jej w panelu.
 * Pokazuje statystyki, aktualny rozkaz i przyciski akcji.
 *
 * Przyciski akcji wymagają zaznaczenia obszaru na mapie –
 * po kliknięciu przycisku okno się zamyka i gra czeka na przeciągnięcie myszą.
 *
 * @param oknoGlowne   okno nadrzędne (JFrame)
 * @param jednostka    jednostka którą zarządzamy
 * @param kontroler    kontroler gry do wydawania rozkazów
 */
public class UnitInfoWindow extends JDialog {

    private final Unit           jednostka;
    private final GameController kontroler;

    public UnitInfoWindow(Frame oknoGlowne, Unit jednostka, GameController kontroler) {
        super(oknoGlowne, jednostka.getImie() + " – " + jednostka.getKlasa().label, false);
        this.jednostka  = jednostka;
        this.kontroler  = kontroler;

        setSize(380, 520);
        setLocationRelativeTo(oknoGlowne);
        setResizable(false);
        getContentPane().setBackground(new Color(28, 28, 40));

        JPanel glownyPanel = new JPanel(new BorderLayout(0, 10));
        glownyPanel.setBackground(new Color(28, 28, 40));
        glownyPanel.setBorder(new EmptyBorder(14, 14, 14, 14));
        setContentPane(glownyPanel);

        glownyPanel.add(zbudujNaglowek(),    BorderLayout.NORTH);
        glownyPanel.add(zbudujStatystyki(),  BorderLayout.CENTER);

        JPanel dol = new JPanel(new BorderLayout(0, 8));
        dol.setOpaque(false);
        dol.add(zbudujPanelRozkazow(), BorderLayout.CENTER);
        dol.add(zbudujPanelAwansu(),   BorderLayout.SOUTH);
        glownyPanel.add(dol, BorderLayout.SOUTH);

        setVisible(true);
    }

    // =========================================================================
    // Nagłówek
    // =========================================================================

    private JPanel zbudujNaglowek() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 3));
        panel.setOpaque(false);

        JLabel imieLabel = new JLabel(jednostka.getImie());
        imieLabel.setFont(new Font("Serif", Font.BOLD, 22));
        imieLabel.setForeground(jednostka.getKlasa().color);

        JLabel klasaLabel = new JLabel(jednostka.getKlasa().label
            + "  (poziom " + jednostka.getKlasa().tier + ")");
        klasaLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        klasaLabel.setForeground(new Color(180, 180, 200));

        JLabel opisLabel = new JLabel("<html><i>" + jednostka.getKlasa().description + "</i></html>");
        opisLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        opisLabel.setForeground(new Color(150, 150, 175));

        panel.add(imieLabel);
        panel.add(klasaLabel);
        panel.add(opisLabel);
        return panel;
    }

    // =========================================================================
    // Statystyki
    // =========================================================================

    private JPanel zbudujStatystyki() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(3, 4, 3, 4);
        gbc.fill    = GridBagConstraints.HORIZONTAL;

        int wiersz = 0;

        // Pasek HP
        dodajWierszZPaskiem(panel, gbc, wiersz++, "HP",
            jednostka.getHp(), jednostka.getMaxHp(),
            new Color(60, 200, 60), new Color(220, 50, 50));

        // Pasek XP
        int xp    = jednostka.getXp();
        int xpMax = jednostka.xpDoAwansu();
        boolean maxPoziom = xpMax >= 999999;
        dodajWierszZPaskiem(panel, gbc, wiersz++, "XP",
            maxPoziom ? 1 : xp, maxPoziom ? 1 : xpMax,
            new Color(100, 180, 255), new Color(100, 180, 255));

        // Separator
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(60, 60, 80));
        gbc.gridx = 0; gbc.gridy = wiersz++; gbc.gridwidth = 3;
        panel.add(separator, gbc); gbc.gridwidth = 1;

        // Cztery statystyki
        dodajWierszStatystyki(panel, gbc, wiersz++,
            "VIT (Wytrzymałość)", jednostka.getWytrzymalosc(),
            "Max HP = " + jednostka.getMaxHp());
        dodajWierszStatystyki(panel, gbc, wiersz++,
            "STR (Siła)", jednostka.getSila(),
            "Szybkość budowania i zbierania");
        dodajWierszStatystyki(panel, gbc, wiersz++,
            "PRE (Precyzja)", jednostka.getPrecyzja(),
            "Skuteczność wytwarzania");
        dodajWierszStatystyki(panel, gbc, wiersz++,
            "CHA (Charyzma)", jednostka.getCharyzma(),
            "Leczenie i handel");

        // Stan i rozkaz
        gbc.gridx = 0; gbc.gridy = wiersz; gbc.gridwidth = 3;
        String stanTekst = "Stan: " + jednostka.getStan().name().toLowerCase()
            + "   Rozkaz: " + jednostka.getRozkaz().name().toLowerCase();
        JLabel stanLabel = etykieta(stanTekst);
        stanLabel.setForeground(new Color(180, 200, 180));
        panel.add(stanLabel, gbc);

        return panel;
    }

    private void dodajWierszZPaskiem(JPanel panel, GridBagConstraints gbc,
                                      int wiersz, String nazwa,
                                      int wartosc, int maksimum,
                                      Color kolorWysoki, Color kolorNiski) {
        gbc.gridx = 0; gbc.gridy = wiersz; gbc.weightx = 0;
        panel.add(etykieta(nazwa + ":"), gbc);

        JProgressBar pasek = new JProgressBar(0, Math.max(1, maksimum));
        pasek.setValue(Math.min(wartosc, maksimum));
        float stosunek = (float) wartosc / Math.max(1, maksimum);
        pasek.setForeground(stosunek > 0.5f ? kolorWysoki
            : stosunek > 0.25f ? new Color(220, 180, 0) : kolorNiski);
        pasek.setBackground(new Color(40, 40, 55));
        pasek.setBorderPainted(false);
        pasek.setPreferredSize(new Dimension(120, 10));
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(pasek, gbc);

        JLabel wartoscLabel = new JLabel(wartosc + "/" + maksimum);
        wartoscLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
        wartoscLabel.setForeground(Color.WHITE);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(wartoscLabel, gbc);
    }

    private void dodajWierszStatystyki(JPanel panel, GridBagConstraints gbc,
                                        int wiersz, String nazwa, int wartosc, String opis) {
        gbc.gridx = 0; gbc.gridy = wiersz;
        panel.add(etykieta(nazwa + ":"), gbc);

        JLabel wartoscLabel = new JLabel(String.valueOf(wartosc));
        wartoscLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        wartoscLabel.setForeground(new Color(120, 210, 255));
        wartoscLabel.setToolTipText(opis);
        gbc.gridx = 1;
        panel.add(wartoscLabel, gbc);

        JLabel opisLabel = etykieta(opis);
        opisLabel.setForeground(new Color(120, 120, 145));
        gbc.gridx = 2;
        panel.add(opisLabel, gbc);
    }

    // =========================================================================
    // Panel rozkazów
    // =========================================================================

    /**
     * Buduje panel z przyciskami rozkazów.
     *
     * Po kliknięciu przycisku okno się zamyka i gra przechodzi do trybu
     * oczekiwania na zaznaczenie obszaru – gracz przeciąga myszą prostokąt
     * na mapie który staje się obszarem rozkazu.
     */
    private JPanel zbudujPanelRozkazow() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 85)),
            "Rozkazy", 0, 0,
            new Font("SansSerif", Font.BOLD, 11), new Color(160, 160, 200)));

        UnitClass klasa = jednostka.getKlasa();

        // STÓJ – dostępna zawsze
        panel.add(przyciskRozkazu("Stój",
            "Jednostka stoi i nic nie robi",
            new Color(50, 50, 70),
            () -> {
                kontroler.wydajRozkaz(jednostka, Unit.Rozkaz.STOJ, -1, -1, -1, -1);
                dispose();
            }));

        // ZBIERAJ – dla jednostek które umieją zbierać
        if (klasa.canHarvest) {
            panel.add(przyciskRozkazu("Zbieraj [obszar]",
                "Zaznacz obszar na mapie – jednostka zbierze tam zasoby",
                new Color(40, 80, 40),
                () -> {
                    kontroler.czekajNaObszarRozkazu(jednostka, Unit.Rozkaz.ZBIERAJ);
                    dispose();
                }));
        }

        // BUDUJ – dla jednostek które umieją budować
        if (klasa.canBuild) {
            panel.add(przyciskRozkazu("Buduj [obszar]",
                "Zaznacz obszar na mapie – jednostka wybuduje tam zlecone budowle",
                new Color(80, 60, 20),
                () -> {
                    kontroler.czekajNaObszarRozkazu(jednostka, Unit.Rozkaz.BUDUJ);
                    dispose();
                }));
        }

        // PATROLUJ – dla jednostek które umieją walczyć
        if (klasa.canFight) {
            panel.add(przyciskRozkazu("Patroluj [obszar]",
                "Zaznacz obszar – jednostka będzie tam walczyć z wrogami",
                new Color(40, 40, 80),
                () -> {
                    kontroler.czekajNaObszarRozkazu(jednostka, Unit.Rozkaz.PATROLUJ);
                    dispose();
                }));
        }

        // EWAKUACJA – dla wszystkich jednostek gracza
        panel.add(przyciskRozkazu("Ewakuacja [strefa]",
            "Zaznacz strefę – jednostka ucieknie tam gdy wróg się zbliży",
            new Color(80, 20, 20),
            () -> {
                kontroler.czekajNaObszarRozkazu(jednostka, Unit.Rozkaz.EWAKUUJ);
                dispose();
            }));

        // COFNIJ ROZKAZ
        panel.add(przyciskRozkazu("Cofnij rozkaz",
            "Wróć do zachowania domyślnego dla tej klasy",
            new Color(40, 40, 55),
            () -> {
                kontroler.wydajRozkaz(jednostka, Unit.Rozkaz.BRAK, -1, -1, -1, -1);
                dispose();
            }));

        return panel;
    }

    private JButton przyciskRozkazu(String tekst, String podpowiedz,
                                     Color kolorTla, Runnable akcja) {
        JButton przycisk = new JButton("<html><center>" + tekst + "</center></html>");
        przycisk.setBackground(kolorTla);
        przycisk.setForeground(Color.WHITE);
        przycisk.setFont(new Font("SansSerif", Font.BOLD, 11));
        przycisk.setFocusPainted(false);
        przycisk.setBorderPainted(false);
        przycisk.setOpaque(true);
        przycisk.setToolTipText(podpowiedz);
        przycisk.setPreferredSize(new Dimension(0, 40));
        przycisk.addActionListener(e -> akcja.run());
        return przycisk;
    }

    // =========================================================================
    // Panel awansu
    // =========================================================================

    private JPanel zbudujPanelAwansu() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 85)),
            "Awans", 0, 0,
            new Font("SansSerif", Font.BOLD, 11), new Color(160, 160, 200)));

        UnitClass klasa = jednostka.getKlasa();
        if (klasa.promotionOptions.isEmpty()) {
            panel.add(etykieta("Ta klasa jest ostateczna – nie można awansować."),
                      BorderLayout.CENTER);
            return panel;
        }

        boolean czyGotowa = jednostka.gotowaNaAwans();

        if (!czyGotowa) {
            int brakujaceXp = klasa.xpToPromote - jednostka.getXp();
            JLabel info = etykieta("Brak " + brakujaceXp + " XP do odblokowania awansu.");
            info.setForeground(new Color(150, 150, 170));
            panel.add(info, BorderLayout.CENTER);
            return panel;
        }

        JPanel przyciskiAwansu = new JPanel(new GridLayout(0, 1, 4, 4));
        przyciskiAwansu.setOpaque(false);

        for (String docelowaKlasaId : klasa.promotionOptions) {
            UnitClass docelowa = UnitClass.get(docelowaKlasaId);
            JButton przycisk = new JButton(
                "<html><b>" + docelowa.label + "</b>  "
                + "<span style='color:#aaa'>" + docelowa.description + "</span></html>");
            przycisk.setBackground(new Color(50, 100, 50));
            przycisk.setForeground(Color.WHITE);
            przycisk.setFocusPainted(false);
            przycisk.setBorderPainted(false);
            przycisk.setOpaque(true);
            przycisk.setHorizontalAlignment(SwingConstants.LEFT);
            przycisk.setMargin(new Insets(6, 8, 6, 8));
            przycisk.addActionListener(e -> {
                kontroler.awansujJednostke(jednostka, docelowaKlasaId);
                dispose();
            });
            przyciskiAwansu.add(przycisk);
        }

        panel.add(przyciskiAwansu, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Pomocnicze
    // =========================================================================

    private JLabel etykieta(String tekst) {
        JLabel etykieta = new JLabel(tekst);
        etykieta.setFont(new Font("SansSerif", Font.PLAIN, 11));
        etykieta.setForeground(new Color(160, 160, 180));
        return etykieta;
    }
}
