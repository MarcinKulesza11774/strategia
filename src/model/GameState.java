package model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralny stan gry (Model w MVC).
 */
public class GameState {
    public enum Phase { PLAYER_TURN, AI_TURN, GAME_OVER }

    private final GameMap    map;
    private final Player[]   players;
    private final List<Unit> units;

    private int    currentPlayerIndex;
    private int    turnNumber;
    private Phase  phase;
    private Player winner;
    private Unit   selectedUnit;

    public GameState() {
        map     = new GameMap();
        players = new Player[]{
            new Player("Gracz", new Color(60, 120, 220), false),
            new Player("AI",    new Color(220,  60,  60), true)
        };
        units       = new ArrayList<>();
        phase       = Phase.PLAYER_TURN;
        turnNumber  = 1;
        currentPlayerIndex = 0;
        spawnUnits();
    }

    private void spawnUnits() {
        placeUnit(UnitType.WARRIOR, players[0], 0, 0);
        placeUnit(UnitType.ARCHER,  players[0], 1, 0);
        placeUnit(UnitType.KNIGHT,  players[0], 0, 1);

        placeUnit(UnitType.WARRIOR, players[1], GameMap.ROWS-1, GameMap.COLS-1);
        placeUnit(UnitType.ARCHER,  players[1], GameMap.ROWS-2, GameMap.COLS-1);
        placeUnit(UnitType.KNIGHT,  players[1], GameMap.ROWS-1, GameMap.COLS-2);
    }

    private void placeUnit(UnitType type, Player owner, int row, int col) {
        Tile tile = map.getTile(row, col);
        if (tile == null || tile.getTerrain() == Terrain.WATER) return;
        Unit unit = new Unit(type, owner, row, col);
        units.add(unit);
        tile.setUnit(unit);
        tile.setOwner(owner);
    }

    /** Kończy turę i przekazuje kontrolę drugiemu graczowi. */
    public void endTurn() {
        selectedUnit = null;
        currentPlayerIndex = 1 - currentPlayerIndex;
        if (currentPlayerIndex == 0) turnNumber++;
        phase = getCurrentPlayer().isAI() ? Phase.AI_TURN : Phase.PLAYER_TURN;
        for (Unit u : units)
            if (u.getOwner() == getCurrentPlayer()) u.resetTurn();
        checkWinCondition();
    }

    private void checkWinCondition() {
        boolean p0 = units.stream().anyMatch(u -> u.getOwner() == players[0] && u.isAlive());
        boolean p1 = units.stream().anyMatch(u -> u.getOwner() == players[1] && u.isAlive());
        if (!p0) { winner = players[1]; phase = Phase.GAME_OVER; }
        if (!p1) { winner = players[0]; phase = Phase.GAME_OVER; }
    }

    /** Przesuwa jednostkę na docelowe pole. */
    public void moveUnit(Unit unit, int toRow, int toCol) {
        map.getTile(unit.getRow(), unit.getCol()).setUnit(null);
        Tile dest = map.getTile(toRow, toCol);
        dest.setUnit(unit);
        dest.setOwner(unit.getOwner());
        unit.setPosition(toRow, toCol);
        unit.setMovedThisTurn(true);
    }

    /**
     * Przeprowadza atak. Zwraca opis walki do logu.
     */
    public String attack(Unit attacker, Unit target) {
        int damage   = attacker.calculateDamage(target);
        boolean alive = target.takeDamage(damage);
        attacker.setAttackedThisTurn(true);
        attacker.setMovedThisTurn(true);

        String log = attacker.getType().displayName + " atakuje " +
                     target.getType().displayName + " – " + damage + " obrażeń.";
        if (!alive) {
            map.getTile(target.getRow(), target.getCol()).setUnit(null);
            units.remove(target);
            log += " Pokonany!";
        }
        checkWinCondition();
        return log;
    }

    public GameMap    getMap()                { return map; }
    public Player[]   getPlayers()            { return players; }
    public List<Unit> getUnits()              { return units; }
    public Player     getCurrentPlayer()      { return players[currentPlayerIndex]; }
    public int        getTurnNumber()         { return turnNumber; }
    public Phase      getPhase()              { return phase; }
    public Player     getWinner()             { return winner; }
    public Unit       getSelectedUnit()       { return selectedUnit; }
    public void       setSelectedUnit(Unit u) { this.selectedUnit = u; }
}
