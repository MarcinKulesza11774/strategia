package ai;

import model.GameState;
import model.entity.Unit;
import model.entity.UnitClass;
import model.world.Tile;
import model.world.TileType;
import model.world.WorldMap;
import util.GameConfig;
import util.JsonParser;
import util.Pathfinder;

import java.util.List;
import java.util.Map;

/**
 * Maszyna stanów AI dla wszystkich jednostek.
 *
 * Wywołuj tick() dla każdej żywej jednostki raz na klatkę.
 *
 * Zasada działania:
 *   1. Rozkaz gracza (Unit.getRozkaz()) określa co jednostka ma robić.
 *   2. Jeśli rozkaz to BRAK – AI wybiera zachowanie domyślne dla klasy.
 *   3. Ruch odbywa się wyłącznie po wyliczonej ścieżce (Pathfinder A*),
 *      dzięki czemu jednostki nie przechodzą przez ściany.
 *
 * Jednostki NIE działają automatycznie jeśli nie mają rozkazu (poza strażnikami
 * którzy patrolują). Robotnik bez rozkazu STOI.
 *
 * @param stan  centralny stan gry (mapa, jednostki, zasoby)
 */
public class UnitAI {

    private final GameState stan;
    private final int xpZaZbieranie;
    private final int xpZaBudowanie;
    private final int xpZaAtak;
    private final int xpZaZabicie;

    public UnitAI(GameState stan) {
        this.stan = stan;
        Map<String, Object> konfig = JsonParser.asMap(
            GameConfig.get().loader().gameConfig.get("genericUnit"));
        xpZaZbieranie = JsonParser.getInt(konfig, "xpPerHarvestTick", 1);
        xpZaBudowanie = JsonParser.getInt(konfig, "xpPerBuildTick",   1);
        xpZaAtak      = JsonParser.getInt(konfig, "xpPerAttackHit",   8);
        xpZaZabicie   = JsonParser.getInt(konfig, "xpPerKill",        25);
    }

    /**
     * Wykonuje jeden tick AI dla podanej jednostki.
     *
     * @param jednostka  jednostka do zaktualizowania
     * @param dt         czas od ostatniej klatki w sekundach
     */
    public void tick(Unit jednostka, double dt) {
        if (!jednostka.zyje()) return;
        jednostka.zmniejszCooldown();

        if (jednostka.nalezydoGracza()) {
            tickJednostkiGracza(jednostka, dt);
        } else {
            tickWraga(jednostka, dt);
        }
    }

    // =========================================================================
    // Jednostki gracza
    // =========================================================================

    private void tickJednostkiGracza(Unit jednostka, double dt) {
        Unit.Rozkaz rozkaz = jednostka.getRozkaz();

        switch (rozkaz) {
            case STOJ    -> jednostka.ustawStan(Unit.Stan.STOI);
            case ZBIERAJ -> wykonajRozkazZbierania(jednostka, dt);
            case BUDUJ   -> wykonajRozkazBudowania(jednostka, dt);
            case PATROLUJ -> wykonajRozkazPatrolu(jednostka, dt);
            case EWAKUUJ  -> wykonajRozkazEwakuacji(jednostka, dt);
            case BRAK    -> wykonajDomyslneZachowanie(jednostka, dt);
        }
    }

    // -------------------------------------------------------------------------
    // Rozkaz: ZBIERAJ
    // -------------------------------------------------------------------------

