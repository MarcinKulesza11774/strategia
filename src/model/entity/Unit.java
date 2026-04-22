package model.entity;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * Jednostka na mapie świata.
 *
 * Każda jednostka ma imię, klasę i cztery statystyki:
 *   VIT (wytrzymałość) – maksymalne HP = vit * 3
 *   STR (siła)         – szybkość budowania i wydobywania
 *   PRE (precyzja)     – skuteczność w produkcji i jako łucznik
 *   CHA (charyzma)     – skuteczność handlu i leczenia
 *
 * Jednostka działa według rozkazu wydanego przez gracza (pole rozkaz).
 * Jeśli brak rozkazu – AI wybiera domyślne zachowanie klasy.
 *
 * Ścieżka ruchu jest przechowywana jako lista punktów wyliczona przez Pathfinder.
 * Jednostka idzie punkt po punkcie, nie teleportuje się przez ściany.
 */
public class Unit {

    private static int nastepneId = 1;

    /** Stan aktywności jednostki – wyświetlany w UI. */
    public enum Stan {
        STOI, IDZIE, ZBIERA, BUDUJE, ATAKUJE, PATROLUJE, LECZY, UCIEKA
    }

    /**
     * Rozkaz wydany przez gracza określa co jednostka ma robić.
     * BRAK oznacza że AI wybiera zachowanie samodzielnie.
     */
    public enum Rozkaz {
        BRAK,       // AI decyduje na podstawie klasy
        ZBIERAJ,    // idź do zaznaczonego obszaru i zbieraj zasoby
        BUDUJ,      // idź do zaznaczonego obszaru i buduj
        PATROLUJ,   // trzymaj się w zaznaczonym obszarze i walcz z wrogami
        EWAKUUJ,    // w razie zagrożenia uciekaj do zaznaczonej strefy
        STOJ        // stój w miejscu, nie rób nic
    }

    // Tożsamość
    private final int     id;
    private final String  imie;
    private String        klasaId;
    private final boolean nalezydoGracza;

    // Statystyki
    private int wytrzymalosc;
    private int sila;
    private int precyzja;
    private int charyzma;
    private int hp;
    private int xp;
    private int xpDoNastepnegoStatu; // XP akumulowane do wzrostu statystyk
    private boolean gotowaNaAwans = false;

    // Pozycja (w kafelkach, zmiennoprzecinkowa dla płynnego ruchu)
    private double pozycjaX, pozycjaY;

    // Ścieżka ruchu wyliczona przez Pathfinder
    private List<int[]> sciezka = new ArrayList<>();
    private int indeksSciezki = 0;

    // Rozkaz gracza
    private Rozkaz rozkaz = Rozkaz.BRAK;

    // Obszar rozkazu: prostokąt [x1,y1,x2,y2] w kafelkach
    // Używany przez: ZBIERAJ (tu szukaj zasobów), PATROLUJ (tu walcz),
    //                EWAKUUJ (tu uciekaj), BUDUJ (tu buduj)
    private int rozkazObszarX1 = -1, rozkazObszarY1 = -1;
    private int rozkazObszarX2 = -1, rozkazObszarY2 = -1;

    // Aktualny stan (do wyświetlania)
    private Stan stan = Stan.STOI;

    // Cel akcji (kafelek do zebrania lub zbudowania)
    private int celAkcjiX = -1, celAkcjiY = -1;
    private int postepAkcji = 0;

    // Cooldown między atakami
    private int cooldownAtaku = 0;

    // Buffy z pokoi – ustawiane przez GameState co klatkę
    private int buffSzybkosci = 0;
    private int buffAtaku = 0;
    private int buffObrony = 0;

    private static final String[] PRZEDROSTKI = {
        "Ka", "Be", "Mor", "Al", "Dun", "Tor", "Sel", "Bran", "Gal", "Fen",
        "Ra", "Och", "Ild", "Vor", "Nak"
    };
    private static final String[] PRZYROSTKI = {
        "dor", "ric", "ath", "en", "im", "ar", "is", "an", "or", "mir",
        "dan", "vil", "ret", "las", "mon"
    };
    private static final Random LOSOWNIK = new Random();

    public Unit(String klasaId, double pozycjaX, double pozycjaY, boolean nalezydoGracza) {
        this.id               = nastepneId++;
        this.imie             = losujImie();
        this.klasaId          = klasaId;
        this.pozycjaX         = pozycjaX;
        this.pozycjaY         = pozycjaY;
        this.nalezydoGracza   = nalezydoGracza;

        UnitClass klasa = getKlasa();
        this.wytrzymalosc = klasa.baseVit;
        this.sila         = klasa.baseStr;
        this.precyzja     = klasa.basePre;
        this.charyzma     = klasa.baseCha;
        this.hp           = getMaxHp();
    }

