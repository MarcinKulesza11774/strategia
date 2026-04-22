package controller;

import ai.UnitAI;
import model.GameState;
import model.entity.Unit;
import model.entity.UnitSpawner;
import model.world.TileType;
import view.Camera;
import view.GameWindow;
import view.SidePanel;

import javax.swing.*;
import java.util.List;
import java.util.Map;

/**
 * Kontroler gry – łączy widok z modelem.
 *
 * Obsługuje kliknięcia i przeciągnięcia myszy, wydaje rozkazy jednostkom,
 * zarządza trybem budowania i trybem oczekiwania na obszar rozkazu.
 */
public class GameController {

    private GameState  stan;
    private GameWindow okno;
    private UnitAI     ai;
    private UnitSpawner spawner;

    /** Jednostka czekająca na zaznaczenie obszaru dla swojego rozkazu. */
    private Unit   jednostkaOczekujacaNaObszar = null;
    /** Rozkaz który zostanie przypisany po zaznaczeniu obszaru. */
    private Unit.Rozkaz rozkazOczekujacy = null;

    private final Timer odswiezanieBocznegoPanel;

    public GameController() {
        odswiezanieBocznegoPanel = new Timer(200, e -> {
            if (okno != null) okno.getSidePanel().update();
        });
    }

    public void start() {
        stan    = new GameState();
        ai      = new UnitAI(stan);
        spawner = new UnitSpawner();
        okno    = new GameWindow(stan, this);
        odswiezanieBocznegoPanel.start();
        okno.getGamePanel().startLoop();
        okno.getGamePanel().requestFocusInWindow();
    }

    public void nowaGra() {
        okno.getGamePanel().stopLoop();
        odswiezanieBocznegoPanel.stop();
        stan    = new GameState();
        ai      = new UnitAI(stan);
        spawner = new UnitSpawner();
        okno.dispose();
        okno    = new GameWindow(stan, this);
        odswiezanieBocznegoPanel.start();
        okno.getGamePanel().startLoop();
    }

    // =========================================================================
    // Tick – wywoływany z game loop
    // =========================================================================

    /**
     * Główny tick logiki gry, wywoływany co klatkę przez GamePanel.
     * Kamera aktualizuje się zawsze (nawet na pauzie).
     *
     * @param dt  czas od ostatniej klatki w sekundach
     */
    public void tick(double dt) {
        if (!stan.isPaused()) {
            stan.tick(dt);
            spawner.tick(stan);
            // Kopiujemy listę żeby uniknąć ConcurrentModificationException
            for (Unit jednostka : List.copyOf(stan.getUnits())) {
                if (jednostka.zyje()) ai.tick(jednostka, dt);
            }
        }
    }

    // =========================================================================
    // Kliknięcia myszy
    // =========================================================================

    /**
     * Lewy przycisk myszy:
     *   - w trybie budowania: postaw kafelek
     *   - kliknięcie własnej jednostki: zaznacz
     *   - kliknięcie wroga (gdy jest zaznaczona jednostka bojowa): rozkaz ataku
     *   - puste pole: odznacz
     */
    public void onLeftClick(int kafelekX, int kafelekY) {
        if (!stan.getMap().inBounds(kafelekX, kafelekY)) return;

        String trybBudowania = stan.getBuildMode();
        if (trybBudowania != null) {
            postaw(kafelekX, kafelekY, trybBudowania);
            return;
        }

        Unit kliknieta = znajdzJednostkeNa(kafelekX, kafelekY);
        if (kliknieta != null) {
            if (kliknieta.nalezydoGracza()) {
                stan.setSelectedUnit(kliknieta);
            } else {
                // Kliknięto wroga – wyślij zaznaczoną jednostkę bojową do ataku
                Unit zaznaczona = stan.getSelectedUnit();
                if (zaznaczona != null && zaznaczona.getKlasa().canFight) {
                    wydajRozkaz(zaznaczona, Unit.Rozkaz.PATROLUJ,
                        kliknieta.getKafelekX(), kliknieta.getKafelekY(),
                        kliknieta.getKafelekX(), kliknieta.getKafelekY());
                    stan.log(zaznaczona.getImie() + " atakuje " + kliknieta.getImie());
                }
            }
            return;
        }
        stan.setSelectedUnit(null);
    }

    /**
     * Przeciągnięcie lewym przyciskiem (obszar zaznaczenia):
     *   - jeśli jest tryb budowania: postaw kafelki na całym obszarze
     *   - jeśli czekamy na obszar rozkazu: przypisz obszar do rozkazu jednostki
     *   - normalnie: zaznacz wszystkie jednostki w obszarze
     */
    public void onAreaSelect(int x1, int y1, int x2, int y2) {
        String trybBudowania = stan.getBuildMode();
        if (trybBudowania != null) {
            for (int y = y1; y <= y2; y++)
                for (int x = x1; x <= x2; x++)
                    postaw(x, y, trybBudowania);
            return;
        }

        // Czy czekamy na obszar rozkazu?
        if (jednostkaOczekujacaNaObszar != null && rozkazOczekujacy != null) {
            wydajRozkaz(jednostkaOczekujacaNaObszar, rozkazOczekujacy, x1, y1, x2, y2);
            stan.log(jednostkaOczekujacaNaObszar.getImie() + ": rozkaz "
                + rozkazOczekujacy.name().toLowerCase()
                + " [" + x1 + "," + y1 + " – " + x2 + "," + y2 + "]");
            jednostkaOczekujacaNaObszar = null;
            rozkazOczekujacy = null;
            return;
        }

        // Zaznacz wszystkie jednostki gracza w obszarze
        List<Unit> wObszarze = stan.getPlayerUnits().stream()
            .filter(u -> u.getKafelekX() >= x1 && u.getKafelekX() <= x2
                      && u.getKafelekY() >= y1 && u.getKafelekY() <= y2)
            .toList();
        if (!wObszarze.isEmpty()) {
            stan.setSelectedUnit(wObszarze.get(0));
            if (wObszarze.size() > 1)
                stan.log("Zaznaczono " + wObszarze.size() + " jednostek");
        }
    }

