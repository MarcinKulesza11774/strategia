public class BudynekWMiescie extends ObiektNaMapie{
    private Budynek budynek;

    public BudynekWMiescie(Budynek budynek, int row, int col, Gracz wlasciciel) {
        this.budynek = budynek;
        this.row = row;
        this.col = col;
        this.wlasciciel = wlasciciel;
    }
}
