# Turowa Gra Strategiczna – Java Swing

## Opis gry
Turowa gra strategiczna 1vs AI. Gracz kontroluje niebieską frakcję i stara się
zniszczyć wszystkie czerwone jednostki przeciwnika.

## Jak uruchomić w IntelliJ
1. Otwórz folder `StrategyGame` jako projekt (File → Open)
2. Kliknij prawym na folder `src` → **Mark Directory as → Sources Root**
3. IntelliJ automatycznie wykryje klasę `Main` z metodą `main()`
4. Kliknij zielony przycisk ▶ obok metody `main` w pliku `Main.java`

## Sterowanie
1. Kliknij swoją jednostkę (niebieski krąg) – żółta ramka = zaznaczona
2. **Zielone pola** = możliwy ruch, kliknij żeby się poruszyć
3. **Czerwone pola** = możliwy atak, kliknij wroga żeby zaatakować
4. Kliknij **Zakoncz ture** gdy skończyłeś wszystkie akcje

## Jednostki
| Symbol | Nazwa    | HP | ATK | DEF | RUCH |
|--------|----------|----|-----|-----|------|
| W      | Wojownik | 30 |  8  |  3  |  3   |
| L      | Łucznik  | 20 |  6  |  1  |  4   |
| R      | Rycerz   | 50 | 10  |  5  |  2   |

## Tereny
- Równina – koszt ruchu: 1
- Las     – koszt ruchu: 2  
- Góry    – koszt ruchu: 3
- Woda    – nieprzejezdna

## Mechaniki
- Mapa 10×14 z 4 typami terenu (różne koszty ruchu)
- BFS do obliczania zasięgu ruchu
- Walka ATK/DEF z losowym wahaniem ±20%
- AI zbliża się do gracza i atakuje (greedy)
- Warunki wygranej: zniszcz wszystkie jednostki wroga

## Struktura projektu (MVC)
```
src/
├── Main.java              – punkt startowy
├── model/
│   ├── Terrain.java
│   ├── Tile.java
│   ├── Player.java
│   ├── UnitType.java
│   ├── Unit.java
│   ├── GameMap.java
│   └── GameState.java
├── view/
│   ├── GameWindow.java
│   ├── MapPanel.java
│   └── InfoPanel.java
├── controller/
│   └── GameController.java
└── ai/
    └── SimpleAI.java
```

## Znane ograniczenia
- Brak zapisu stanu gry
- Brak animacji ruchu
- Brak dźwięku
