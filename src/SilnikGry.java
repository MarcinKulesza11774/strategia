import java.awt.Color;
import java.util.*;
public class SilnikGry {
    private final int limitTur;

    // Kolory kolejnych graczy AI
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

    private int numerTury = 1;
    private boolean turaNalezyDoGracza = true;
    private String komunikat = "Tura 1";

    private Pole zaznaczonePole;
    private Jednostka zaznaczonaJednostka;
    private Miasto zaznaczoneMiasto;

    public enum StanGry { TRWA, WYGRANA_GRACZA, PRZEGRANA, REMIS }
    private StanGry stanGry = StanGry.TRWA;

    private int licznikMiastGracza = 1;
    private final int[] licznikMiastAI;

    private final Random rng = new Random();

    public SilnikGry(UstawieniaGry ust) {
        mapa = new Mapa(ust, System.currentTimeMillis());
        graczLudzki = new Gracz("Gracz", new Color(50, 120, 220), false);
        graczeAI = new ArrayList<>();
        for (int i = 0; i < ust.liczbaAI; i++) {
            Color kolor = KOLORY_AI[i % KOLORY_AI.length];
            graczeAI.add(new Gracz("AI " + (i + 1), kolor, true));
        }
        wszyscy = new ArrayList<>();
        wszyscy.add(graczLudzki);
        wszyscy.addAll(graczeAI);

        limitTur = ust.liczbaTur;
        licznikMiastAI = new int[ust.liczbaAI];
        Arrays.fill(licznikMiastAI, 1);

        rozmieszczGraczy();
    }

//    Losowo rozmieszcza startowe miasto i jednostkę każdego gracza
    private void rozmieszczGraczy() {
        for (int i = 0; i < wszyscy.size(); i++) {
            Gracz gracz = wszyscy.get(i);
            Pole start = znajdzLosowyWolnyStart();
            if (start == null) continue;
            String nazwa = gracz.isCzyAI()
                ? "Miasto " + licznikMiastAI[graczeAI.indexOf(gracz)]++
                : "Miasto " + licznikMiastGracza++;
            zalozMiastoNaPolu(start, gracz, nazwa);
            Pole obokMiasta = znajdzWolnePoleObok(start.getRow(), start.getCol());
            if (obokMiasta != null) utworzJednostke(obokMiasta, gracz);
        }
    }

//     Szuka losowego przejezdnego pola w regionie bez istniejacego miasta.
//     Losuje pole i sprawdza czy jego region jest wolny.
    private Pole znajdzLosowyWolnyStart() {
        for (int proba = 0; proba < 500; proba++) {
            int row = rng.nextInt(mapa.getLiczbaWierszy());
            int col = rng.nextInt(mapa.getLiczbaKolumn());
            Pole p = mapa.getPole(row, col);
            if (p == null || !p.czyPrzejezdne() || p.getMiasto() != null) continue;
            if (!mapa.czyRegionMaMiasto(p.getNumerRegionu())) return p;
        }
        // Fallback – pierwsze przejezdne pole w regionie bez miasta
        for (int row = 0; row < mapa.getLiczbaWierszy(); row++)
            for (int col = 0; col < mapa.getLiczbaKolumn(); col++) {
                Pole p = mapa.getPole(row, col);
                if (p != null && p.czyPrzejezdne() && p.getMiasto() == null
                        && !mapa.czyRegionMaMiasto(p.getNumerRegionu())) return p;
            }
        return null;
    }

