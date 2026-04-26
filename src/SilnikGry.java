import java.awt.Color;
import java.util.*;
import java.util.PriorityQueue;

/**
 * Silnik gry – logika oddzielona od warstwy graficznej.
 */
public class SilnikGry {
    public static final int LIMIT_TUR = 400;

    private final Mapa mapa;
    private final Gracz graczLudzki;
    private final Gracz graczAI;

    private int numerTury = 1;
    private boolean turaNalezyDoGracza = true;
    private String komunikat = "Tura 1 – Twoja kolej!";

    private Pole zaznaczonePole;
    private Jednostka zaznaczonaJednostka;
    private Miasto zaznaczoneMiasto;

    public enum StanGry { TRWA, WYGRANA_GRACZA, WYGRANA_AI, REMIS }
    private StanGry stanGry = StanGry.TRWA;

    private int licznikMiastGracza = 1;
    private int licznikMiastAI = 1;

    public SilnikGry() {
        mapa = new Mapa(System.currentTimeMillis());
        graczLudzki = new Gracz("Gracz", new Color(50, 120, 220), false);
        graczAI     = new Gracz("AI",    new Color(200, 60,  60), true);
        ustawPoczatkoweStarty();
    }

    private void ustawPoczatkoweStarty() {
        Random rng = new Random();
        Pole startGracza = znajdzPrzejezdnePole(rng.nextInt(100), rng.nextInt(100));
        if (startGracza != null) {
            zalozMiastoNaPolu(startGracza, graczLudzki, "Miasto " + licznikMiastGracza++);
            Pole obokMiasta = znajdzWolnePoleObok(startGracza.getRow(), startGracza.getCol());
            if (obokMiasta != null) utworzJednostke(obokMiasta, graczLudzki);
        }
        Pole startAI = znajdzPrzejezdnePole(rng.nextInt(100), rng.nextInt(100));
        if (startAI != null) {
            zalozMiastoNaPolu(startAI, graczAI, "Miasto " + licznikMiastAI++);
            Pole obokMiastaAI = znajdzWolnePoleObok(startAI.getRow(), startAI.getCol());
            if (obokMiastaAI != null) utworzJednostke(obokMiastaAI, graczAI);
        }
    }

    // -------------------------------------------------------------------------
    // Kliknięcie na mapę
    // -------------------------------------------------------------------------

