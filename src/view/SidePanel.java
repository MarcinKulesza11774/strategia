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
 * Boczny panel – budowanie, rekrutacja, lista jednostek.
 * Naprawiony layout: brak poziomego scrolla, stała szerokość 200px.
 */
public class SidePanel extends JPanel {

    private final GameState      state;
    private final GameController controller;
    private final JPanel         unitList;
    private final JButton        btnPause;

    // Koszty budowania ładowane z JSON
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

        setLayout(new BorderLayout(0,4));
        setBackground(new Color(25,25,35));
        setPreferredSize(new Dimension(200,0));
        setMinimumSize(new Dimension(200,0));
        setMaximumSize(new Dimension(200,Integer.MAX_VALUE));
        setBorder(BorderFactory.createMatteBorder(0,1,0,0,new Color(60,60,80)));

        // --- Pauza ---
        btnPause = new JButton("Pauza [SPACE]");
        styleBtn(btnPause, new Color(50,50,80));
        btnPause.addActionListener(e -> state.setPaused(!state.isPaused()));
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(20,20,30));
        topBar.setBorder(new EmptyBorder(4,4,4,4));
        topBar.add(btnPause);
        add(topBar, BorderLayout.NORTH);

        // --- Środek: scrollowalny ---
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(new Color(25,25,35));

        // Budowanie
        center.add(sectionLabel("BUDOWANIE"));
        for (ConfigLoader.BuildingCfg b : GameConfig.get().buildings()) {
            center.add(makeBuildBtn(b.tileId(), b.label(), b.cost()));
        }

        // Rekrutacja
        center.add(sectionLabel("REKRUTACJA"));
        for (ConfigLoader.RecruitCfg r : GameConfig.get().recruit()) {
            center.add(makeRecruitBtn(r.unitId(), r.cost()));
        }

        // Lista jednostek
        center.add(sectionLabel("JEDNOSTKI"));
        unitList = new JPanel();
        unitList.setLayout(new BoxLayout(unitList, BoxLayout.Y_AXIS));
        unitList.setBackground(new Color(25,25,35));
        unitList.setAlignmentX(LEFT_ALIGNMENT);
        center.add(unitList);

        JScrollPane scroll = new JScrollPane(center,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBackground(new Color(25,25,35));
        add(scroll, BorderLayout.CENTER);

        // --- Dół: skróty ---
        JPanel hotkeys = new JPanel();
        hotkeys.setLayout(new BoxLayout(hotkeys, BoxLayout.Y_AXIS));
        hotkeys.setBackground(new Color(18,18,28));
        hotkeys.setBorder(new EmptyBorder(4,6,4,4));
        String[] hks = {"WSAD: kamera","Scroll: zoom","SPACE: pauza",
                        "LPM: wybierz/buduj","Drag LPM: obszar","PPM: ruch/cofnij","ESC: anuluj"};
        for (String h : hks) {
            JLabel l = new JLabel(h);
            l.setFont(new Font("SansSerif",Font.PLAIN,9));
            l.setForeground(new Color(140,140,160));
            hotkeys.add(l);
        }
        add(hotkeys, BorderLayout.SOUTH);
    }

    private JButton makeBuildBtn(String tileId, String label, Map<String,Integer> cost) {
        String costStr = costToString(cost);
        JButton btn = new JButton("<html><b>"+label+"</b><br><small style='color:#aaa'>"+costStr+"</small></html>");
        styleBtn(btn, new Color(38,38,58));
        btn.addActionListener(e -> {
            if (state.canAfford(cost)) state.setBuildMode(tileId);
            else state.log("Za mało surowców: "+label);
        });
        return btn;
    }

    private JButton makeRecruitBtn(String unitId, Map<String,Integer> cost) {
        String label    = model.entity.UnitClass.get(unitId).label;
        String costStr  = costToString(cost);
        JButton btn = new JButton("<html><b>"+label+"</b><br><small style='color:#aaa'>"+costStr+"</small></html>");
        styleBtn(btn, new Color(28,45,68));
        btn.addActionListener(e -> controller.recruit(unitId, cost));
        return btn;
    }

    private void styleBtn(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif",Font.PLAIN,11));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMargin(new Insets(4,6,4,6));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height+4));
        btn.setAlignmentX(LEFT_ALIGNMENT);
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif",Font.BOLD,11));
        l.setForeground(new Color(140,140,200));
        l.setBorder(new EmptyBorder(8,4,2,4));
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return l;
    }

    private String costToString(Map<String,Integer> cost) {
        StringBuilder sb = new StringBuilder();
        cost.forEach((k,v) -> {
            if (sb.length()>0) sb.append(" ");
            sb.append(v).append(" ").append(k);
        });
        return sb.toString();
    }

    public void update() {
        btnPause.setText(state.isPaused() ? "Wznów [SPACE]" : "Pauza [SPACE]");

        unitList.removeAll();
        for (Unit u : state.getPlayerUnits()) {
            JButton btn = new JButton(
                "<html><b>"+u.getUnitClass().label+"</b> #"+u.getId()+
                "<br><small>HP:"+u.getHp()+"/"+u.getMaxHp()+" "+u.getState()+"</small></html>");
            styleBtn(btn, u == state.getSelectedUnit() ? new Color(50,70,130) : new Color(32,32,52));
            btn.addActionListener(e -> { state.setSelectedUnit(u); controller.centerCameraOn(u); });

            // Przycisk awansu
            if (u.canPromote()) {
                JButton promo = new JButton("Awansuj!");
                styleBtn(promo, new Color(120,90,0));
                promo.addActionListener(e -> controller.promoteUnit(u));
                JPanel row = new JPanel(new BorderLayout(2,0));
                row.setBackground(new Color(32,32,52));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
                row.setAlignmentX(LEFT_ALIGNMENT);
                row.add(btn, BorderLayout.CENTER);
                row.add(promo, BorderLayout.EAST);
                unitList.add(row);
            } else {
                unitList.add(btn);
            }
        }
        unitList.revalidate();
        unitList.repaint();
    }
}
