import java.awt.Color;
import java.util.*;

public class SilnikGry {
    private static final Color[] KOLORY_AI = {
        new Color(200, 60, 60),
        new Color(200, 140, 0),
        new Color(140, 0, 200),
        new Color(0, 180, 160),
    };

    private final Mapa mapa;
    private final Gracz graczLudzki;
    private final List<Gracz> graczeAI;
    private final List<Gracz> wszyscy;

    private final int limitTur;
    private int numerTury = 1;
    private boolean turaNalezyDoGracza = true;
    private String komunikat = "Tura 1 – Twoja kolej!";

    // Zaznaczenie
    private Pole zaznaczonePole;
    private JednostkaNaMapie zaznaczonaJednostka;
    private Miasto zaznaczoneMiasto;

    // Stan budowania budynku: jeśli != null, gracz wybrał budynek i klika lokalizację
    private Budynek budynekDoPostawienia = null;
    private Miasto miastoKtoreBuduje    = null;
    private List<Pole> podswietlonePola = new ArrayList<>();

    public enum StanGry { TRWA, WYGRANA_GRACZA, PRZEGRANA, REMIS }
    private StanGry stanGry = StanGry.TRWA;

    private int licznikMiastGracza = 1;
    private final int[] licznikMiastAI;
    private final Random rng = new Random();

    public SilnikGry(UstawieniaGry ust) {
        mapa = new Mapa(ust, System.currentTimeMillis());
        limitTur = ust.liczbaTur;
        graczLudzki = new Gracz("Gracz", new Color(50, 120, 220), false);
        graczeAI = new ArrayList<>();
        for (int i = 0; i < ust.liczbaAI; i++)
            graczeAI.add(new Gracz("AI " + (i + 1), KOLORY_AI[i % KOLORY_AI.length], true));
        wszyscy = new ArrayList<>();
        wszyscy.add(graczLudzki);
        wszyscy.addAll(graczeAI);
        licznikMiastAI = new int[ust.liczbaAI];
        Arrays.fill(licznikMiastAI, 1);
        rozmieszczGraczy();
    }

    private void rozmieszczGraczy() {
        for (int i = 0; i < wszyscy.size(); i++) {
            Gracz gracz = wszyscy.get(i);
            Pole start = znajdzLosowyWolnyStart();
            if (start == null) continue;
            String nazwa = gracz.isCzyAI()
                ? "Miasto " + licznikMiastAI[graczeAI.indexOf(gracz)]++
                : "Miasto " + licznikMiastGracza++;
            zalozMiasto(start, gracz, nazwa);
            Pole obokMiasta = znajdzWolnePoleObok(start.getRow(), start.getCol());
            if (obokMiasta != null) utworzJednostke(obokMiasta, gracz);
        }
    }

    private Pole znajdzLosowyWolnyStart() {
        for (int proba = 0; proba < 500; proba++) {
            int row = rng.nextInt(mapa.getLiczbaWierszy());
            int col = rng.nextInt(mapa.getLiczbaKolumn());
            Pole p = mapa.getPole(row, col);
            if (p == null || !p.czyPrzejezdne() || p.getBudynek() != null) continue;
            if (!mapa.czyRegionMaMiasto(p.getNumerRegionu())) return p;
        }
        for (int row = 0; row < mapa.getLiczbaWierszy(); row++)
            for (int col = 0; col < mapa.getLiczbaKolumn(); col++) {
                Pole p = mapa.getPole(row, col);
                if (p != null && p.czyPrzejezdne() && p.getBudynek() == null
                        && !mapa.czyRegionMaMiasto(p.getNumerRegionu())) return p;
            }
        return null;
    }

    // -------------------------------------------------------------------------
    // Kliknięcie
    // -------------------------------------------------------------------------

