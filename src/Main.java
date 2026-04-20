import controller.GameController;
import javax.swing.SwingUtilities;

/**
 * Punkt startowy aplikacji.
 * Uruchamia grę na wątku EDT zgodnie z wymaganiami Swing.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameController controller = new GameController();
            controller.startGame();
        });
    }
}
