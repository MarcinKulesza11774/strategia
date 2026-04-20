package view;

import model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Panel rysujący mapę gry (kafelki, jednostki, podświetlenia).
 */
public class MapPanel extends JPanel {
    public static final int TILE_SIZE = 60;

    private final GameState state;
    private boolean[][] highlightMove;
    private boolean[][] highlightAttack;

    public interface TileClickListener {
        void onTileClicked(int row, int col);
    }
    private TileClickListener clickListener;

    public MapPanel(GameState state) {
        this.state = state;
        setPreferredSize(new Dimension(GameMap.COLS * TILE_SIZE, GameMap.ROWS * TILE_SIZE));
        setBackground(Color.BLACK);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = e.getX() / TILE_SIZE;
                int row = e.getY() / TILE_SIZE;
                if (row >= 0 && row < GameMap.ROWS && col >= 0 && col < GameMap.COLS)
                    if (clickListener != null) clickListener.onTileClicked(row, col);
            }
        });
    }

    public void setClickListener(TileClickListener l)       { this.clickListener   = l; }
    public void setHighlights(boolean[][] move, boolean[][] attack) {
        this.highlightMove   = move;
        this.highlightAttack = attack;
    }
    public void clearHighlights() { highlightMove = null; highlightAttack = null; }

    // ---------- rysowanie ----------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int r = 0; r < GameMap.ROWS; r++)
            for (int c = 0; c < GameMap.COLS; c++)
                drawTile(g2, state.getMap().getTile(r, c), r, c);
    }

    private void drawTile(Graphics2D g2, Tile tile, int r, int c) {
        int x = c * TILE_SIZE, y = r * TILE_SIZE;

        // Tło terenu
        g2.setColor(tile.getTerrain().color);
        g2.fillRect(x, y, TILE_SIZE, TILE_SIZE);

        // Nakładka właściciela
        if (tile.getOwner() != null) {
            Color oc = tile.getOwner().getColor();
            g2.setColor(new Color(oc.getRed(), oc.getGreen(), oc.getBlue(), 45));
            g2.fillRect(x, y, TILE_SIZE, TILE_SIZE);
        }

        // Podświetlenie ruchu (zielone)
        if (highlightMove != null && highlightMove[r][c]) {
            g2.setColor(new Color(80, 255, 80, 100));
            g2.fillRect(x, y, TILE_SIZE, TILE_SIZE);
            g2.setColor(new Color(80, 255, 80, 200));
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(x+1, y+1, TILE_SIZE-2, TILE_SIZE-2);
        }

        // Podświetlenie ataku (czerwone)
        if (highlightAttack != null && highlightAttack[r][c]) {
            g2.setColor(new Color(255, 60, 60, 130));
            g2.fillRect(x, y, TILE_SIZE, TILE_SIZE);
            g2.setColor(new Color(255, 60, 60, 220));
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(x+1, y+1, TILE_SIZE-2, TILE_SIZE-2);
        }

        // Siatka
        g2.setColor(new Color(0, 0, 0, 55));
        g2.setStroke(new BasicStroke(1));
        g2.drawRect(x, y, TILE_SIZE, TILE_SIZE);

        // Ikona terenu (tekst ASCII, działa na każdej JVM)
        drawTerrainLabel(g2, tile.getTerrain(), x, y);

        // Jednostka
        if (tile.getUnit() != null) drawUnit(g2, tile.getUnit(), x, y);

        // Zaznaczenie
        Unit sel = state.getSelectedUnit();
        if (sel != null && sel.getRow() == r && sel.getCol() == c) {
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(x+2, y+2, TILE_SIZE-4, TILE_SIZE-4);
        }
    }

    private void drawTerrainLabel(Graphics2D g2, Terrain terrain, int x, int y) {
        String label = switch (terrain) {
            case FOREST   -> "las";
            case MOUNTAIN -> "gory";
            case WATER    -> "~";
            default       -> "";
        };
        if (!label.isEmpty()) {
            g2.setFont(new Font("SansSerif", Font.ITALIC, 9));
            g2.setColor(new Color(0, 0, 0, 100));
            g2.drawString(label, x + 3, y + 13);
        }
    }

    private void drawUnit(Graphics2D g2, Unit unit, int x, int y) {
        int pad = 8, size = TILE_SIZE - pad * 2;
        int ux = x + pad, uy = y + pad;

        // Kółko gracza
        g2.setColor(unit.getOwner().getColor());
        g2.fillOval(ux, uy, size, size);

        // Obramowanie
        g2.setColor(state.getSelectedUnit() == unit ? Color.YELLOW : Color.WHITE);
        g2.setStroke(new BasicStroke(state.getSelectedUnit() == unit ? 3f : 1.5f));
        g2.drawOval(ux, uy, size, size);

        // Symbol (litera)
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        String sym = unit.getType().symbol;
        g2.setColor(Color.WHITE);
        g2.drawString(sym, ux + (size - fm.stringWidth(sym)) / 2,
                           uy + (size + fm.getAscent() - fm.getDescent()) / 2 - 1);

        // Pasek HP
        int barW = TILE_SIZE - 4;
        float ratio = (float) unit.getCurrentHp() / unit.getType().maxHp;
        g2.setColor(new Color(50, 50, 50));
        g2.fillRoundRect(x+2, y + TILE_SIZE - 8, barW, 5, 3, 3);
        g2.setColor(ratio > 0.5f ? new Color(80,200,80) : ratio > 0.25f ? new Color(220,180,0) : new Color(220,60,60));
        g2.fillRoundRect(x+2, y + TILE_SIZE - 8, (int)(barW * ratio), 5, 3, 3);

        // Wyszarzenie jeśli jednostka już działała
        if (unit.hasMovedThisTurn() || unit.hasAttackedThisTurn()) {
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillOval(ux, uy, size, size);
        }
    }
}