    /**
     * Jednostka szuka zasobów w wyznaczonym obszarze rozkazu i zbiera je.
     * Obszar rozkazu jest ustawiany gdy gracz przeciągnie zaznaczenie po mapie.
     */
    private void wykonajRozkazZbierania(Unit jednostka, double dt) {
        WorldMap mapa = stan.getMap();

        // Czy mamy jeszcze aktywny cel?
        if (jednostka.maCelAkcji()) {
            int celX = jednostka.getCelAkcjiX();
            int celY = jednostka.getCelAkcjiY();
            Tile kafelek = mapa.getTile(celX, celY);

            // Cel mógł zniknąć (zebrany przez inną jednostkę)
            if (kafelek == null || !TileType.get(kafelek.getTypeId()).isResource) {
                jednostka.wyczysCelAkcji();
                jednostka.ustawSciezke(null);
                return;
            }

            if (stoisObok(jednostka, celX, celY)) {
                // Stoimy obok – zbieraj
                zbieraj(jednostka, kafelek, celX, celY);
            } else {
                idźWzdluzSciezki(jednostka, celX, celY, dt);
            }
            return;
        }

        // Szukaj nowego zasobu w obszarze rozkazu
        int x1 = jednostka.getRozkazObszarX1();
        int y1 = jednostka.getRozkazObszarY1();
        int x2 = jednostka.getRozkazObszarX2();
        int y2 = jednostka.getRozkazObszarY2();

        int[] zasob = (x1 >= 0)
            ? mapa.findNearestFreeResourceInArea(jednostka.getKafelekX(), jednostka.getKafelekY(), x1, y1, x2, y2)
            : mapa.findNearestFreeResource(jednostka.getKafelekX(), jednostka.getKafelekY(), GameConfig.get().workerSearchRadius());

        if (zasob == null) {
            jednostka.ustawStan(Unit.Stan.STOI); // brak zasobów w obszarze
            return;
        }

        // Zarezerwuj i ustaw jako cel
        mapa.getTile(zasob[0], zasob[1]).setMarkedForHarvest(true);
        jednostka.ustawCelAkcji(zasob[0], zasob[1]);
        idźWzdluzSciezki(jednostka, zasob[0], zasob[1], dt);
    }

    /**
     * Wykonuje jeden tick zbierania na kafelku obok jednostki.
     * STR przyspiesza zbieranie – wyższy STR = więcej ticków na klatkę.
     */
    private void zbieraj(Unit jednostka, Tile kafelek, int celX, int celY) {
        jednostka.ustawStan(Unit.Stan.ZBIERA);
        int iloscTickow = Math.max(1, jednostka.getSila() / 4);
        for (int i = 0; i < iloscTickow; i++) {
            if (!kafelek.isHarvestDone()) kafelek.tickHarvest();
        }
        jednostka.zdobadzXp(xpZaZbieranie);

        if (kafelek.isHarvestDone()) {
            TileType typ = TileType.get(kafelek.getTypeId());
            // Ilość surowców bazowo 5-12, zwiększona przez STR
            int ilosc = typ.harvestYieldMin
                + (int)(Math.random() * (typ.harvestYieldMax - typ.harvestYieldMin + 1))
                + jednostka.getSila() / 5;
            if (typ.harvestResource() != null) {
                stan.addResource(typ.harvestResource().id, ilosc);
            }
            kafelek.setType(typ.harvestRemnant().id);
            kafelek.setMarkedForHarvest(false);
            kafelek.resetHarvestProgress();
            jednostka.wyczysCelAkcji();
            jednostka.ustawSciezke(null);
        }
    }

    // -------------------------------------------------------------------------
    // Rozkaz: BUDUJ
    // -------------------------------------------------------------------------

    /**
     * Jednostka szuka zadania budowy w obszarze rozkazu i buduje.
     * Czas budowy: bazowo 120 klatek, zmniejszone przez STR (min 20).
     */
    private void wykonajRozkazBudowania(Unit jednostka, double dt) {
        WorldMap mapa = stan.getMap();

        if (jednostka.maCelAkcji()) {
            int celX = jednostka.getCelAkcjiX();
            int celY = jednostka.getCelAkcjiY();
            Tile kafelek = mapa.getTile(celX, celY);

            if (kafelek == null || !kafelek.isMarkedForBuild()) {
                jednostka.wyczysCelAkcji();
                jednostka.ustawSciezke(null);
                return;
            }

            if (stoisObok(jednostka, celX, celY)) {
                buduj(jednostka, kafelek, celX, celY);
            } else {
                idźWzdluzSciezki(jednostka, celX, celY, dt);
            }
            return;
        }

        int[] zadanie = mapa.findNearestBuildTask(jednostka.getKafelekX(), jednostka.getKafelekY());
        if (zadanie == null) {
            jednostka.ustawStan(Unit.Stan.STOI);
            return;
        }
        jednostka.ustawCelAkcji(zadanie[0], zadanie[1]);
        idźWzdluzSciezki(jednostka, zadanie[0], zadanie[1], dt);
    }

