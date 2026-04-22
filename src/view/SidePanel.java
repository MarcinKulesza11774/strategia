package view;

import controller.GameController;
import model.GameState;
import model.entity.Unit;
import model.world.ResourceType;
import model.world.TileType;
import util.ConfigLoader;
import util.GameConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Boczny panel – lista jednostek + dropdown budowania.
 * Bez poziomego scrolla. Stała szerokość 210px.
 */
public class SidePanel extends JPanel {

    private final GameState      state;
    private final GameController controller;
    private final JPanel         unitList;
    private final JButton        btnPause;
    private       Frame          ownerFrame;

    private static Map<String, Map<String,Integer>> BUILD_COSTS;

    public static void initCosts() {
        BUILD_COSTS = new LinkedHashMap<>();
        for (ConfigLoader.BuildingCfg b : GameConfig.get().buildings())
            BUILD_COSTS.put(b.tileId(), b.cost());
    }

    public static Map<String,Integer> getCost(String tileId) {
        return BUILD_COSTS.getOrDefault(tileId, Map.of());
    }

    public SidePanel(GameState state, GameController controller) {
        this.state      = state;
        this.controller = controller;

        setLayout(new BorderLayout(0, 4));
        setBackground(new Color(22, 22, 34));
        setPreferredSize(new Dimension(210, 0));
        setMinimumSize(new Dimension(210, 0));
        setMaximumSize(new Dimension(210, Integer.MAX_VALUE));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(55, 55, 75)));

        // TOP: pauza + przycisk budowania
        JPanel top = new JPanel(new GridLayout(2, 1, 0, 3));
        top.setBackground(new Color(18, 18, 28));
        top.setBorder(new EmptyBorder(4, 4, 4, 4));

        btnPause = sideBtn("Pauza [SPACE]", new Color(50, 50, 80));
        btnPause.addActionListener(e -> state.setPaused(!state.isPaused()));

        JButton btnBuild = sideBtn("Budowanie ▾", new Color(35, 55, 80));
        btnBuild.addActionListener(e -> showBuildMenu(btnBuild));

        top.add(btnPause);
        top.add(btnBuild);
        add(top, BorderLayout.NORTH);

        // CENTER: lista jednostek
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(new Color(22, 22, 34));

        center.add(sectionLabel("JEDNOSTKI"));
        unitList = new JPanel();
        unitList.setLayout(new BoxLayout(unitList, BoxLayout.Y_AXIS));
        unitList.setBackground(new Color(22, 22, 34));
        unitList.setAlignmentX(LEFT_ALIGNMENT);
        center.add(unitList);

        JScrollPane scroll = new JScrollPane(center,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // BOTTOM: skróty
        JPanel hotkeys = new JPanel();
        hotkeys.setLayout(new BoxLayout(hotkeys, BoxLayout.Y_AXIS));
        hotkeys.setBackground(new Color(16, 16, 26));
        hotkeys.setBorder(new EmptyBorder(4, 6, 4, 4));
        for (String h : new String[]{
                "WSAD: kamera", "Scroll: zoom",
                "Klik Scroll: przewijanie",
                "SPACE: pauza", "LPM: zaznacz/buduj",
                "Drag: obszar", "PPM: ruch/cofnij",
                "2x LPM: zarządzaj jedn.", "ESC: anuluj"}) {
            JLabel l = new JLabel(h);
            l.setFont(new Font("SansSerif", Font.PLAIN, 9));
            l.setForeground(new Color(120, 120, 145));
            hotkeys.add(l);
        }
        add(hotkeys, BorderLayout.SOUTH);
    }

    // -------------------------------------------------------------------------
    // Menu budowania (popup)
    // -------------------------------------------------------------------------

    private void showBuildMenu(JComponent anchor) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(new Color(28, 28, 45));

        // Kategorie
        String[] catLabels = {"Konstrukcje", "Meble", "Sprzęt"};
        String[][] catTiles = {
            {TileType.WALL, TileType.FLOOR, TileType.DOOR, TileType.GATE},
            {TileType.BED, TileType.TABLE, TileType.CHAIR, TileType.CAMPFIRE, TileType.STOCKPILE},
            {TileType.ANVIL, TileType.FORGE, TileType.BOOKSHELF,
             TileType.TRAINING_DUMMY, TileType.THRONE, TileType.WATCHTOWER}
        };

        for (int ci = 0; ci < catLabels.length; ci++) {
            if (ci > 0) menu.addSeparator();
            JLabel catLbl = new JLabel("  " + catLabels[ci]);
            catLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
            catLbl.setForeground(new Color(140, 140, 200));
            catLbl.setBorder(new EmptyBorder(2, 4, 2, 4));
            menu.add(catLbl);

            for (String tileId : catTiles[ci]) {
                if (!BUILD_COSTS.containsKey(tileId)) continue;
                TileType tt = TileType.get(tileId);
                Map<String,Integer> cost = BUILD_COSTS.get(tileId);
                String costStr = costToString(cost);
                JMenuItem item = new JMenuItem(tt.label + "  [" + costStr + "]");
                item.setFont(new Font("SansSerif", Font.PLAIN, 11));
                item.setForeground(Color.WHITE);
                item.setBackground(new Color(28, 28, 45));
                item.setOpaque(true);
                item.addActionListener(e -> {
                    if (state.canAfford(cost)) state.setBuildMode(tileId);
                    else state.log("Za mało surowców: " + tt.label);
                });
                menu.add(item);
            }
        }
        menu.show(anchor, 0, anchor.getHeight());
    }

    // -------------------------------------------------------------------------
    // Odświeżanie listy jednostek
    // -------------------------------------------------------------------------

    public void update() {
        btnPause.setText(state.isPaused() ? "Wznów [SPACE]" : "Pauza [SPACE]");

        unitList.removeAll();
        for (Unit u : state.getPlayerUnits()) {
            unitList.add(makeUnitEntry(u));
        }
        unitList.revalidate();
        unitList.repaint();
    }

    private JPanel makeUnitEntry(Unit u) {
        JPanel entry = new JPanel(new BorderLayout(3, 0));
        entry.setBackground(u == state.getSelectedUnit()
                ? new Color(45, 65, 120) : new Color(28, 28, 45));
        entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        entry.setAlignmentX(LEFT_ALIGNMENT);
        entry.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(45, 45, 65)));

        // Kolorowy pasek po lewej (kolor klasy)
        JPanel bar = new JPanel();
        bar.setPreferredSize(new Dimension(4, 0));
        bar.setBackground(u.getUnitClass().color);
        entry.add(bar, BorderLayout.WEST);

        // Tekst
        JPanel info = new JPanel(new GridLayout(3, 1, 0, 0));
        info.setOpaque(false);
        info.setBorder(new EmptyBorder(3, 5, 3, 3));

        JLabel nameLabel = new JLabel(u.getName() + "  " + u.getUnitClass().label);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        nameLabel.setForeground(u.getUnitClass().color);

        // HP bar miniaturka
        JProgressBar hp = new JProgressBar(0, u.getMaxHp());
        hp.setValue(u.getHp());
        float ratio = (float) u.getHp() / u.getMaxHp();
        hp.setForeground(ratio > 0.5f ? new Color(60,200,60) : ratio > 0.25f ? new Color(220,180,0) : new Color(220,50,50));
        hp.setBackground(new Color(40,40,55));
        hp.setBorderPainted(false);
        hp.setPreferredSize(new Dimension(0, 5));

        JLabel stateLabel = new JLabel(u.getState().name().toLowerCase()
                + (u.isReadyToPromote() ? "  ★ AWANS!" : ""));
        stateLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        stateLabel.setForeground(u.isReadyToPromote()
                ? new Color(255, 210, 30) : new Color(140, 140, 160));

        info.add(nameLabel);
        info.add(hp);
        info.add(stateLabel);
        entry.add(info, BorderLayout.CENTER);

        // Kliknięcie = zaznacz; podwójne kliknięcie = otwórz okno
        entry.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                state.setSelectedUnit(u);
                controller.centerCameraOn(u);
                if (e.getClickCount() >= 2) openUnitWindow(u);
            }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                entry.setBackground(new Color(40, 60, 110));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                entry.setBackground(u == state.getSelectedUnit()
                        ? new Color(45, 65, 120) : new Color(28, 28, 45));
            }
        });

        return entry;
    }

    private void openUnitWindow(Unit u) {
        if (ownerFrame == null)
            ownerFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        new UnitInfoWindow(ownerFrame, u, controller);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private JButton sideBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        b.setAlignmentX(LEFT_ALIGNMENT);
        return b;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(new Color(140, 140, 200));
        l.setBorder(new EmptyBorder(6, 4, 2, 4));
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return l;
    }

    private String costToString(Map<String,Integer> cost) {
        StringBuilder sb = new StringBuilder();
        cost.forEach((k,v) -> { if (sb.length()>0) sb.append(" "); sb.append(v).append(k.charAt(0)); });
        return sb.toString();
    }
}
