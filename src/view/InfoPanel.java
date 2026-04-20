package view;

import model.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel boczny z informacjami o grze i logiem akcji.
 */
public class InfoPanel extends JPanel {

    private final JLabel    lblTurn;
    private final JLabel    lblPhase;
    private final JLabel    lblUnitName;
    private final JLabel    lblUnitHp;
    private final JLabel    lblUnitStats;
    private final JLabel    lblUnitStatus;
    private final JTextArea logArea;
    private final JButton   btnEndTurn;

    public interface EndTurnListener { void onEndTurn(); }
    private EndTurnListener endTurnListener;

    public InfoPanel() {
        setLayout(new BorderLayout(0, 8));
        setBackground(new Color(30, 30, 40));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(220, 0));

        // --- Nagłówek ---
        JPanel top = new JPanel(new GridLayout(2, 1, 0, 4));
        top.setOpaque(false);
        lblTurn  = label("Tura 1", 15, Font.BOLD,  Color.WHITE);
        lblPhase = label("Twoja tura", 12, Font.PLAIN, new Color(160, 255, 160));
        top.add(lblTurn);
        top.add(lblPhase);
        add(top, BorderLayout.NORTH);

        // --- Info o jednostce ---
        JPanel unitPanel = new JPanel(new GridLayout(4, 1, 0, 4));
        unitPanel.setOpaque(false);
        unitPanel.setBorder(titledBorder("Wybrana jednostka"));
        lblUnitName   = label("–",                          13, Font.BOLD,  Color.WHITE);
        lblUnitHp     = label("HP: –",                      11, Font.PLAIN, new Color(120, 220, 120));
        lblUnitStats  = label("ATK:– DEF:– RUCH:–",        11, Font.PLAIN, new Color(200, 200, 200));
        lblUnitStatus = label("",                           11, Font.ITALIC,new Color(220, 200, 120));
        unitPanel.add(lblUnitName);
        unitPanel.add(lblUnitHp);
        unitPanel.add(lblUnitStats);
        unitPanel.add(lblUnitStatus);

        // --- Log ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        logArea.setBackground(new Color(20, 20, 30));
        logArea.setForeground(new Color(200, 200, 200));
        logArea.setBorder(new EmptyBorder(4, 4, 4, 4));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(titledBorder("Log akcji"));

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(unitPanel, BorderLayout.NORTH);
        center.add(scroll,    BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // --- Przycisk ---
        btnEndTurn = new JButton("Zakoncz ture >");
        btnEndTurn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnEndTurn.setBackground(new Color(60, 120, 220));
        btnEndTurn.setForeground(Color.WHITE);
        btnEndTurn.setFocusPainted(false);
        btnEndTurn.setBorder(new EmptyBorder(10, 10, 10, 10));
        btnEndTurn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEndTurn.addActionListener(e -> { if (endTurnListener != null) endTurnListener.onEndTurn(); });
        add(btnEndTurn, BorderLayout.SOUTH);
    }

    public void update(GameState state) {
        Player cur = state.getCurrentPlayer();
        lblTurn.setText("Tura " + state.getTurnNumber());
        lblPhase.setText(cur.isAI() ? "Tura AI..." : "Twoja tura");
        lblPhase.setForeground(cur.isAI() ? new Color(255,160,160) : new Color(160,255,160));

        Unit sel = state.getSelectedUnit();
        if (sel != null) {
            lblUnitName.setText("[" + sel.getType().symbol + "] " +
                    sel.getType().displayName + " (" + sel.getOwner().getName() + ")");
            lblUnitHp.setText("HP: " + sel.getCurrentHp() + " / " + sel.getType().maxHp);
            lblUnitStats.setText("ATK:" + sel.getType().attack +
                    "  DEF:" + sel.getType().defense + "  RUCH:" + sel.getType().moveRange);
            List<String> st = new ArrayList<>();
            if (sel.hasMovedThisTurn())    st.add("ruszyl");
            if (sel.hasAttackedThisTurn()) st.add("atakowal");
            lblUnitStatus.setText(st.isEmpty() ? "Gotowy" : String.join(", ", st));
        } else {
            lblUnitName.setText("–");
            lblUnitHp.setText("HP: –");
            lblUnitStats.setText("ATK:– DEF:– RUCH:–");
            lblUnitStatus.setText("");
        }

        boolean playerTurn = state.getPhase() == GameState.Phase.PLAYER_TURN;
        btnEndTurn.setEnabled(playerTurn);
        btnEndTurn.setBackground(playerTurn ? new Color(60,120,220) : new Color(70,70,80));
    }

    public void appendLog(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void setEndTurnListener(EndTurnListener l) { this.endTurnListener = l; }

    // --- helpers ---
    private JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        return l;
    }

    private Border titledBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80,80,100)),
                title, TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 11), new Color(180,180,200));
    }
}
