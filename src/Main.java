import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            OknoGry oknoGry = new OknoGry();
            oknoGry.setVisible(true);
        });
    }
}