    // =========================================================================
    // Statystyki
    // =========================================================================

    public int    getMaxHp()         { return wytrzymalosc * 3; }
    public int    getSilAtaku()      { return sila + buffAtaku
                                        + (klasaId.equals(UnitClass.ARCHER) ? precyzja : 0); }
    public int    getObrona()        { return wytrzymalosc / 3 + buffObrony; }
    public double getSzybkosc()      { return getKlasa().speed * (1.0 + buffSzybkosci / 100.0)
                                        * (1.0 + sila * 0.005); }
    public int    getMocLeczenia()   { return Math.max(1, charyzma / 2); }

    // =========================================================================
    // Ruch wzdłuż ścieżki
    // =========================================================================

    /**
     * Ustawia nową ścieżkę ruchu wyliczoną przez Pathfinder.
     * Jednostka będzie szła punkt po punkcie nie przez ściany.
     *
     * @param nowaSciezka  lista punktów [x,y] od pierwszego kroku do celu
     */
    public void ustawSciezke(List<int[]> nowaSciezka) {
        this.sciezka       = nowaSciezka != null ? nowaSciezka : new ArrayList<>();
        this.indeksSciezki = 0;
    }

    /**
     * Przesuwa jednostkę o jeden krok wzdłuż ścieżki.
     * Zwraca true gdy jednostka dotarła do końca ścieżki.
     *
     * @param dt  czas od ostatniej klatki w sekundach
     */
    public boolean krokWzdluzSciezki(double dt) {
        if (sciezka.isEmpty() || indeksSciezki >= sciezka.size()) return true;

        int[] nastepnyPunkt = sciezka.get(indeksSciezki);
        double celX = nastepnyPunkt[0];
        double celY = nastepnyPunkt[1];

        double dx       = celX - pozycjaX;
        double dy       = celY - pozycjaY;
        double odleglosc = Math.hypot(dx, dy);
        double predkosc  = getSzybkosc() * dt;

        if (odleglosc <= predkosc) {
            pozycjaX = celX;
            pozycjaY = celY;
            indeksSciezki++;
            return indeksSciezki >= sciezka.size();
        }

        pozycjaX += dx / odleglosc * predkosc;
        pozycjaY += dy / odleglosc * predkosc;
        return false;
    }

    /** Czy jednostka aktualnie ma ścieżkę do przejścia. */
    public boolean maSciezke() {
        return !sciezka.isEmpty() && indeksSciezki < sciezka.size();
    }

    // =========================================================================
    // Walka
    // =========================================================================

    /**
     * Oblicza obrażenia zadane celowi na podstawie siły ataku i obrony celu.
     * Dodaje ±25% losowego wahania.
     */
    public int obliczObrazenia(Unit cel) {
        int baza     = Math.max(1, getSilAtaku() - cel.getObrona() / 2);
        int wahanie  = Math.max(1, (int)(baza * 0.25));
        return baza + (int)(Math.random() * wahanie);
    }

    /** Zadaje obrażenia. Zwraca true jeśli jednostka przeżyła. */
    public boolean przyjmijObrazenia(int obrazenia) {
        hp = Math.max(0, hp - obrazenia);
        return hp > 0;
    }

    public void ulecz(int ilosc) {
        hp = Math.min(getMaxHp(), hp + ilosc);
    }

    // =========================================================================
    // XP i awans
    // =========================================================================

    /**
     * Przyznaje punkty doświadczenia.
     * Co 50 XP losowo rośnie jedna statystyka odpowiednia do klasy.
     * Gdy XP przekroczy próg klasy – ustawia flagę gotowaNaAwans.
     */
    public void zdobadzXp(int ilosc) {
        xp                    += ilosc;
        xpDoNastepnegoStatu   += ilosc;

        while (xpDoNastepnegoStatu >= 50) {
            xpDoNastepnegoStatu -= 50;
            zwiekszStat();
        }

        UnitClass klasa = getKlasa();
        if (!gotowaNaAwans && xp >= klasa.xpToPromote
                && !klasa.promotionOptions.isEmpty()) {
            gotowaNaAwans = true;
        }
    }

