public class BudynekWMiescie extends ObiektNaMapie {
    private final Budynek budynek;
    private final Miasto miasto; // miasto do którego należy ten budynek

    public BudynekWMiescie(Budynek budynek, int row, int col, Gracz wlasciciel, Miasto miasto) {
        this.budynek = budynek;
        this.row = row;
        this.col = col;
        this.wlasciciel = wlasciciel;
        this.miasto = miasto;
    }

    public Budynek getBudynek() { return budynek; }
    public Miasto getMiasto()   { return miasto; }
}