    public void kliknijPole(int row, int col) {
        if (!turaNalezyDoGracza || stanGry != StanGry.TRWA) return;
        Pole klikniete = mapa.getPole(row, col);
        if (klikniete == null) return;

        // Tryb budowania: gracz wybrał budynek, teraz klika lokalizację
        if (budynekDoPostawienia != null) {
            if (podswietlonePola.contains(klikniete)) {
                postawBudynek(klikniete, budynekDoPostawienia, miastoKtoreBuduje);
            } else {
                komunikat = "Wybierz podświetlone pole.";
            }
            anulujBudowanie();
            return;
        }

        // Tryb normalny: zaznaczenie lub akcja jednostki
        if (zaznaczonaJednostka != null) {
            JednostkaNaMapie celWrogi = klikniete.getJednostka();
            if (celWrogi != null && celWrogi.getWlasciciel() != graczLudzki) {
                akcjaAtak(zaznaczonaJednostka, celWrogi);
            } else {
                akcjaRuch(zaznaczonaJednostka, klikniete);
            }
            wyczyscZaznaczenie();
            sprawdzZwyciestwo();
            return;
        }

        wyczyscZaznaczenie();
        if (klikniete.getJednostka() != null
                && klikniete.getJednostka().getWlasciciel() == graczLudzki) {
            zaznaczonaJednostka = klikniete.getJednostka();
            zaznaczonePole = klikniete;
            JednostkaNaMapie j = zaznaczonaJednostka;
            komunikat = "Jednostka: HP " + j.getPunktyZycia() + "/" + j.getMaksymalnePunktyZycia()
                      + "  ATK " + j.getObrazenia() + "  RUCH " + j.getPozostalyruch();
        } else if (klikniete.getBudynek() != null
                && klikniete.getBudynek().getWlasciciel() == graczLudzki) {
            zaznaczoneMiasto = klikniete.getBudynek().getMiasto();
            zaznaczonePole = klikniete;
            komunikat = "Miasto: " + zaznaczoneMiasto.getNazwa();
        } else {
            komunikat = klikniete.getTeren().nazwa;
        }
    }

    // -------------------------------------------------------------------------
    // Ruch i atak
    // -------------------------------------------------------------------------

    private void akcjaRuch(JednostkaNaMapie jednostka, Pole cel) {
        if (!cel.czyPrzejezdne())       { komunikat = "Nieprzejezdny teren."; return; }
        if (cel.getJednostka() != null) { komunikat = "Pole zajęte."; return; }

        List<Pole> sciezka = znajdzSciezke(jednostka.getRow(), jednostka.getCol(),
                                            cel.getRow(), cel.getCol());
        if (sciezka == null) { komunikat = "Brak ścieżki."; return; }

        for (Pole nastepne : sciezka) {
            if (jednostka.getPozostalyruch() < nastepne.getTeren().kosztRuchu) break;
            Pole poprzednie = mapa.getPole(jednostka.getRow(), jednostka.getCol());
            jednostka.przesun(nastepne.getRow(), nastepne.getCol(), nastepne.getTeren().kosztRuchu);
            poprzednie.setJednostka(null);
            nastepne.setJednostka(jednostka);
            // Przejęcie: wejście na pole z budynkiem wroga
            if (nastepne.getBudynek() != null
                    && nastepne.getBudynek().getWlasciciel() != graczLudzki)
                przejmijBudynek(nastepne, graczLudzki);
        }
        komunikat = "Ruch. Pozostały ruch: " + jednostka.getPozostalyruch();
    }

    private void akcjaAtak(JednostkaNaMapie atakujacy, JednostkaNaMapie obronca) {
        if (!sasiaduja4(atakujacy.getRow(), atakujacy.getCol(),
                        obronca.getRow(), obronca.getCol())) {
            komunikat = "Cel zbyt daleko."; return;
        }
        Gracz wlascicielObroncy = obronca.getWlasciciel();
        boolean ginie = atakujacy.atakuj(obronca);
        komunikat = "Atak! Wróg ma " + obronca.getPunktyZycia() + " HP.";
        if (ginie)             { usunJednostke(obronca, wlascicielObroncy); komunikat = "Wróg pokonany!"; }
        if (!atakujacy.czyZyje()) { usunJednostke(atakujacy, graczLudzki); komunikat += " Twoja jednostka zginęła!"; }
    }

    // -------------------------------------------------------------------------
    // Budowanie miasta (Town Hall przez jednostkę)
    // -------------------------------------------------------------------------