    private void zwiekszStat() {
        UnitClass klasa = getKlasa();
        if (klasa.canFight)          { sila++; wytrzymalosc++; }
        else if (klasa.canBuild)     { sila++; precyzja++; }
        else if (klasa.canHarvest)   { sila++; }
        else if (klasa.canTrade)     { charyzma++; }
        else {
            switch (LOSOWNIK.nextInt(4)) {
                case 0 -> wytrzymalosc++;
                case 1 -> sila++;
                case 2 -> precyzja++;
                case 3 -> charyzma++;
            }
        }
        hp = Math.min(hp, getMaxHp());
    }

    /**
     * Awansuje jednostkę na wskazaną klasę docelową.
     * Statystyki są podnoszone do minimum nowej klasy jeśli są niższe.
     *
     * @param docelowaKlasaId  ID klasy docelowej z units.json
     */
    public void awansuj(String docelowaKlasaId) {
        if (!getKlasa().canPromoteTo(docelowaKlasaId)) return;

        int stosunekHp = hp * 100 / getMaxHp();
        klasaId       = docelowaKlasaId;
        gotowaNaAwans = false;
        xp            = 0;

        UnitClass nowaKlasa = getKlasa();
        wytrzymalosc = Math.max(wytrzymalosc, nowaKlasa.baseVit);
        sila         = Math.max(sila, nowaKlasa.baseStr);
        precyzja     = Math.max(precyzja, nowaKlasa.basePre);
        charyzma     = Math.max(charyzma, nowaKlasa.baseCha);

        hp = getMaxHp() * stosunekHp / 100;
    }

    // =========================================================================
    // Gettery
    // =========================================================================

    public int     getId()              { return id; }
    public String  getImie()            { return imie; }
    public String  getKlasaId()         { return klasaId; }
    public UnitClass getKlasa()         { return UnitClass.get(klasaId); }
    public double  getPozycjaX()        { return pozycjaX; }
    public double  getPozycjaY()        { return pozycjaY; }
    public int     getKafelekX()        { return (int) Math.round(pozycjaX); }
    public int     getKafelekY()        { return (int) Math.round(pozycjaY); }
    public int     getHp()              { return hp; }
    public int     getXp()              { return xp; }
    public int     getWytrzymalosc()    { return wytrzymalosc; }
    public int     getSila()            { return sila; }
    public int     getPrecyzja()        { return precyzja; }
    public int     getCharyzma()        { return charyzma; }
    public Stan    getStan()            { return stan; }
    public Rozkaz  getRozkaz()          { return rozkaz; }
    public boolean nalezydoGracza()     { return nalezydoGracza; }
    public boolean zyje()               { return hp > 0; }
    public boolean gotowaNaAwans()      { return gotowaNaAwans; }
    public int     xpDoAwansu()         { return getKlasa().xpToPromote; }

    public int getCelAkcjiX()           { return celAkcjiX; }
    public int getCelAkcjiY()           { return celAkcjiY; }
    public void ustawCelAkcji(int x, int y) { celAkcjiX = x; celAkcjiY = y; }
    public void wyczysCelAkcji()        { celAkcjiX = -1; celAkcjiY = -1; }
    public boolean maCelAkcji()         { return celAkcjiX >= 0; }

    public int  getPostepAkcji()        { return postepAkcji; }
    public void zwiekszPostepAkcji()    { postepAkcji++; }
    public void resetujPostepAkcji()    { postepAkcji = 0; }

    public int  getCooldownAtaku()      { return cooldownAtaku; }
    public void ustawCooldownAtaku(int v){ cooldownAtaku = v; }
    public void zmniejszCooldown()      { if (cooldownAtaku > 0) cooldownAtaku--; }

    public void ustawStan(Stan s)       { stan = s; }

    /**
     * Ustawia rozkaz gracza i obszar jego wykonania.
     *
     * @param rozkaz  co jednostka ma robić
     * @param x1,y1   lewy-górny róg obszaru rozkazu
     * @param x2,y2   prawy-dolny róg obszaru rozkazu (-1 jeśli brak obszaru)
     */
    public void ustawRozkaz(Rozkaz rozkaz, int x1, int y1, int x2, int y2) {
        this.rozkaz         = rozkaz;
        this.rozkazObszarX1 = x1;
        this.rozkazObszarY1 = y1;
        this.rozkazObszarX2 = x2;
        this.rozkazObszarY2 = y2;
        // Wyczyść aktualną ścieżkę i cel – nowy rozkaz wymaga nowego planowania
        ustawSciezke(null);
        wyczysCelAkcji();
        resetujPostepAkcji();
    }

    public void ustawRozkaz(Rozkaz rozkaz) {
        ustawRozkaz(rozkaz, -1, -1, -1, -1);
    }