    private void buduj(Unit jednostka, Tile kafelek, int celX, int celY) {
        jednostka.ustawStan(Unit.Stan.BUDUJE);
        jednostka.zwiekszPostepAkcji();
        jednostka.zdobadzXp(xpZaBudowanie);

        int potrzebneTicki = Math.max(20, 120 - jednostka.getSila() * 2);
        if (jednostka.getPostepAkcji() >= potrzebneTicki) {
            jednostka.resetujPostepAkcji();
            String oczekiwanyTyp = kafelek.getPendingBuildTypeId();
            if (oczekiwanyTyp != null) {
                stan.getMap().setTile(celX, celY, oczekiwanyTyp);
                kafelek.clearBuildMark();
                stan.getRoomSystem().rescan();
                stan.log(jednostka.getImie() + " wybudował: " + TileType.get(oczekiwanyTyp).label);
            }
            jednostka.wyczysCelAkcji();
            jednostka.ustawSciezke(null);
        }
    }

    // -------------------------------------------------------------------------
    // Rozkaz: PATROLUJ
    // -------------------------------------------------------------------------

    /**
     * Jednostka patroluje w wyznaczonym obszarze i atakuje wrogów w nim.
     * Nie wychodzi za obszar rozkazu.
     */
    private void wykonajRozkazPatrolu(Unit jednostka, double dt) {
        // Szukaj wrogów w obszarze patrolu
        Unit wrog = znajdzNajblizszegoWraga(jednostka, 15);
        if (wrog != null && jednostka.kafelekWObszarzeRozkazu(wrog.getKafelekX(), wrog.getKafelekY())) {
            if (odleglosc(jednostka, wrog) <= jednostka.getKlasa().attackRange) {
                atakuj(jednostka, wrog);
            } else {
                idźWzdluzSciezki(jednostka, (int)wrog.getPozycjaX(), (int)wrog.getPozycjaY(), dt);
            }
            return;
        }

        // Wróg poza obszarem lub brak – patroluj
        if (!jednostka.maSciezke()) {
            int x1 = jednostka.getRozkazObszarX1();
            int y1 = jednostka.getRozkazObszarY1();
            int x2 = jednostka.getRozkazObszarX2();
            int y2 = jednostka.getRozkazObszarY2();
            if (x1 < 0) { jednostka.ustawStan(Unit.Stan.STOI); return; }

            int celX = x1 + (int)(Math.random() * (x2 - x1 + 1));
            int celY = y1 + (int)(Math.random() * (y2 - y1 + 1));
            idźWzdluzSciezki(jednostka, celX, celY, dt);
        } else {
            if (jednostka.krokWzdluzSciezki(dt)) {
                jednostka.ustawSciezke(null);
                jednostka.ustawStan(Unit.Stan.STOI);
            } else {
                jednostka.ustawStan(Unit.Stan.PATROLUJE);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Rozkaz: EWAKUUJ
    // -------------------------------------------------------------------------

    /**
     * Gdy wróg jest w pobliżu (zasięg 8) – uciekaj do wyznaczonej strefy ewakuacji.
     * Gdy bezpiecznie – stój w strefie.
     */
    private void wykonajRozkazEwakuacji(Unit jednostka, double dt) {
        boolean wrogBlisko = znajdzNajblizszegoWraga(jednostka, 8) != null;
        int x1 = jednostka.getRozkazObszarX1();
        int y1 = jednostka.getRozkazObszarY1();
        int x2 = jednostka.getRozkazObszarX2();
        int y2 = jednostka.getRozkazObszarY2();

        if (x1 < 0) { jednostka.ustawStan(Unit.Stan.STOI); return; }

        boolean jestWStrefie = jednostka.kafelekWObszarzeRozkazu(
            jednostka.getKafelekX(), jednostka.getKafelekY());

        if (wrogBlisko && !jestWStrefie && !jednostka.maSciezke()) {
            int celX = (x1 + x2) / 2;
            int celY = (y1 + y2) / 2;
            idźWzdluzSciezki(jednostka, celX, celY, dt);
        } else if (jednostka.maSciezke()) {
            if (jednostka.krokWzdluzSciezki(dt)) {
                jednostka.ustawSciezke(null);
            }
            jednostka.ustawStan(Unit.Stan.UCIEKA);
        } else {
            jednostka.ustawStan(Unit.Stan.STOI);
        }
    }

    // -------------------------------------------------------------------------
    // Domyślne zachowanie (Rozkaz.BRAK)
    // -------------------------------------------------------------------------

    /**
     * Domyślne zachowanie gdy gracz nie wydał rozkazu.
     * Strażnicy patrolują twierdzę i atakują wrogów.
     * Robotnicy, zbieracze itp. – STOJĄ i czekają na rozkaz.
     * Kapłan leczy pobliskich sojuszników.
     */
    private void wykonajDomyslneZachowanie(Unit jednostka, double dt) {
        UnitClass klasa = jednostka.getKlasa();

        if (klasa.canFight) {
            // Strażnicy mają domyślny patrol bez rozkazu
            wykonajDomyslnyPatrol(jednostka, dt);
        } else if (klasa.id.equals(UnitClass.CLERIC)) {
            tickKaplan(jednostka, dt);
        } else {
            // Robotnicy, budowniczowie, zbieracze – stoją bez rozkazu
            jednostka.ustawStan(Unit.Stan.STOI);
        }
    }

    private void wykonajDomyslnyPatrol(Unit jednostka, double dt) {
        Unit wrog = znajdzNajblizszegoWraga(jednostka, GameConfig.get().fightRange() * 2);
        if (wrog != null) {
            if (odleglosc(jednostka, wrog) <= jednostka.getKlasa().attackRange) {
                atakuj(jednostka, wrog);
            } else {
                idźWzdluzSciezki(jednostka, (int)wrog.getPozycjaX(), (int)wrog.getPozycjaY(), dt);
            }
            return;
        }

        if (!jednostka.maSciezke()) {
            double kat     = Math.random() * Math.PI * 2;
            double promien = 3 + Math.random() * 5;
            int celX = (int)(stan.getMap().fortressX + Math.cos(kat) * promien);
            int celY = (int)(stan.getMap().fortressY + Math.sin(kat) * promien);
            idźWzdluzSciezki(jednostka, celX, celY, dt);
        } else {
            if (jednostka.krokWzdluzSciezki(dt)) {
                jednostka.ustawSciezke(null);
            }
            jednostka.ustawStan(Unit.Stan.PATROLUJE);
        }
    }

    private void tickKaplan(Unit jednostka, double dt) {
        jednostka.ustawStan(Unit.Stan.LECZY);
        if (jednostka.getCooldownAtaku() > 0) return;
        for (Unit sojusznik : stan.getPlayerUnits()) {
            if (sojusznik == jednostka || !sojusznik.zyje()) continue;
            if (odleglosc(jednostka, sojusznik) <= 3.0
                    && sojusznik.getHp() < sojusznik.getMaxHp()) {
                sojusznik.ulecz(jednostka.getMocLeczenia());
                jednostka.zdobadzXp(2);
            }
        }
        jednostka.ustawCooldownAtaku(30);
    }

    // =========================================================================
    // Wrogowie
    // =========================================================================

    /**
     * Wróg idzie do najbliższej jednostki gracza i atakuje.
     * Używa Pathfinder – omija ściany.
     */
    private void tickWraga(Unit jednostka, double dt) {
        Unit cel = znajdzNajblizszego(jednostka, stan.getPlayerUnits(), 9999);
        if (cel == null) {
            if (!jednostka.maSciezke()) {
                idźWzdluzSciezki(jednostka, stan.getMap().fortressX, stan.getMap().fortressY, dt);
            } else {
                if (jednostka.krokWzdluzSciezki(dt)) jednostka.ustawSciezke(null);
                jednostka.ustawStan(Unit.Stan.IDZIE);
            }
            return;
        }

        if (odleglosc(jednostka, cel) <= jednostka.getKlasa().attackRange) {
            atakuj(jednostka, cel);
        } else {
            idźWzdluzSciezki(jednostka, (int)cel.getPozycjaX(), (int)cel.getPozycjaY(), dt);
        }
    }

    // =========================================================================
    // Ruch z Pathfinder
    // =========================================================================

    /**
     * Wyznacza ścieżkę do celu jeśli jej nie ma lub cel się zmienił,
     * a następnie wykonuje jeden krok wzdłuż tej ścieżki.
     *
     * Pathfinder respektuje ściany – jednostki nie przechodzą przez kafelki
     * z passable=false.
     *
     * @param jednostka  poruszana jednostka
     * @param celX       docelowy kafelek X
     * @param celY       docelowy kafelek Y
     * @param dt         delta czasu w sekundach
     */
    private void idźWzdluzSciezki(Unit jednostka, int celX, int celY, double dt) {
        // Czy potrzebujemy nowej ścieżki?
        if (!jednostka.maSciezke()) {
            List<int[]> sciezka = Pathfinder.find(
                stan.getMap(),
                jednostka.getKafelekX(), jednostka.getKafelekY(),
                celX, celY,
                true  // ignoreFinalPassable – możemy stanąć obok nieprzejezdnego kafelka
            );
            jednostka.ustawSciezke(sciezka);
        }

        if (jednostka.maSciezke()) {
            jednostka.krokWzdluzSciezki(dt);
            jednostka.ustawStan(Unit.Stan.IDZIE);
        }
    }

    // =========================================================================
    // Walka
    // =========================================================================

    private void atakuj(Unit atakujacy, Unit cel) {
        if (atakujacy.getCooldownAtaku() > 0) return;
        int obrazenia = atakujacy.obliczObrazenia(cel);
        cel.przyjmijObrazenia(obrazenia);
        atakujacy.ustawCooldownAtaku(GameConfig.get().attackCooldown());
        atakujacy.zdobadzXp(xpZaAtak);
        atakujacy.ustawStan(Unit.Stan.ATAKUJE);

        if (!cel.zyje()) {
            stan.log(atakujacy.getImie() + " pokonał " + cel.getImie() + "!");
            atakujacy.zdobadzXp(xpZaZabicie);
        }
    }

    // =========================================================================
    // Pomocnicze
    // =========================================================================

    private Unit znajdzNajblizszegoWraga(Unit od, double zasieg) {
        return znajdzNajblizszego(od, stan.getEnemyUnits(), zasieg);
    }

    private Unit znajdzNajblizszego(Unit od, List<Unit> kandydaci, double maksymalnyZasieg) {
        Unit najblizszy = null;
        double najblizszaOdleglosc = maksymalnyZasieg;
        for (Unit kandydat : kandydaci) {
            if (!kandydat.zyje()) continue;
            double odl = odleglosc(od, kandydat);
            if (odl < najblizszaOdleglosc) {
                najblizszaOdleglosc = odl;
                najblizszy = kandydat;
            }
        }
        return najblizszy;
    }

    /** Odległość między dwiema jednostkami w kafelkach. */
    private double odleglosc(Unit a, Unit b) {
        return Math.hypot(a.getPozycjaX() - b.getPozycjaX(),
                          a.getPozycjaY() - b.getPozycjaY());
    }

    /**
     * Sprawdza czy jednostka stoi bezpośrednio obok kafelka (x, y) –
     * na sąsiednim polu w czterech kierunkach lub po skosie.
     */
    private boolean stoisObok(Unit jednostka, int x, int y) {
        return Math.abs(jednostka.getKafelekX() - x) <= 1
            && Math.abs(jednostka.getKafelekY() - y) <= 1;
    }
}
