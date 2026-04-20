package view;

import model.GameState;

import javax.swing.*;
import java.awt.*;

/**
 * Główne okno gry (JFrame).
 */
public class GameWindow extends JFrame {

    private final MapPanel  mapPanel;
    private final InfoPanel infoPanel;

    public GameWindow(GameState state) {
        super("Turowa Gra Strategiczna");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        mapPanel  = new MapPanel(state);
        infoPanel = new InfoPanel();

        // Menu
        JMenuBar bar   = new JMenuBar();
        JMenu    mGra  = new JMenu("Gra");
        JMenu    mHelp = new JMenu("Pomoc");

        JMenuItem miWyjdz = new JMenuItem("Wyjdz");
        miWyjdz.addActionListener(e -> System.exit(0));
        mGra.add(miWyjdz);

        JMenuItem miHelp = new JMenuItem("Instrukcja");
        miHelp.addActionListener(e -> showHelp());
        mHelp.add(miHelp);

        bar.add(mGra);
        bar.add(mHelp);
        setJMenuBar(bar);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.BLACK);
        root.add(mapPanel,  BorderLayout.CENTER);
        root.add(infoPanel, BorderLayout.EAST);
        setContentPane(root);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void showHelp() {
        JOptionPane.showMessageDialog(this,
            "STEROWANIE:\n" +
            "1. Kliknij swoja jednostke (niebieski krag), aby ja zaznaczyc.\n" +
            "2. Zielone pola = mozliwy ruch. Kliknij pole, aby sie przesunac.\n" +
            "3. Czerwone pola = mozliwy atak. Kliknij wroga, aby atakowac.\n" +
            "4. Kliknij 'Zakoncz ture' gdy skonczysz.\n\n" +
            "JEDNOSTKI:  W=Wojownik(30HP)  L=Lucznik(20HP)  R=Rycerz(50HP)\n\n" +
            "CEL: Zniszcz wszystkie jednostki AI (czerwone).",
            "Instrukcja", JOptionPane.INFORMATION_MESSAGE);
    }

    public MapPanel  getMapPanel()  { return mapPanel; }
    public InfoPanel getInfoPanel() { return infoPanel; }

    public void showGameOver(String winnerName) {
        String msg = winnerName.equals("Gracz")
            ? "Wygrales! Wszystkie jednostki wroga zostaly pokonane!"
            : "Przegrales! Twoje jednostki zostaly zniszczone.";
        JOptionPane.showMessageDialog(this, msg, "Koniec gry", JOptionPane.INFORMATION_MESSAGE);
    }
}