    public void budujMiastoZaznaczonegoGracza() {
        if (zaznaczonaJednostka == null) { komunikat = "Zaznacz jednostkę."; return; }
        Pole pole = mapa.getPole(zaznaczonaJednostka.getRow(), zaznaczonaJednostka.getCol());
        if (pole.getBudynek() != null)                         { komunikat = "Tu już stoi budynek."; return; }
        if (mapa.czyRegionMaMiasto(pole.getNumerRegionu()))    { komunikat = "Region już ma miasto."; return; }
        int koszt = graczLudzki.getKosztBudowyMiasta();
        if (graczLudzki.getZloto() < koszt)                    { komunikat = "Za mało złota (" + koszt + ")."; return; }
        graczLudzki.odejmijZloto(koszt);
        zalozMiasto(pole, graczLudzki, "Miasto " + licznikMiastGracza++);
        komunikat = "Zbudowano miasto!";
        wyczyscZaznaczenie();
    }

    // -------------------------------------------------------------------------
    // Budowanie budynku w mieście
    // -------------------------------------------------------------------------

    /**
     * Rozpoczyna tryb budowania: zapamiętuje wybrany budynek i miasto,
     * oblicza i ustawia listę podswietlonePola.
     */
    public void rozpocznijBudowanie(Budynek budynek, Miasto miasto) {
        if (!graczLudzki.moznaWydacPopulacje(budynek.kosztWPopulacji)) {
            komunikat = "Za mało wolnej populacji."; return;
        }
        if (graczLudzki.getZloto() < budynek.kosztWZlocie) {
            komunikat = "Za mało złota (" + budynek.kosztWZlocie + ")."; return;
        }
        budynekDoPostawienia = budynek;
        miastoKtoreBuduje = miasto;
        podswietlonePola = obliczDostepnePola(miasto);
        if (podswietlonePola.isEmpty()) {
            komunikat = "Brak miejsca wokół miasta.";
            anulujBudowanie();
            return;
        }
        komunikat = "Kliknij podświetlone pole aby postawić " + budynek.nazwa + ".";
    }

    private void postawBudynek(Pole pole, Budynek budynek, Miasto miasto) {
        graczLudzki.odejmijZloto(budynek.kosztWZlocie);
        graczLudzki.wydajPopulacje(budynek.kosztWPopulacji);
        BudynekWMiescie nowy = new BudynekWMiescie(budynek, pole.getRow(), pole.getCol(),
                                                    graczLudzki, miasto);
        pole.setBudynek(nowy);
        miasto.dodajPole(pole);
        komunikat = "Postawiono " + budynek.nazwa + "!";
    }

    /** Pola sąsiadujące z dowolnym polem miasta, wolne od budynków i jednostek. */
    private List<Pole> obliczDostepnePola(Miasto miasto) {
        Set<Pole> dostepne = new LinkedHashSet<>();
        int[][] kierunki = {{-1,0},{1,0},{0,-1},{0,1}};
        for (Pole poleMiasta : miasto.getPola()) {
            for (int[] k : kierunki) {
                Pole kandydat = mapa.getPole(poleMiasta.getRow() + k[0], poleMiasta.getCol() + k[1]);
                if (kandydat != null && kandydat.czyPrzejezdne()
                        && kandydat.getBudynek() == null
                        && kandydat.getJednostka() == null)
                    dostepne.add(kandydat);
            }
        }
        return new ArrayList<>(dostepne);
    }

