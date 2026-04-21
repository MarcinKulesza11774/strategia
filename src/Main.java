import controller.GameController;
import model.building.RoomDef;
import model.entity.UnitClass;
import model.world.TileType;
import util.GameConfig;
import view.SidePanel;

import javax.swing.SwingUtilities;

/**
 * Punkt startowy gry.
 * Kolejność inicjalizacji:
 *   1. GameConfig.init()    – ładuje wszystkie pliki JSON z /data
 *   2. TileType.loadAll()   – rejestruje typy kafelków
 *   3. UnitClass.loadAll()  – rejestruje klasy jednostek
 *   4. RoomDef.loadAll()    – rejestruje definicje pokoi
 *   5. SidePanel.initCosts() – ładuje koszty budowania
 *   6. GameController.start() – tworzy stan gry i okno
 */
public class Main {
    public static void main(String[] args) {
        // Załaduj konfigurację z JSON przed wszystkim innym
        GameConfig.init();
        TileType.loadAll();
        UnitClass.loadAll();
        RoomDef.loadAll();
        SidePanel.initCosts();

        SwingUtilities.invokeLater(() -> new GameController().start());
    }
}
