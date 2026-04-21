package view;

import controller.GameController;
import model.GameState;
import model.entity.Unit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Główny panel gry z game loop na osobnym wątku.
 * Obsługuje: przewijanie kamerą, zoom scrollem, klikanie, zaznaczanie obszaru.
 */
public class GamePanel extends JPanel {

    private final GameState      state;
    private final Camera         cam;
    private final MapRenderer    renderer;
    private final GameController controller;

    private Thread   gameThread;
    private volatile boolean running = false;

    // FPS
    private int  fps=0, fpsCount=0;
    private long fpsTimer=System.currentTimeMillis();

    // Zaznaczanie obszaru myszą
    private boolean dragging    = false;
    private int     dragStartX, dragStartY;   // kafelki świata
    private int     dragEndX,   dragEndY;
    private int     dragScreenX, dragScreenY; // piksele ekranu

    public GamePanel(GameState state, GameController controller) {
        this.state      = state;
        this.controller = controller;
        this.renderer   = new MapRenderer();
        this.cam        = new Camera(1100, 700,
                state.getMap().fortressX - 30, state.getMap().fortressY - 20);

        setBackground(Color.BLACK);
        setFocusable(true);
        requestFocusInWindow();
        setupInput();
    }

    // -------------------------------------------------------------------------
    // Game loop
    // -------------------------------------------------------------------------

    public void startLoop() {
        running    = true;
        gameThread = new Thread(this::loop, "GameLoop");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    public void stopLoop() { running = false; }

    private void loop() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            double dt = Math.min((now - last) / 1_000_000_000.0, 0.05);
            last = now;

            controller.tick(dt);
            SwingUtilities.invokeLater(this::repaint);

            fpsCount++;
            long now2 = System.currentTimeMillis();
            if (now2 - fpsTimer >= 1000) { fps=fpsCount; fpsCount=0; fpsTimer=now2; }

            try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        cam.resize(getWidth(), getHeight());

        // Zaznaczony obszar w kafelkach (normalizowany)
        int sx1 = Math.min(dragStartX, dragEndX);
        int sy1 = Math.min(dragStartY, dragEndY);
        int sx2 = Math.max(dragStartX, dragEndX);
        int sy2 = Math.max(dragStartY, dragEndY);

        renderer.render(g2, state, cam, state.getSelectedUnit(), sx1, sy1, sx2, sy2, dragging);
        HudRenderer.render(g2, state, fps, getWidth(), getHeight(), cam.getTileSize());

        if (state.isGameWon()) drawWinScreen(g2);
    }

    private void drawWinScreen(Graphics2D g) {
        g.setColor(new Color(0,0,0,160)); g.fillRect(0,0,getWidth(),getHeight());
        g.setFont(new Font("Serif",Font.BOLD,48));
        String t="ZWYCIĘSTWO!"; FontMetrics fm=g.getFontMetrics();
        g.setColor(new Color(255,210,30)); g.drawString(t,(getWidth()-fm.stringWidth(t))/2,getHeight()/2-20);
        g.setFont(new Font("SansSerif",Font.PLAIN,20));
        String s="Pierwsza fala odparta. Gra trwa dalej..."; fm=g.getFontMetrics();
        g.setColor(Color.WHITE); g.drawString(s,(getWidth()-fm.stringWidth(s))/2,getHeight()/2+30);
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private void setupInput() {
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT,  KeyEvent.VK_A -> cam.setLeft(true);
                    case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> cam.setRight(true);
                    case KeyEvent.VK_UP,    KeyEvent.VK_W -> cam.setUp(true);
                    case KeyEvent.VK_DOWN,  KeyEvent.VK_S -> cam.setDown(true);
                    case KeyEvent.VK_SPACE                -> state.setPaused(!state.isPaused());
                    case KeyEvent.VK_ESCAPE               -> { state.setBuildMode(null); state.setSelectedUnit(null); }
                }
            }
            @Override public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT,  KeyEvent.VK_A -> cam.setLeft(false);
                    case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> cam.setRight(false);
                    case KeyEvent.VK_UP,    KeyEvent.VK_W -> cam.setUp(false);
                    case KeyEvent.VK_DOWN,  KeyEvent.VK_S -> cam.setDown(false);
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (SwingUtilities.isLeftMouseButton(e)) {
                    dragStartX  = cam.screenToTileX(e.getX());
                    dragStartY  = cam.screenToTileY(e.getY());
                    dragEndX    = dragStartX;
                    dragEndY    = dragStartY;
                    dragScreenX = e.getX();
                    dragScreenY = e.getY();
                    dragging    = false;
                }
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (dragging) {
                        // Zaznaczanie obszaru zakończone
                        int tx1 = Math.min(dragStartX, dragEndX);
                        int ty1 = Math.min(dragStartY, dragEndY);
                        int tx2 = Math.max(dragStartX, dragEndX);
                        int ty2 = Math.max(dragStartY, dragEndY);
                        controller.onAreaSelect(tx1, ty1, tx2, ty2);
                        dragging = false;
                    } else {
                        // Zwykłe kliknięcie
                        controller.onLeftClick(cam.screenToTileX(e.getX()), cam.screenToTileY(e.getY()));
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    controller.onRightClick(cam.screenToTileX(e.getX()), cam.screenToTileY(e.getY()));
                }
            }
            @Override public void mouseClicked(MouseEvent e) {}
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    // Zacznij drag jeśli przesunięto o >5px
                    if (!dragging && Math.hypot(e.getX()-dragScreenX, e.getY()-dragScreenY) > 5)
                        dragging = true;
                    dragEndX = cam.screenToTileX(e.getX());
                    dragEndY = cam.screenToTileY(e.getY());
                }
            }
        });

        // Scroll = zoom
        addMouseWheelListener(e -> {
            int delta = e.getWheelRotation() < 0 ? 1 : -1;
            cam.zoom(delta, e.getX(), e.getY(), getWidth(), getHeight());
        });
    }

    public Camera getCamera() { return cam; }
}
