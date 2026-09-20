import javax.swing.*;
import java.awt.*;

public class DialogUstawien extends JDialog {
    private UstawieniaGry wynik = null;

    private static final String[] ROZMIARY = {
        "Mała (16×24)", "Średnia (24×36)", "Duża (32×48)", "Ogromna (40×60)"
    };
    private static final int[][] WYMIARY = {
        {16, 24}, {24, 36}, {32, 48}, {40, 60}
    };

    private DialogUstawien(JFrame owner) {
        super(owner, "Nowa gra – ustawienia", true);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        JComboBox<String> comboRozmiar = new JComboBox<>(ROZMIARY);
        comboRozmiar.setSelectedIndex(1);
        JSpinner spinnerRegiony = new JSpinner(new SpinnerNumberModel(18, 6, 60, 1));
        JSpinner spinnerZalazki = new JSpinner(new SpinnerNumberModel(60, 10, 200, 5));
        JSpinner spinnerAI      = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1));
        JSpinner spinnerTury    = new JSpinner(new SpinnerNumberModel(40, 10, 200, 5));

        JPanel panelPol = new JPanel(new GridLayout(5, 2, 8, 8));
        panelPol.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));
        panelPol.add(new JLabel("Rozmiar mapy:"));         panelPol.add(comboRozmiar);
        panelPol.add(new JLabel("Liczba regionów:"));      panelPol.add(spinnerRegiony);
        panelPol.add(new JLabel("Zalążki terenu:"));       panelPol.add(spinnerZalazki);
        panelPol.add(new JLabel("Liczba przeciwników:"));  panelPol.add(spinnerAI);
        panelPol.add(new JLabel("Liczba tur:"));           panelPol.add(spinnerTury);

        JPanel panelPrzyciskow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelPrzyciskow.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));
        JButton btnStart  = new JButton("Zacznij grę");
        JButton btnAnuluj = new JButton("Anuluj");
        btnStart.addActionListener(e -> {
            int[] wym = WYMIARY[comboRozmiar.getSelectedIndex()];
            wynik = new UstawieniaGry(wym[0], wym[1],
                (int) spinnerRegiony.getValue(), (int) spinnerZalazki.getValue(),
                (int) spinnerAI.getValue(), (int) spinnerTury.getValue());
            dispose();
        });
        btnAnuluj.addActionListener(e -> dispose());
        panelPrzyciskow.add(btnAnuluj);
        panelPrzyciskow.add(btnStart);

        add(panelPol, BorderLayout.CENTER);
        add(panelPrzyciskow, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    public static UstawieniaGry pokaz(JFrame owner) {
        DialogUstawien dialog = new DialogUstawien(owner);
        dialog.setVisible(true);
        return dialog.wynik;
    }
}
