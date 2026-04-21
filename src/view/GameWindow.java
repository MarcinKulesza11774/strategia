package view;

import controller.GameController;
import model.GameState;

import javax.swing.*;
import java.awt.*;

/**
 * Główne okno gry.
 */
public class GameWindow extends JFrame {
    private final GamePanel gamePanel;
    private final SidePanel sidePanel;

    public GameWindow(GameState state, GameController controller) {
        super("Fortress – Gra Strategiczna");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        gamePanel = new GamePanel(state, controller);
        sidePanel = new SidePanel(state, controller);

        JMenuBar bar = new JMenuBar();
        JMenu mGra   = new JMenu("Gra");
        JMenuItem miNowa  = new JMenuItem("Nowa gra");
        JMenuItem miWyjdz = new JMenuItem("Wyjdź");
        miNowa.addActionListener(e  -> controller.newGame());
        miWyjdz.addActionListener(e -> System.exit(0));
        mGra.add(miNowa); mGra.addSeparator(); mGra.add(miWyjdz);

        JMenu mHelp  = new JMenu("Pomoc");
        JMenuItem miH = new JMenuItem("Instrukcja");
        miH.addActionListener(e -> showHelp());
        mHelp.add(miH);
        bar.add(mGra); bar.add(mHelp);
        setJMenuBar(bar);

        // Layout: mapa zajmuje resztę, panel boczny ma stałą szerokość
        JPanel root = new JPanel(new BorderLayout(0,0));
        root.setBackground(Color.BLACK);
        root.add(gamePanel, BorderLayout.CENTER);
        root.add(sidePanel, BorderLayout.EAST);
        setContentPane(root);

        setSize(1320, 760);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setVisible(true);
        gamePanel.requestFocusInWindow();
    }

    private void showHelp() {
        JOptionPane.showMessageDialog(this, """
                === FORTRESS – INSTRUKCJA ===
                
                KAMERA:
                  WSAD / Strzałki  = przewijanie
                  Scroll myszy     = zoom (przybliż/oddal)
                  SPACE            = pauza / wznów
                  ESC              = anuluj tryb budowania
                
                ZAZNACZANIE I ROZKAZY:
                  LPM na jednostce       = zaznacz
                  PPM na polu            = rozkaz ruchu
                  LPM na wrogu           = rozkaz ataku
                  Przeciągnij LPM        = zaznacz obszar
                    → w trybie budowania: postaw wiele kafelków naraz
                    → normalnie: zaznacz wszystkie jednostki w obszarze
                
                BUDOWANIE:
                  Wybierz element z panelu bocznego
                  LPM klik  = postaw jeden kafelek
                  LPM drag  = postaw obszar naraz
                  PPM       = cofnij jeden kafelek
                  ESC       = wyjdź z trybu budowania
                
                POKOJE (system sąsiedztwa):
                  Otocz obszar FLOOR ścianami (WALL) i wstaw meble
                  Pokój zostanie automatycznie wykryty i da buffy.
                  Sąsiadujące pokoje dają dodatkowe bonusy – sprawdź rooms.json!
                
                PLIKI KONFIGURACYJNE (folder /data):
                  tiles.json       = typy kafelków i ich właściwości
                  units.json       = klasy jednostek i ich statystyki
                  rooms.json       = definicje pokoi i buffy sąsiedztwa
                  buildings.json   = co można budować i za ile
                  recruitment.json = jakie jednostki rekrutować i za ile
                  game_config.json = wszystkie parametry gry (fale, ekonomia, AI...)
                """,
                "Pomoc", JOptionPane.INFORMATION_MESSAGE);
    }

    public GamePanel getGamePanel() { return gamePanel; }
    public SidePanel getSidePanel() { return sidePanel; }
}
