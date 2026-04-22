import controller.GameController;
import model.building.RoomDef;
import model.entity.UnitClass;
import model.world.TileType;
import util.GameConfig;
import view.SidePanel;

import javax.swing.SwingUtilities;

/**
 * Punkt startowy.
 * Kolejność: JSON → typy kafelków → klasy jednostek → pokoje → gra
 */
public class Main {
    public static void main(String[] args) {
        GameConfig.init();
        TileType.loadAll();
        UnitClass.loadAll();   // teraz ładuje ze swojego własnego pliku
        RoomDef.loadAll();
        SidePanel.initCosts();

        SwingUtilities.invokeLater(() -> new GameController().start());
    }
}