    /**
     * Prawy przycisk myszy:
     *   - w trybie budowania: cofnij znacznik na tym kafelku
     *   - normalnie: wydaj zaznaczonej jednostce rozkaz ruchu do klikniętego miejsca
     */
    public void onRightClick(int kafelekX, int kafelekY) {
        String trybBudowania = stan.getBuildMode();
        if (trybBudowania != null) {
            var kafelek = stan.getMap().getTile(kafelekX, kafelekY);
            if (kafelek != null && kafelek.isMarkedForBuild()) {
                SidePanel.getCost(trybBudowania).forEach((typ, ilosc) ->
                    stan.addResource(typ, ilosc));
                kafelek.clearBuildMark();
            }
            return;
        }

        Unit zaznaczona = stan.getSelectedUnit();
        if (zaznaczona != null && stan.getMap().isPassable(kafelekX, kafelekY)) {
            // Rozkaz ruchu: patroluj w punkcie docelowym (1x1 obszar)
            wydajRozkaz(zaznaczona, Unit.Rozkaz.PATROLUJ, kafelekX, kafelekY, kafelekX, kafelekY);
            stan.log(zaznaczona.getImie() + " → [" + kafelekX + "," + kafelekY + "]");
        }
    }

    // =========================================================================
    // Rozkazy
    // =========================================================================

    /**
     * Wydaje jednostce rozkaz z obszarem.
     *
     * @param jednostka  jednostka która dostaje rozkaz
     * @param rozkaz     typ rozkazu
     * @param x1,y1      lewy górny róg obszaru (-1 jeśli brak obszaru)
     * @param x2,y2      prawy dolny róg obszaru
     */
    public void wydajRozkaz(Unit jednostka, Unit.Rozkaz rozkaz,
                             int x1, int y1, int x2, int y2) {
        jednostka.ustawRozkaz(rozkaz, x1, y1, x2, y2);
    }

    /**
     * Ustawia tryb oczekiwania na obszar rozkazu.
     * Następne przeciągnięcie myszą na mapie wyznaczy obszar.
     *
     * @param jednostka   jednostka dla której czekamy na obszar
     * @param rozkaz      rozkaz do przypisania po zaznaczeniu
     */
    public void czekajNaObszarRozkazu(Unit jednostka, Unit.Rozkaz rozkaz) {
        jednostkaOczekujacaNaObszar = jednostka;
        rozkazOczekujacy            = rozkaz;
        stan.log("Zaznacz obszar rozkazu dla " + jednostka.getImie()
            + " (" + rozkaz.name().toLowerCase() + ")...");
    }

    /** Czy aktualnie oczekujemy na zaznaczenie obszaru rozkazu. */
    public boolean czyOczekujeNaObszar() {
        return jednostkaOczekujacaNaObszar != null;
    }

    // =========================================================================
    // Awans i rekrutacja
    // =========================================================================

    /**
     * Awansuje jednostkę na wskazaną klasę docelową.
     * Wymaga by jednostka była gotowa do awansu (gotowaNaAwans = true).
     */
    public void awansujJednostke(Unit jednostka, String docelowaKlasaId) {
        if (!jednostka.gotowaNaAwans()) {
            stan.log("Jednostka nie jest jeszcze gotowa do awansu!");
            return;
        }
        String poprzedniaKlasa = jednostka.getKlasa().label;
        jednostka.awansuj(docelowaKlasaId);
        stan.log(jednostka.getImie() + " awansował: "
            + poprzedniaKlasa + " → " + jednostka.getKlasa().label + "!");
    }

    // =========================================================================
    // Budowanie
    // =========================================================================

    private void postaw(int kafelekX, int kafelekY, String typKafelka) {
        if (!stan.getMap().inBounds(kafelekX, kafelekY)) return;
        var kafelek = stan.getMap().getTile(kafelekX, kafelekY);
        if (kafelek.isMarkedForBuild()) return;

        Map<String, Integer> koszt = SidePanel.getCost(typKafelka);
        if (!stan.canAfford(koszt)) {
            stan.log("Za mało surowców: " + TileType.get(typKafelka).label);
            return;
        }
        stan.spend(koszt);
        kafelek.markForBuild(typKafelka);
    }

    // =========================================================================
    // Pomocnicze
    // =========================================================================

    /** Przesuwa kamerę tak żeby jednostka była widoczna na środku ekranu. */
    public void centerCameraOn(Unit jednostka) {
        Camera kamera = okno.getGamePanel().getCamera();
        kamera.setCam(jednostka.getPozycjaX() - 35, jednostka.getPozycjaY() - 22);
    }

    private Unit znajdzJednostkeNa(int kafelekX, int kafelekY) {
        for (Unit jednostka : stan.getUnits()) {
            if (jednostka.zyje()
                    && jednostka.getKafelekX() == kafelekX
                    && jednostka.getKafelekY() == kafelekY) {
                return jednostka;
            }
        }
        return null;
    }

    // Stare nazwy zachowane dla kompatybilności z GameWindow
    public void newGame() { nowaGra(); }
    public void promoteUnitTo(Unit u, String id) { awansujJednostke(u, id); }
    public void recruit(String id, Map<String, Integer> koszt) {
        stan.log("Rekrutacja wyłączona – jednostki przychodzą same jako wędrowcy.");
    }
}