    public void kliknijPole(int row, int col) {
        if (!turaNalezyDoGracza || stanGry != StanGry.TRWA) return;
        Pole klikniete = mapa.getPole(row, col);
        if (klikniete == null) return;

        if (zaznaczonaJednostka != null) {
            Jednostka celWrogi = klikniete.getJednostka();
            if (celWrogi != null && celWrogi.getWlasciciel() == graczAI) {
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
            Jednostka j = zaznaczonaJednostka;
            komunikat = "Jednostka: HP " + j.getPunktyZycia() + "/" + j.getMaksymalnePunktyZycia()
                      + "  ATK " + j.getObrazenia() + "  RUCH " + j.getPozostalyruch();
        } else if (klikniete.getMiasto() != null
                && klikniete.getMiasto().getWlasciciel() == graczLudzki) {
            zaznaczoneMiasto = klikniete.getMiasto();
            zaznaczonePole = klikniete;
            komunikat = "Miasto: " + zaznaczoneMiasto.getNazwa()
                      + " poz." + zaznaczoneMiasto.getPoziom();
        } else {
            komunikat = klikniete.getTeren().nazwa;
        }
    }

    private void akcjaRuch(Jednostka jednostka, Pole cel) {
        if (!cel.czyPrzejezdne())       { komunikat = "Nieprzejezdny teren."; return; }
        if (cel.getJednostka() != null) { komunikat = "Pole zajęte."; return; }

        List<Pole> sciezka = znajdzSciezke(jednostka.getRow(), jednostka.getCol(),
                                            cel.getRow(), cel.getCol());
        if (sciezka == null) { komunikat = "Brak dostępnej ścieżki."; return; }

        // Idź wzdłuż ścieżki dopóki starczy punktów ruchu
        for (Pole nastepne : sciezka) {
            if (jednostka.getPozostalyruch() < nastepne.getTeren().kosztRuchu) break;
            Pole poprzednie = mapa.getPole(jednostka.getRow(), jednostka.getCol());
            jednostka.przesun(nastepne.getRow(), nastepne.getCol(), nastepne.getTeren().kosztRuchu);
            poprzednie.setJednostka(null);
            nastepne.setJednostka(jednostka);
            if (nastepne.getMiasto() != null && nastepne.getMiasto().getWlasciciel() == graczAI)
                przejmijMiasto(nastepne.getMiasto(), graczLudzki, graczAI);
        }
        komunikat = "Ruch. Pozostały ruch: " + jednostka.getPozostalyruch();
    }

    /**
     * A* – zwraca listę pól ścieżki od (startRow,startCol) do (celRow,celCol),
     * bez pola startowego, lub null jeśli brak ścieżki.
     * Koszt = suma kosztRuchu pól.
     */
    private List<Pole> znajdzSciezke(int startRow, int startCol, int celRow, int celCol) {
        // Węzeł A*
        int[][] g = new int[Mapa.ROWS][Mapa.COLS];       // koszt dotarcia
        int[][] rodzicRow = new int[Mapa.ROWS][Mapa.COLS];
        int[][] rodzicCol = new int[Mapa.ROWS][Mapa.COLS];
        for (int[] r : g) Arrays.fill(r, Integer.MAX_VALUE);
        for (int[] r : rodzicRow) Arrays.fill(r, -1);

        g[startRow][startCol] = 0;

        // PriorityQueue z tablicą: [f, row, col]
        PriorityQueue<int[]> otwarte = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        otwarte.add(new int[]{heurystyka(startRow, startCol, celRow, celCol), startRow, startCol});

        int[][] kierunki = {{-1,0},{1,0},{0,-1},{0,1}};

        while (!otwarte.isEmpty()) {
            int[] biezacy = otwarte.poll();
            int row = biezacy[1], col = biezacy[2];

            if (row == celRow && col == celCol) {
                // Odtwórz ścieżkę
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
                // Nie omijaj celu nawet jeśli jest tam jednostka
                if (sasiad.getJednostka() != null && (nr != celRow || nc != celCol)) continue;
                int nowyG = g[row][col] + sasiad.getTeren().kosztRuchu;
                if (nowyG < g[nr][nc]) {
                    g[nr][nc] = nowyG;
                    rodzicRow[nr][nc] = row;
                    rodzicCol[nr][nc] = col;
                    int f = nowyG + heurystyka(nr, nc, celRow, celCol);
                    otwarte.add(new int[]{f, nr, nc});
                }
            }
        }
        return null;
    }

    private int heurystyka(int row, int col, int celRow, int celCol) {
        return Math.abs(celRow - row) + Math.abs(celCol - col);
    }

    private void akcjaAtak(Jednostka atakujacy, Jednostka obronca) {
        if (!sasiaduja4(atakujacy.getRow(), atakujacy.getCol(),
                        obronca.getRow(),   obronca.getCol())) {
            komunikat = "Cel zbyt daleko."; return;
        }
        boolean obronzaGinie = atakujacy.atakuj(obronca);
        komunikat = "Atak! Wróg ma " + obronca.getPunktyZycia() + " HP.";
        if (obronzaGinie) { usunJednostke(obronca, graczAI);     komunikat = "Wróg pokonany!"; }
        if (!atakujacy.czyZyje()) { usunJednostke(atakujacy, graczLudzki); komunikat += " Twoja jednostka zginęła!"; }
    }

    // -------------------------------------------------------------------------
    // Budowa miasta
    // -------------------------------------------------------------------------

    public void budujMiastoZaznaczonegoGracza() {
        if (zaznaczonaJednostka == null) { komunikat = "Zaznacz jednostkę."; return; }
        Pole pole = mapa.getPole(zaznaczonaJednostka.getRow(), zaznaczonaJednostka.getCol());
        if (pole.getMiasto() != null)                         { komunikat = "Tu już stoi miasto."; return; }
        if (mapa.czyRegionMaMiasto(pole.getNumerRegionu()))   { komunikat = "Region już ma miasto."; return; }
        int koszt = graczLudzki.getKosztBudowyMiasta();
        if (graczLudzki.getZloto() < koszt)                   { komunikat = "Za mało złota (" + koszt + ")."; return; }
        graczLudzki.odejmijZloto(koszt);
        zalozMiastoNaPolu(pole, graczLudzki, "Miasto " + licznikMiastGracza++);
        komunikat = "Zbudowano miasto " + (licznikMiastGracza - 1) + "!";
        wyczyscZaznaczenie();
    }

    // -------------------------------------------------------------------------
    // Szkolenie jednostki
    // -------------------------------------------------------------------------

    public void szkolJednostkeWZaznaczonymMiescie() {
        if (zaznaczoneMiasto == null) { komunikat = "Zaznacz miasto."; return; }
        if (graczLudzki.getZloto() < Miasto.KOSZT_JEDNOSTKI) {
            komunikat = "Za mało złota (" + Miasto.KOSZT_JEDNOSTKI + ")."; return;
        }
        Pole wolne = znajdzWolnePoleObok(zaznaczoneMiasto.getRow(), zaznaczoneMiasto.getCol());
        if (wolne == null) { komunikat = "Brak miejsca wokół miasta."; return; }
        graczLudzki.odejmijZloto(Miasto.KOSZT_JEDNOSTKI);
        utworzJednostke(wolne, graczLudzki);
        komunikat = "Wyszkolono jednostkę w " + zaznaczoneMiasto.getNazwa() + "!";
    }

    // -------------------------------------------------------------------------
    // Ulepszenia
    // -------------------------------------------------------------------------

    public void ulepszAtakZaznaczonegoGracza() {
        if (!moznaUlepszac()) return;
        if (graczLudzki.getZloto() < Jednostka.KOSZT_ULEPSZENIA) { komunikat = "Za mało złota."; return; }
        if (!zaznaczonaJednostka.moznaUlepszycAtak())             { komunikat = "Max poziom ataku."; return; }
        graczLudzki.odejmijZloto(Jednostka.KOSZT_ULEPSZENIA);
        zaznaczonaJednostka.ulepszAtak();
        komunikat = "Ulepszono atak! ATK: " + zaznaczonaJednostka.getObrazenia();
    }

    public void ulepszZycieZaznaczonegoGracza() {
        if (!moznaUlepszac()) return;
        if (graczLudzki.getZloto() < Jednostka.KOSZT_ULEPSZENIA) { komunikat = "Za mało złota."; return; }
        if (!zaznaczonaJednostka.moznaUlepszycZycie())            { komunikat = "Max poziom HP."; return; }
        graczLudzki.odejmijZloto(Jednostka.KOSZT_ULEPSZENIA);
        zaznaczonaJednostka.ulepszZycie();
        komunikat = "Ulepszono HP! Max: " + zaznaczonaJednostka.getMaksymalnePunktyZycia();
    }

    public void ulepszRuchZaznaczonegoGracza() {
        if (!moznaUlepszac()) return;
        if (graczLudzki.getZloto() < Jednostka.KOSZT_ULEPSZENIA) { komunikat = "Za mało złota."; return; }
        if (!zaznaczonaJednostka.moznaUlepszycRuch())             { komunikat = "Max poziom ruchu."; return; }
        graczLudzki.odejmijZloto(Jednostka.KOSZT_ULEPSZENIA);
        zaznaczonaJednostka.ulepszRuch();
        komunikat = "Ulepszono ruch! Max: " + zaznaczonaJednostka.getMaksymalnyRuch();
    }

    private boolean moznaUlepszac() {
        if (zaznaczonaJednostka == null) { komunikat = "Zaznacz jednostkę."; return false; }
        Pole pole = mapa.getPole(zaznaczonaJednostka.getRow(), zaznaczonaJednostka.getCol());
        if (pole.getMiasto() == null || pole.getMiasto().getWlasciciel() != graczLudzki) {
            komunikat = "Jednostka musi stać w swoim mieście."; return false;
        }
        return true;
    }

    public boolean czyZaznaczonaJednostkaWMiescie() {
        if (zaznaczonaJednostka == null) return false;
        Pole pole = mapa.getPole(zaznaczonaJednostka.getRow(), zaznaczonaJednostka.getCol());
        return pole.getMiasto() != null && pole.getMiasto().getWlasciciel() == graczLudzki;
    }

    // -------------------------------------------------------------------------
    // Tura
    // -------------------------------------------------------------------------

    public void zakonczTure() {
        if (!turaNalezyDoGracza || stanGry != StanGry.TRWA) return;
        wyczyscZaznaczenie();

        graczLudzki.zbierzZasobyZMiast();
        for (Miasto m : graczLudzki.getMiasta()) m.dodajPunktyRozwoju(3 * m.getPoziom());

        turaNalezyDoGracza = false;
        wykonajTureAI();
        graczAI.zbierzZasobyZMiast();
        for (Miasto m : graczAI.getMiasta()) m.dodajPunktyRozwoju(3 * m.getPoziom());

        for (Jednostka j : graczLudzki.getJednostki()) j.rozpocznijNowaTure();
        for (Jednostka j : graczAI.getJednostki())     j.rozpocznijNowaTure();

        numerTury++;
        turaNalezyDoGracza = true;
        komunikat = "Tura " + numerTury + " – Twoja kolej!";

        if (numerTury > LIMIT_TUR) rozstrzygnijLimitTur();
        else sprawdzZwyciestwo();
    }

    // -------------------------------------------------------------------------
    // AI
    // -------------------------------------------------------------------------

    private void wykonajTureAI() {
        List<Jednostka> jednostkiAI = new ArrayList<>(graczAI.getJednostki());
        for (Jednostka jednostka : jednostkiAI) {
            if (!jednostka.czyZyje()) continue;
            Jednostka cel = znajdzNajblizszaJednostkeGracza(jednostka);
            if (cel != null) {
                if (sasiaduja4(jednostka.getRow(), jednostka.getCol(), cel.getRow(), cel.getCol())) {
                    boolean celGinie = jednostka.atakuj(cel);
                    if (celGinie) usunJednostke(cel, graczLudzki);
                    if (!jednostka.czyZyje()) { usunJednostke(jednostka, graczAI); continue; }
                } else {
                    poruszAIKuCelowi(jednostka, cel.getRow(), cel.getCol());
                }
            }
        }

        for (Miasto miasto : graczAI.getMiasta()) {
            if (graczAI.getZloto() >= Miasto.KOSZT_JEDNOSTKI
                    && graczAI.getJednostki().size() < graczAI.getMiasta().size() * 3) {
                Pole wolne = znajdzWolnePoleObok(miasto.getRow(), miasto.getCol());
                if (wolne != null) {
                    graczAI.odejmijZloto(Miasto.KOSZT_JEDNOSTKI);
                    utworzJednostke(wolne, graczAI);
                }
            }
        }

        if (numerTury % 5 == 0 && !graczAI.getJednostki().isEmpty()) {
            int koszt = graczAI.getKosztBudowyMiasta();
            if (graczAI.getZloto() >= koszt) {
                Jednostka pionier = graczAI.getJednostki().get(0);
                Pole pole = mapa.getPole(pionier.getRow(), pionier.getCol());
                if (pole.getMiasto() == null && !mapa.czyRegionMaMiasto(pole.getNumerRegionu())) {
                    graczAI.odejmijZloto(koszt);
                    zalozMiastoNaPolu(pole, graczAI, "Miasto " + licznikMiastAI++);
                }
            }
        }
        sprawdzZwyciestwo();
    }

    private void poruszAIKuCelowi(Jednostka jednostka, int targetRow, int targetCol) {
        int dr = Integer.compare(targetRow, jednostka.getRow());
        int dc = Integer.compare(targetCol, jednostka.getCol());
        int[][] kierunki = {{dr,0},{0,dc},{dr,dc},{-dr,0},{0,-dc}};
        for (int[] k : kierunki) {
            if (k[0] == 0 && k[1] == 0) continue;
            int newRow = jednostka.getRow() + k[0];
            int newCol = jednostka.getCol() + k[1];
            Pole cel = mapa.getPole(newRow, newCol);
            if (cel != null && cel.czyPrzejezdne() && cel.getJednostka() == null) {
                Pole poprzednie = mapa.getPole(jednostka.getRow(), jednostka.getCol());
                if (jednostka.przesun(newRow, newCol, cel.getTeren().kosztRuchu)) {
                    poprzednie.setJednostka(null);
                    cel.setJednostka(jednostka);
                    if (cel.getMiasto() != null && cel.getMiasto().getWlasciciel() == graczLudzki)
                        przejmijMiasto(cel.getMiasto(), graczAI, graczLudzki);
                }
                break;
            }
        }
    }

    private Jednostka znajdzNajblizszaJednostkeGracza(Jednostka zrodlo) {
        Jednostka najblizszy = null;
        int minDist = Integer.MAX_VALUE;
        for (Jednostka cel : graczLudzki.getJednostki()) {
            int d = Math.abs(cel.getRow() - zrodlo.getRow()) + Math.abs(cel.getCol() - zrodlo.getCol());
            if (d < minDist) { minDist = d; najblizszy = cel; }
        }
        return najblizszy;
    }

    // -------------------------------------------------------------------------
    // Warunki zwycięstwa
    // -------------------------------------------------------------------------

    private void sprawdzZwyciestwo() {
        if (graczAI.getJednostki().isEmpty() && graczAI.getMiasta().isEmpty())
            stanGry = StanGry.WYGRANA_GRACZA;
        else if (graczLudzki.getJednostki().isEmpty() && graczLudzki.getMiasta().isEmpty())
            stanGry = StanGry.WYGRANA_AI;
    }

    private void rozstrzygnijLimitTur() {
        int mg = graczLudzki.getMiasta().size(), ma = graczAI.getMiasta().size();
        if (mg > ma) stanGry = StanGry.WYGRANA_GRACZA;
        else if (mg < ma) stanGry = StanGry.WYGRANA_AI;
        else stanGry = StanGry.REMIS;
    }

    // -------------------------------------------------------------------------
    // Pomocnicze
    // -------------------------------------------------------------------------

    private void zalozMiastoNaPolu(Pole pole, Gracz wlasciciel, String nazwa) {
        Miasto miasto = new Miasto(nazwa, pole.getRow(), pole.getCol(), wlasciciel);
        pole.setMiasto(miasto);
        wlasciciel.dodajMiasto(miasto);
    }

    private void utworzJednostke(Pole pole, Gracz wlasciciel) {
        Jednostka j = new Jednostka(pole.getRow(), pole.getCol(), wlasciciel);
        pole.setJednostka(j);
        wlasciciel.dodajJednostke(j);
    }

    private void usunJednostke(Jednostka j, Gracz wlasciciel) {
        Pole pole = mapa.getPole(j.getRow(), j.getCol());
        if (pole != null) pole.setJednostka(null);
        wlasciciel.usunJednostke(j);
    }

    private void przejmijMiasto(Miasto miasto, Gracz nowyWlasciciel, Gracz staryWlasciciel) {
        staryWlasciciel.usunMiasto(miasto);
        nowyWlasciciel.dodajMiasto(miasto);
        miasto.setWlasciciel(nowyWlasciciel);
        komunikat = "Przejęto " + miasto.getNazwa() + "!";
    }

    private Pole znajdzPrzejezdnePole(int row, int col) {
        for (int dr = 0; dr <= 3; dr++)
            for (int dc = 0; dc <= 3; dc++) {
                Pole p = mapa.getPole(row + dr, col + dc);
                if (p != null && p.czyPrzejezdne()) return p;
            }
        return null;
    }

    private Pole znajdzWolnePoleObok(int row, int col) {
        int[][] kierunki = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] k : kierunki) {
            Pole p = mapa.getPole(row + k[0], col + k[1]);
            if (p != null && p.czyPrzejezdne() && p.getJednostka() == null) return p;
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

    public Mapa getMapa()                      { return mapa; }
    public Gracz getGraczLudzki()              { return graczLudzki; }
    public Gracz getGraczAI()                  { return graczAI; }
    public int getNumerTury()                  { return numerTury; }
    public String getKomunikat()               { return komunikat; }
    public StanGry getStanGry()                { return stanGry; }
    public boolean isTuraNalezyDoGracza()      { return turaNalezyDoGracza; }
    public Pole getZaznaczonePole()            { return zaznaczonePole; }
    public Jednostka getZaznaczonaJednostka()  { return zaznaczonaJednostka; }
    public Miasto getZaznaczoneMiasto()        { return zaznaczoneMiasto; }
}
