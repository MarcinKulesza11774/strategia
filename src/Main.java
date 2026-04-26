import javax.swing.SwingUtilities;

/**
 * Punkt startowy aplikacji – gra strategiczna turowa.
 * Uruchamia główne okno gry w wątku obsługi zdarzeń Swing (EDT).
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            OknoGry oknoGry = new OknoGry();
            oknoGry.setVisible(true);
        });
    }
}