    public void kliknijPole(int row, int col) {
        if (!turaNalezyDoGracza || stanGry != StanGry.TRWA) return;
        Pole klikniete = mapa.getPole(row, col);
        if (klikniete == null) return;

        if (zaznaczonaJednostka != null) {
            Jednostka celWrogi = klikniete.getJednostka();
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

        for (Pole nastepne : sciezka) {
            if (jednostka.getPozostalyruch() < nastepne.getTeren().kosztRuchu) break;
            Pole poprzednie = mapa.getPole(jednostka.getRow(), jednostka.getCol());
            jednostka.przesun(nastepne.getRow(), nastepne.getCol(), nastepne.getTeren().kosztRuchu);
            poprzednie.setJednostka(null);
            nastepne.setJednostka(jednostka);
            if (nastepne.getMiasto() != null
                    && nastepne.getMiasto().getWlasciciel() != graczLudzki)
                przejmijMiasto(nastepne.getMiasto(), graczLudzki,
                               nastepne.getMiasto().getWlasciciel());
        }
        komunikat = "Pozostały ruch: " + jednostka.getPozostalyruch();
    }

    private void akcjaAtak(Jednostka atakujacy, Jednostka obronca) {
        if (!sasiaduja4(atakujacy.getRow(), atakujacy.getCol(),
                        obronca.getRow(),   obronca.getCol())) {
            komunikat = "Cel zbyt daleko."; return;
        }
        Gracz wlascicielObroncy = obronca.getWlasciciel();
        boolean obronzaGinie = atakujacy.atakuj(obronca);
        komunikat = "Atak - Wróg ma " + obronca.getPunktyZycia() + " HP.";
        if (obronzaGinie) { usunJednostke(obronca, wlascicielObroncy); komunikat = "Wróg pokonany"; }
        if (!atakujacy.czyZyje()) {
            usunJednostke(atakujacy, graczLudzki); komunikat += " Twoja jednostka zginęła";
        }
    }

    public void budujMiastoZaznaczonegoGracza() {
        if (zaznaczonaJednostka == null) { komunikat = "Zaznacz jednostkę."; return; }
        Pole pole = mapa.getPole(zaznaczonaJednostka.getRow(), zaznaczonaJednostka.getCol());
        if (pole.getMiasto() != null)                       { komunikat = "Tu już stoi miasto"; return; }
        if (mapa.czyRegionMaMiasto(pole.getNumerRegionu())) { komunikat = "Region już ma miasto"; return; }
        int koszt = graczLudzki.getKosztBudowyMiasta();
        if (graczLudzki.getZloto() < koszt)                 { komunikat = "Za mało złota (" + koszt + ")"; return; }
        graczLudzki.odejmijZloto(koszt);
        zalozMiastoNaPolu(pole, graczLudzki, "Miasto " + licznikMiastGracza++);
        komunikat = "Zbudowano miasto";
        wyczyscZaznaczenie();
    }

    public void szkolJednostkeWZaznaczonymMiescie() {
        if (zaznaczoneMiasto == null) { komunikat = "Zaznacz miasto"; return; }
        if (graczLudzki.getZloto() < Miasto.KOSZT_JEDNOSTKI) {
            komunikat = "Za mało złota (" + Miasto.KOSZT_JEDNOSTKI + ")"; return;
        }
        Pole wolne = znajdzWolnePoleObok(zaznaczoneMiasto.getRow(), zaznaczoneMiasto.getCol());
        if (wolne == null) { komunikat = "Brak miejsca wokół miasta"; return; }
        graczLudzki.odejmijZloto(Miasto.KOSZT_JEDNOSTKI);
        utworzJednostke(wolne, graczLudzki);
        komunikat = "Wyszkolono jednostkę";
    }

    public void ulepszAtakZaznaczonegoGracza() {
        if (!moznaUlepszac()) return;
        if (graczLudzki.getZloto() < Jednostka.KOSZT_ULEPSZENIA) { komunikat = "Za mało złota"; return; }
        if (!zaznaczonaJednostka.moznaUlepszycAtak())             { komunikat = "Max poziom"; return; }
        graczLudzki.odejmijZloto(Jednostka.KOSZT_ULEPSZENIA);
        zaznaczonaJednostka.ulepszAtak();
        komunikat = "Ulepszono atak";
    }

    public void ulepszZycieZaznaczonegoGracza() {
        if (!moznaUlepszac()) return;
        if (graczLudzki.getZloto() < Jednostka.KOSZT_ULEPSZENIA) { komunikat = "Za mało złota"; return; }
        if (!zaznaczonaJednostka.moznaUlepszycZycie())            { komunikat = "Max poziom"; return; }
        graczLudzki.odejmijZloto(Jednostka.KOSZT_ULEPSZENIA);
        zaznaczonaJednostka.ulepszZycie();
        komunikat = "Ulepszono HP";
    }

    public void ulepszRuchZaznaczonegoGracza() {
        if (!moznaUlepszac()) return;
        if (graczLudzki.getZloto() < Jednostka.KOSZT_ULEPSZENIA) { komunikat = "Za mało złota."; return; }
        if (!zaznaczonaJednostka.moznaUlepszycRuch())             { komunikat = "Max poziom."; return; }
        graczLudzki.odejmijZloto(Jednostka.KOSZT_ULEPSZENIA);
        zaznaczonaJednostka.ulepszRuch();
        komunikat = "Ulepszono ruch";
    }

    private boolean moznaUlepszac() {
        if (zaznaczonaJednostka == null) { komunikat = "Zaznacz jednostkę."; return false; }
        Pole pole = mapa.getPole(zaznaczonaJednostka.getRow(), zaznaczonaJednostka.getCol());
        if (pole.getMiasto() == null || pole.getMiasto().getWlasciciel() != graczLudzki) {
            komunikat = "Jednostka musi stać w swoim mieście"; return false;
        }
        return true;
    }

    public boolean czyZaznaczonaJednostkaWMiescie() {
        if (zaznaczonaJednostka == null) return false;
        Pole pole = mapa.getPole(zaznaczonaJednostka.getRow(), zaznaczonaJednostka.getCol());
        return pole.getMiasto() != null && pole.getMiasto().getWlasciciel() == graczLudzki;
    }

    // obsługa tury

    public void zakonczTure() {
        if (!turaNalezyDoGracza || stanGry != StanGry.TRWA) return;
        wyczyscZaznaczenie();

        graczLudzki.zbierzZasobyZMiast();
        for (Miasto m : graczLudzki.getMiasta()) m.dodajPunktyRozwoju(3 * m.getPoziom());

        turaNalezyDoGracza = false;
        for (int i = 0; i < graczeAI.size(); i++) {
            Gracz ai = graczeAI.get(i);
            if (!ai.getMiasta().isEmpty() || !ai.getJednostki().isEmpty())
                wykonajTureAI(ai, i);
            ai.zbierzZasobyZMiast();
            for (Miasto m : ai.getMiasta()) m.dodajPunktyRozwoju(3 * m.getPoziom());
        }

        for (Gracz g : wszyscy)
            for (Jednostka j : g.getJednostki()) j.rozpocznijNowaTure();

        numerTury++;
        turaNalezyDoGracza = true;
        komunikat = "Tura " + numerTury + " – Twoja kolej!";

        if (numerTury > limitTur) rozstrzygnijLimitTur();
        else sprawdzZwyciestwo();
    }

    private void wykonajTureAI(Gracz ai, int indeks) {
        List<Jednostka> jednostki = new ArrayList<>(ai.getJednostki());
        for (Jednostka j : jednostki) {
            if (!j.czyZyje()) continue;
            Jednostka cel = znajdzNajblizszaWrogaJednostke(j, ai);
            if (cel != null) {
                if (sasiaduja4(j.getRow(), j.getCol(), cel.getRow(), cel.getCol())) {
                    Gracz wlascicielCelu = cel.getWlasciciel();
                    boolean ginie = j.atakuj(cel);
                    if (ginie) usunJednostke(cel, wlascicielCelu);
                    if (!j.czyZyje()) { usunJednostke(j, ai); continue; }
                } else {
                    poruszAIDoCelu(j, cel.getRow(), cel.getCol(), ai);
                }
            }
        }
        // Szkolenie jednostek
        for (Miasto m : ai.getMiasta()) {
            if (ai.getZloto() >= Miasto.KOSZT_JEDNOSTKI
                    && ai.getJednostki().size() < ai.getMiasta().size() * 3) {
                Pole wolne = znajdzWolnePoleObok(m.getRow(), m.getCol());
                if (wolne != null) {
                    ai.odejmijZloto(Miasto.KOSZT_JEDNOSTKI);
                    utworzJednostke(wolne, ai);
                }
            }
        }
        // Budowa miasta co kilka tur
        if (numerTury % 5 == 0 && !ai.getJednostki().isEmpty()) {
            int koszt = ai.getKosztBudowyMiasta();
            if (ai.getZloto() >= koszt) {
                Jednostka pionier = ai.getJednostki().get(0);
                Pole pole = mapa.getPole(pionier.getRow(), pionier.getCol());
                if (pole.getMiasto() == null && !mapa.czyRegionMaMiasto(pole.getNumerRegionu())) {
                    ai.odejmijZloto(koszt);
                    zalozMiastoNaPolu(pole, ai, "Miasto " + licznikMiastAI[indeks]++);
                }
            }
        }
    }

    private void poruszAIDoCelu(Jednostka j, int targetRow, int targetCol, Gracz ai) {
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
                    if (cel.getMiasto() != null && cel.getMiasto().getWlasciciel() != ai)
                        przejmijMiasto(cel.getMiasto(), ai, cel.getMiasto().getWlasciciel());
                }
                break;
            }
        }
    }

    private Jednostka znajdzNajblizszaWrogaJednostke(Jednostka zrodlo, Gracz wlasciciel) {
        Jednostka najblizszy = null;
        int minDist = Integer.MAX_VALUE;
        for (Gracz g : wszyscy) {
            if (g == wlasciciel) continue;
            for (Jednostka cel : g.getJednostki()) {
                int d = Math.abs(cel.getRow() - zrodlo.getRow())
                      + Math.abs(cel.getCol() - zrodlo.getCol());
                if (d < minDist) { minDist = d; najblizszy = cel; }
            }
        }
        return najblizszy;
    }