    public int getRozkazObszarX1() { return rozkazObszarX1; }
    public int getRozkazObszarY1() { return rozkazObszarY1; }
    public int getRozkazObszarX2() { return rozkazObszarX2; }
    public int getRozkazObszarY2() { return rozkazObszarY2; }

    /** Czy podany kafelek mieści się w obszarze rozkazu. */
    public boolean kafelekWObszarzeRozkazu(int x, int y) {
        if (rozkazObszarX1 < 0) return false;
        return x >= rozkazObszarX1 && x <= rozkazObszarX2
            && y >= rozkazObszarY1 && y <= rozkazObszarY2;
    }

    public void ustawBuffy(int szybkosc, int atak, int obrona) {
        buffSzybkosci = szybkosc;
        buffAtaku     = atak;
        buffObrony    = obrona;
    }

    // =========================================================================
    // Pomocnicze – stare nazwy zachowane dla kompatybilności z fragmentami kodu
    // =========================================================================

    /** @deprecated Używaj getPozycjaX() */
    public double getX()       { return pozycjaX; }
    /** @deprecated Używaj getPozycjaY() */
    public double getY()       { return pozycjaY; }
    /** @deprecated Używaj getKafelekX() */
    public int getTileX()      { return getKafelekX(); }
    /** @deprecated Używaj getKafelekY() */
    public int getTileY()      { return getKafelekY(); }
    /** @deprecated Używaj getImie() */
    public String getName()    { return imie; }
    /** @deprecated Używaj getKlasa() */
    public UnitClass getUnitClass() { return getKlasa(); }
    /** @deprecated Używaj getKlasaId() */
    public String getClassId() { return klasaId; }
    /** @deprecated Używaj zyje() */
    public boolean isAlive()   { return zyje(); }
    /** @deprecated Używaj nalezydoGracza() */
    public boolean isPlayerOwned() { return nalezydoGracza; }
    /** @deprecated Używaj gotowaNaAwans() */
    public boolean isReadyToPromote() { return gotowaNaAwans; }
    /** @deprecated Używaj obliczObrazenia() */
    public int calculateDamage(Unit cel) { return obliczObrazenia(cel); }
    /** @deprecated Używaj przyjmijObrazenia() */
    public boolean takeDamage(int n)    { return przyjmijObrazenia(n); }
    /** @deprecated Używaj ulecz() */
    public void heal(int n)            { ulecz(n); }
    /** @deprecated Używaj zdobadzXp() */
    public void gainXp(int n)          { zdobadzXp(n); }
    /** @deprecated Używaj awansuj() */
    public void promoteTo(String id)   { awansuj(id); }
    /** @deprecated Używaj getStan() */
    public Stan getState()             { return stan; }
    /** @deprecated Używaj ustawStan() */
    public void setState(Stan s)       { stan = s; }
    /** @deprecated Używaj getWytrzymalosc() */
    public int getVit()  { return wytrzymalosc; }
    /** @deprecated Używaj getSila() */
    public int getStr()  { return sila; }
    /** @deprecated Używaj getPrecyzja() */
    public int getPre()  { return precyzja; }
    /** @deprecated Używaj getCharyzma() */
    public int getCha()  { return charyzma; }
    public int getAttack()  { return getSilAtaku(); }
    public int getDefense() { return getObrona(); }
    public int xpForNextTier() { return xpDoAwansu(); }
    public void setSpeedBuff(int v)  { buffSzybkosci = v; }
    public void setAttackBuff(int v) { buffAtaku = v; }
    public void setDefenseBuff(int v){ buffObrony = v; }
    public int getActionTargetX()    { return celAkcjiX; }
    public int getActionTargetY()    { return celAkcjiY; }
    public void setActionTarget(int x, int y) { ustawCelAkcji(x, y); }
    public int  getActionProgress()  { return postepAkcji; }
    public void tickActionProgress() { zwiekszPostepAkcji(); }
    public void resetActionProgress(){ resetujPostepAkcji(); }
    public int  getActionCooldown()  { return cooldownAtaku; }
    public void setActionCooldown(int v){ ustawCooldownAtaku(v); }
    public void tickCooldown()       { zmniejszCooldown(); }

    // =========================================================================
    // Prywatne
    // =========================================================================

    private static String losujImie() {
        return PRZEDROSTKI[LOSOWNIK.nextInt(PRZEDROSTKI.length)]
             + PRZYROSTKI[LOSOWNIK.nextInt(PRZYROSTKI.length)];
    }

    @Override
    public String toString() {
        return imie + "(" + getKlasa().label + ")";
    }
}