    private void anulujBudowanie() {
        budynekDoPostawienia = null;
        miastoKtoreBuduje = null;
        podswietlonePola = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Szkolenie jednostki
    // -------------------------------------------------------------------------

    public void szkolJednostkeWZaznaczonymMiescie() {
        if (zaznaczoneMiasto == null) { komunikat = "Zaznacz miasto."; return; }
        if (graczLudzki.getZloto() < Miasto.KOSZT_JEDNOSTKI) {
            komunikat = "Za mało złota (" + Miasto.KOSZT_JEDNOSTKI + ")."; return;
        }
        // Szukaj wolnego pola przy dowolnym budynku miasta
        Pole wolne = null;
        for (Pole p : zaznaczoneMiasto.getPola()) {
            wolne = znajdzWolnePoleObok(p.getRow(), p.getCol());
            if (wolne != null) break;
        }
        if (wolne == null) { komunikat = "Brak miejsca wokół miasta."; return; }
        graczLudzki.odejmijZloto(Miasto.KOSZT_JEDNOSTKI);
        utworzJednostke(wolne, graczLudzki);
        komunikat = "Wyszkolono jednostkę!";
    }

    public void szkolJednostkeWZaznaczonymMiescie(Jednostka jednostka) {
        if (zaznaczoneMiasto == null) { komunikat = "Zaznacz miasto."; return; }
        if (graczLudzki.getZloto() < jednostka.kosztWZlocie){
            komunikat = "Za mało złota (" + Miasto.KOSZT_JEDNOSTKI + ")."; return;
        }
        if (graczLudzki.getWolnaPopulacja() < jednostka.kosztWPopulacji){
            komunikat = "Za mało ludzi (" + Miasto.KOSZT_JEDNOSTKI + ")."; return;
        }
        // Szukaj wolnego pola przy dowolnym budynku miasta
        Pole wolne = null;
        for (Pole p : zaznaczoneMiasto.getPola()) {
            wolne = znajdzWolnePoleObok(p.getRow(), p.getCol());
            if (wolne != null) break;
        }
        if (wolne == null) { komunikat = "Brak miejsca wokół miasta."; return; }
        graczLudzki.odejmijZloto(Miasto.KOSZT_JEDNOSTKI);
//        graczLudzki.o
        utworzJednostke(wolne, graczLudzki, jednostka);
        komunikat = "Wyszkolono jednostkę!";
    }

    // -------------------------------------------------------------------------
    // Ulepszenia jednostki
    // -------------------------------------------------------------------------

    public void ulepszAtakZaznaczonegoGracza() {
        if (!moznaUlepszac()) return;
        if (graczLudzki.getZloto() < JednostkaNaMapie.KOSZT_ULEPSZENIA) { komunikat = "Za mało złota."; return; }
        if (!zaznaczonaJednostka.moznaUlepszycAtak())             { komunikat = "Max poziom."; return; }
        graczLudzki.odejmijZloto(JednostkaNaMapie.KOSZT_ULEPSZENIA);
        zaznaczonaJednostka.ulepszAtak();
        komunikat = "Ulepszono atak!";
    }

    public void ulepszZycieZaznaczonegoGracza() {
        if (!moznaUlepszac()) return;
        if (graczLudzki.getZloto() < JednostkaNaMapie.KOSZT_ULEPSZENIA) { komunikat = "Za mało złota."; return; }
        if (!zaznaczonaJednostka.moznaUlepszycZycie())            { komunikat = "Max poziom."; return; }
        graczLudzki.odejmijZloto(JednostkaNaMapie.KOSZT_ULEPSZENIA);
        zaznaczonaJednostka.ulepszZycie();
        komunikat = "Ulepszono HP!";
    }

    public void ulepszRuchZaznaczonegoGracza() {
        if (!moznaUlepszac()) return;
        if (graczLudzki.getZloto() < JednostkaNaMapie.KOSZT_ULEPSZENIA) { komunikat = "Za mało złota."; return; }
        if (!zaznaczonaJednostka.moznaUlepszycRuch())             { komunikat = "Max poziom."; return; }
        graczLudzki.odejmijZloto(JednostkaNaMapie.KOSZT_ULEPSZENIA);
        zaznaczonaJednostka.ulepszRuch();
        komunikat = "Ulepszono ruch!";
    }

    private boolean moznaUlepszac() {
        if (zaznaczonaJednostka == null) { komunikat = "Zaznacz jednostkę."; return false; }
        Pole pole = mapa.getPole(zaznaczonaJednostka.getRow(), zaznaczonaJednostka.getCol());
        if (pole.getBudynek() == null || pole.getBudynek().getWlasciciel() != graczLudzki) {
            komunikat = "Jednostka musi stać w swoim mieście."; return false;
        }
        return true;
    }

    public boolean czyZaznaczonaJednostkaWMiescie() {
        if (zaznaczonaJednostka == null) return false;
        Pole pole = mapa.getPole(zaznaczonaJednostka.getRow(), zaznaczonaJednostka.getCol());
        return pole.getBudynek() != null && pole.getBudynek().getWlasciciel() == graczLudzki;
    }

    // -------------------------------------------------------------------------
    // Tura
    // -------------------------------------------------------------------------

    public void zakonczTure() {
        if (!turaNalezyDoGracza || stanGry != StanGry.TRWA) return;
        wyczyscZaznaczenie();
        anulujBudowanie();

        graczLudzki.zbierzZasobyZMiast();

        turaNalezyDoGracza = false;
        for (int i = 0; i < graczeAI.size(); i++) {
            Gracz ai = graczeAI.get(i);
            if (!ai.getMiasta().isEmpty() || !ai.getJednostki().isEmpty())
                wykonajTureAI(ai, i);
            ai.zbierzZasobyZMiast();
        }

        for (Gracz g : wszyscy)
            for (JednostkaNaMapie j : g.getJednostki()) j.rozpocznijNowaTure();

        numerTury++;
        turaNalezyDoGracza = true;
        komunikat = "Tura " + numerTury + " – Twoja kolej!";

        if (numerTury > limitTur) rozstrzygnijLimitTur();
        else sprawdzZwyciestwo();
    }

    // -------------------------------------------------------------------------
    // AI
    // -------------------------------------------------------------------------

    private void wykonajTureAI(Gracz ai, int indeks) {
        List<JednostkaNaMapie> jednostki = new ArrayList<>(ai.getJednostki());
        for (JednostkaNaMapie j : jednostki) {
            if (!j.czyZyje()) continue;
            JednostkaNaMapie cel = znajdzNajblizszaWrogazednostke(j, ai);
            if (cel != null) {
                if (sasiaduja4(j.getRow(), j.getCol(), cel.getRow(), cel.getCol())) {
                    Gracz wlCelu = cel.getWlasciciel();
                    if (j.atakuj(cel)) usunJednostke(cel, wlCelu);
                    if (!j.czyZyje()) { usunJednostke(j, ai); continue; }
                } else {
                    poruszAIKuCelowi(j, cel.getRow(), cel.getCol(), ai);
                }
            }
        }
        for (Miasto m : ai.getMiasta()) {
            if (ai.getZloto() >= Miasto.KOSZT_JEDNOSTKI
                    && ai.getJednostki().size() < ai.getMiasta().size() * 3) {
                for (Pole p : m.getPola()) {
                    Pole wolne = znajdzWolnePoleObok(p.getRow(), p.getCol());
                    if (wolne != null) {
                        ai.odejmijZloto(Miasto.KOSZT_JEDNOSTKI);
                        utworzJednostke(wolne, ai);
                        break;
                    }
                }
            }
        }
        if (numerTury % 5 == 0 && !ai.getJednostki().isEmpty()) {
            int koszt = ai.getKosztBudowyMiasta();
            if (ai.getZloto() >= koszt) {
                JednostkaNaMapie pionier = ai.getJednostki().get(0);
                Pole pole = mapa.getPole(pionier.getRow(), pionier.getCol());
                if (pole.getBudynek() == null && !mapa.czyRegionMaMiasto(pole.getNumerRegionu())) {
                    ai.odejmijZloto(koszt);
                    zalozMiasto(pole, ai, "Miasto " + licznikMiastAI[indeks]++);
                }
            }
        }
    }

    private void poruszAIKuCelowi(JednostkaNaMapie j, int targetRow, int targetCol, Gracz ai) {
        int dr = Integer.compare(targetRow, j.getRow());
        int dc = Integer.compare(targetCol, j.getCol());
        int[][] kierunki = {{dr,0},{0,dc},{dr,dc},{-dr,0},{0,-dc}};
        for (int[] k : kierunki) {
            if (k[0] == 0 && k[1] == 0) continue;
            int nr = j.getRow() + k[0], nc = j.getCol() + k[1];
            Pole cel = mapa.getPole(nr, nc);
            if (cel != null && cel.czyPrzejezdne() && cel.getJednostka() == null) {
                Pole poprz = mapa.getPole(j.getRow(), j.getCol());
                if (j.przesun(nr, nc, cel.getTeren().kosztRuchu)) {
                    poprz.setJednostka(null);
                    cel.setJednostka(j);
                    if (cel.getBudynek() != null && cel.getBudynek().getWlasciciel() != ai)
                        przejmijBudynek(cel, ai);
                }
                break;
            }
        }
    }

    private JednostkaNaMapie znajdzNajblizszaWrogazednostke(JednostkaNaMapie zrodlo, Gracz wlasciciel) {
        JednostkaNaMapie najblizszy = null;
        int minDist = Integer.MAX_VALUE;
        for (Gracz g : wszyscy) {
            if (g == wlasciciel) continue;
            for (JednostkaNaMapie cel : g.getJednostki()) {
                int d = Math.abs(cel.getRow() - zrodlo.getRow())
                      + Math.abs(cel.getCol() - zrodlo.getCol());
                if (d < minDist) { minDist = d; najblizszy = cel; }
            }
        }
        return najblizszy;
    }

    // -------------------------------------------------------------------------
    // A*
    // -------------------------------------------------------------------------

    private List<Pole> znajdzSciezke(int startRow, int startCol, int celRow, int celCol) {
        int rows = mapa.getLiczbaWierszy(), cols = mapa.getLiczbaKolumn();
        int[][] g = new int[rows][cols];
        int[][] rodzicRow = new int[rows][cols];
        int[][] rodzicCol = new int[rows][cols];
        for (int[] r : g) Arrays.fill(r, Integer.MAX_VALUE);
        for (int[] r : rodzicRow) Arrays.fill(r, -1);
        g[startRow][startCol] = 0;
        PriorityQueue<int[]> otwarte = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        otwarte.add(new int[]{heurystyka(startRow, startCol, celRow, celCol), startRow, startCol});
        int[][] kierunki = {{-1,0},{1,0},{0,-1},{0,1}};
        while (!otwarte.isEmpty()) {
            int[] biezacy = otwarte.poll();
            int row = biezacy[1], col = biezacy[2];
            if (row == celRow && col == celCol) {
                List<Pole> sciezka = new ArrayList<>();
                int r = celRow, c = celCol;
                while (r != startRow || c != startCol) {
                    sciezka.add(0, mapa.getPole(r, c));
                    int pr = rodzicRow[r][c], pc = rodzicCol[r][c];
                    r = pr; c = pc;
                }
                return sciezka;
            }
            for (int[] k : kierunki) {
                int nr = row + k[0], nc = col + k[1];
                Pole sasiad = mapa.getPole(nr, nc);
                if (sasiad == null || !sasiad.czyPrzejezdne()) continue;
                if (sasiad.getJednostka() != null && (nr != celRow || nc != celCol)) continue;
                int nowyG = g[row][col] + sasiad.getTeren().kosztRuchu;
                if (nowyG < g[nr][nc]) {
                    g[nr][nc] = nowyG;
                    rodzicRow[nr][nc] = row;
                    rodzicCol[nr][nc] = col;
                    otwarte.add(new int[]{nowyG + heurystyka(nr, nc, celRow, celCol), nr, nc});
                }
            }
        }
        return null;
    }

    private int heurystyka(int row, int col, int celRow, int celCol) {
        return Math.abs(celRow - row) + Math.abs(celCol - col);
    }

    // -------------------------------------------------------------------------
    // Warunki zwycięstwa
    // -------------------------------------------------------------------------

    private void sprawdzZwyciestwo() {
        boolean wszyscyAIMartiwi = graczeAI.stream()
            .allMatch(ai -> ai.getJednostki().isEmpty() && ai.getMiasta().isEmpty());
        if (wszyscyAIMartiwi) { stanGry = StanGry.WYGRANA_GRACZA; return; }
        if (graczLudzki.getJednostki().isEmpty() && graczLudzki.getMiasta().isEmpty())
            stanGry = StanGry.PRZEGRANA;
    }

    private void rozstrzygnijLimitTur() {
        int mg = graczLudzki.getMiasta().size();
        int maxAI = graczeAI.stream().mapToInt(ai -> ai.getMiasta().size()).max().orElse(0);
        if (mg > maxAI) stanGry = StanGry.WYGRANA_GRACZA;
        else if (mg < maxAI) stanGry = StanGry.PRZEGRANA;
        else stanGry = StanGry.REMIS;
    }

    // -------------------------------------------------------------------------
    // Pomocnicze
    // -------------------------------------------------------------------------

    /** Tworzy nowe miasto z Town Hallem na danym polu. */
    private void zalozMiasto(Pole pole, Gracz wlasciciel, String nazwa) {
        Miasto miasto = new Miasto(nazwa);
        BudynekWMiescie townHall = new BudynekWMiescie(Budynek.TOWNHALL, pole.getRow(),
                                                        pole.getCol(), wlasciciel, miasto);
        pole.setBudynek(townHall);
        miasto.dodajPole(pole);
        wlasciciel.dodajMiasto(miasto);
    }

    /** Przejmuje jeden budynek (zmienia właściciela). Jeśli to ostatni budynek miasta, usuwa całe miasto. */
    private void przejmijBudynek(Pole pole, Gracz nowyWlasciciel) {
        BudynekWMiescie budynek = pole.getBudynek();
        Gracz staryWlasciciel = budynek.getWlasciciel();
        Miasto miasto = budynek.getMiasto();

        // Sprawdź czy całe miasto należy do starego właściciela – jeśli tak, przejmij je całe
        boolean caleMiastoWroga = miasto.getPola().stream()
            .allMatch(p -> p.getBudynek() == null || p.getBudynek().getWlasciciel() == staryWlasciciel);

        if (caleMiastoWroga) {
            staryWlasciciel.usunMiasto(miasto);
            nowyWlasciciel.dodajMiasto(miasto);
            for (Pole p : miasto.getPola()) {
                if (p.getBudynek() != null) {
                    BudynekWMiescie stary = p.getBudynek();
                    p.setBudynek(new BudynekWMiescie(stary.getBudynek(), p.getRow(), p.getCol(),
                                                     nowyWlasciciel, miasto));
                }
            }
            komunikat = "Przejęto miasto " + miasto.getNazwa() + "!";
        }
    }

    private void utworzJednostke(Pole pole, Gracz wlasciciel) {
        JednostkaNaMapie j = new JednostkaNaMapie(pole.getRow(), pole.getCol(), wlasciciel);
        pole.setJednostka(j);
        wlasciciel.dodajJednostke(j);
    }

    private void utworzJednostke(Pole pole, Gracz wlasciciel, Jednostka jednostka) {
        JednostkaNaMapie j = new JednostkaNaMapie(pole.getRow(), pole.getCol(), wlasciciel, jednostka);
        pole.setJednostka(j);
        wlasciciel.dodajJednostke(j);
    }

    private void usunJednostke(JednostkaNaMapie j, Gracz wlasciciel) {
        Pole pole = mapa.getPole(j.getRow(), j.getCol());
        if (pole != null) pole.setJednostka(null);
        wlasciciel.usunJednostke(j);
    }

    private Pole znajdzWolnePoleObok(int row, int col) {
        int[][] kierunki = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] k : kierunki) {
            Pole p = mapa.getPole(row + k[0], col + k[1]);
            if (p != null && p.czyPrzejezdne() && p.getJednostka() == null
                    && p.getBudynek() == null) return p;
        }
        return null;
    }

    private boolean sasiaduja4(int r1, int c1, int r2, int c2) {
        return Math.abs(r1 - r2) + Math.abs(c1 - c2) == 1;
    }

    private void wyczyscZaznaczenie() {
        zaznaczonePole = null;
        zaznaczonaJednostka = null;
        zaznaczoneMiasto = null;
    }

    public Mapa getMapa()                       { return mapa; }
    public Gracz getGraczLudzki()               { return graczLudzki; }
    public List<Gracz> getGraczeAI()            { return graczeAI; }
    public List<Gracz> getWszyscy()             { return wszyscy; }
    public int getNumerTury()                   { return numerTury; }
    public int getLimitTur()                    { return limitTur; }
    public String getKomunikat()                { return komunikat; }
    public StanGry getStanGry()                 { return stanGry; }
    public boolean isTuraNalezyDoGracza()       { return turaNalezyDoGracza; }
    public Pole getZaznaczonePole()             { return zaznaczonePole; }
    public JednostkaNaMapie getZaznaczonaJednostka()   { return zaznaczonaJednostka; }
    public Miasto getZaznaczoneMiasto()         { return zaznaczoneMiasto; }
    public List<Pole> getPodswietlonePola()     { return podswietlonePola; }
    public boolean czyTrybBudowania()           { return budynekDoPostawienia != null; }
}