//    oblicza najkrótszą ścieżkę do celu z uwzględnieniem kosztów ruchu terenu
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

//    określa odległość od celu w linii prostej
    private int heurystyka(int row, int col, int celRow, int celCol) {
        return Math.abs(celRow - row) + Math.abs(celCol - col);
    }

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
        if (mg > maxAI)      stanGry = StanGry.WYGRANA_GRACZA;
        else if (mg < maxAI) stanGry = StanGry.PRZEGRANA;
        else                 stanGry = StanGry.REMIS;
    }

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
        komunikat = "Przejęto miasto";
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

    public int getLimitTur()                   { return limitTur; }
    public Mapa getMapa()                      { return mapa; }
    public Gracz getGraczLudzki()              { return graczLudzki; }
    public List<Gracz> getGraczeAI()           { return graczeAI; }
    public List<Gracz> getWszyscy()            { return wszyscy; }
    public int getNumerTury()                  { return numerTury; }
    public String getKomunikat()               { return komunikat; }
    public StanGry getStanGry()                { return stanGry; }
    public boolean isTuraNalezyDoGracza()      { return turaNalezyDoGracza; }
    public Pole getZaznaczonePole()            { return zaznaczonePole; }
    public Jednostka getZaznaczonaJednostka()  { return zaznaczonaJednostka; }
    public Miasto getZaznaczoneMiasto()        { return zaznaczoneMiasto; }
}
