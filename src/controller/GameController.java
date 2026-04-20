package controller;

import ai.SimpleAI;
import model.*;
import view.GameWindow;

import javax.swing.Timer;
import java.util.List;

/**
 * Kontroler (MVC) – łączy model z widokiem, obsługuje zdarzenia gracza i AI.
 */
public class GameController {

    private GameState  state;
    private GameWindow window;
    private SimpleAI   ai;

    public void startGame() {
        state  = new GameState();
        window = new GameWindow(state);
        ai     = new SimpleAI(state);

        window.getMapPanel().setClickListener(this::handleTileClick);
        window.getInfoPanel().setEndTurnListener(this::handleEndTurn);

        refresh();
        window.getInfoPanel().appendLog("Gra rozpoczeta. Twoja tura!");
        window.getInfoPanel().appendLog("Kliknij jednostke, aby ja wybrac.");
    }

    // ---------- zdarzenia ----------

    private void handleTileClick(int row, int col) {
        if (state.getPhase() != GameState.Phase.PLAYER_TURN) return;

        Tile clickedTile = state.getMap().getTile(row, col);
        Unit clickedUnit = clickedTile.getUnit();
        Unit selected    = state.getSelectedUnit();

        // Klik własnej jednostki → zaznacz
        if (clickedUnit != null && clickedUnit.getOwner() == state.getCurrentPlayer()) {
            state.setSelectedUnit(clickedUnit);
            window.getInfoPanel().appendLog("Wybrano: " + clickedUnit.getType().displayName);
            showHighlights(clickedUnit);
            refresh();
            return;
        }

        // Ruch na wolne pole
        if (selected != null && !selected.hasMovedThisTurn() && clickedUnit == null) {
            boolean[][] reachable = state.getMap().getReachableTiles(selected);
            if (reachable[row][col]) {
                state.moveUnit(selected, row, col);
                window.getInfoPanel().appendLog(
                        selected.getType().displayName + " -> [" + row + "," + col + "]");
                showHighlights(selected);
                refresh();
                return;
            }
        }

        // Atak na wroga
        if (selected != null && !selected.hasAttackedThisTurn()
                && clickedUnit != null && clickedUnit.getOwner() != state.getCurrentPlayer()) {
            boolean[][] attackable = state.getMap().getAttackableTiles(selected);
            if (attackable[row][col]) {
                window.getInfoPanel().appendLog(state.attack(selected, clickedUnit));
                state.setSelectedUnit(null);
                window.getMapPanel().clearHighlights();
                refresh();
                checkGameOver();
                return;
            }
        }

        // Klik pustego / niezaznaczalnego → odznacz
        state.setSelectedUnit(null);
        window.getMapPanel().clearHighlights();
        refresh();
    }

    private void handleEndTurn() {
        state.setSelectedUnit(null);
        window.getMapPanel().clearHighlights();
        state.endTurn();
        window.getInfoPanel().appendLog("--- Tura " + state.getTurnNumber() + " ---");
        refresh();
        if (state.getPhase() == GameState.Phase.AI_TURN) runAiTurn();
    }

    private void runAiTurn() {
        window.getInfoPanel().appendLog("[AI] Mysli...");
        Timer t = new Timer(600, e -> {
            List<String> log = ai.playTurn();
            log.forEach(window.getInfoPanel()::appendLog);
            state.endTurn();
            window.getInfoPanel().appendLog("Twoja tura!");
            refresh();
            checkGameOver();
        });
        t.setRepeats(false);
        t.start();
    }

    private void checkGameOver() {
        if (state.getPhase() == GameState.Phase.GAME_OVER) {
            String winner = state.getWinner().getName();
            window.getInfoPanel().appendLog("=== KONIEC GRY === Zwyciezca: " + winner);
            refresh();
            Timer t = new Timer(500, e -> window.showGameOver(winner));
            t.setRepeats(false);
            t.start();
        }
    }

    private void showHighlights(Unit unit) {
        boolean[][] move   = unit.hasMovedThisTurn()    ? null : state.getMap().getReachableTiles(unit);
        boolean[][] attack = unit.hasAttackedThisTurn() ? null : state.getMap().getAttackableTiles(unit);
        window.getMapPanel().setHighlights(move, attack);
    }

    private void refresh() {
        window.getInfoPanel().update(state);
        window.getMapPanel().repaint();
    }
}
