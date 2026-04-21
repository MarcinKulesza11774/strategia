# Wyjaśnienie kodu – Fortress

## Jak działa gra od startu

```
Main.java
  └─ GameConfig.init()          ← czyta 5 plików JSON z /data
  └─ TileType.loadAll()         ← rejestruje typy kafelków
  └─ UnitClass.loadAll()        ← rejestruje klasy jednostek
  └─ RoomDef.loadAll()          ← rejestruje definicje pokoi
  └─ SidePanel.initCosts()      ← ładuje koszty budowania
  └─ GameController.start()
       └─ new GameState()       ← generuje mapę, tworzy jednostki startowe
       └─ new GameWindow()      ← tworzy okno Swing
       └─ GamePanel.startLoop() ← uruchamia wątek gry
```

---

## Architektura MVC

```
MODEL                    VIEW                     CONTROLLER
─────────────────        ─────────────────        ─────────────────
GameState                GameWindow               GameController
  WorldMap               GamePanel (JPanel)         .tick(dt)
  WaveManager            MapRenderer                .onLeftClick()
  RoomSystem             HudRenderer                .onRightClick()
  List<Unit>             SidePanel                  .onAreaSelect()
                         Camera                     .recruit()
                                                    .promoteUnit()
```

Model nie wie nic o widoku. Widok nie modyfikuje modelu.
Kontroler jest mostem – odbiera eventy z widoku, modyfikuje model.

---

## Game Loop (GamePanel.java)

```java
private void loop() {
    long last = System.nanoTime();
    while (running) {
        long now = System.nanoTime();
        double dt = (now - last) / 1_000_000_000.0;  // czas od ostatniej klatki w sekundach
        last = now;

        controller.tick(dt);           // logika gry
        SwingUtilities.invokeLater(this::repaint);  // rendering na wątku EDT
        Thread.sleep(1);               // 1ms = max ~1000fps, ale też nie pali CPU
    }
}
```

**Dlaczego `dt` (delta time)?**
Jednostki poruszają się o `speed * dt` kafelków na tick. Dzięki temu przy 60fps
i 30fps poruszają się z tą samą prędkością w czasie rzeczywistym.

**Dlaczego osobny wątek?**
Swing działa na wątku EDT (Event Dispatch Thread). Gdybyśmy robili game loop na EDT,
zablokalibyśmy GUI – okno by nie reagowało. Dlatego logika gry jest na osobnym wątku,
a rendering (repaint) jest zlecany na EDT przez `SwingUtilities.invokeLater`.

---

## System konfiguracji JSON

### Pliki w /data:
| Plik | Co zawiera |
|------|-----------|
| `tiles.json` | Typy kafelków: kolor, czy przejezdny, czy zasób, ile ticków wydobycia |
| `units.json` | Klasy jednostek: HP, atak, obrona, prędkość, czy może zbierać/budować/walczyć |
| `rooms.json` | Definicje pokoi: wymagane meble, buffy własne, buffy sąsiedztwa |
| `buildings.json` | Co można budować i za ile surowców |
| `recruitment.json` | Jakie jednostki rekrutować i za ile |
| `game_config.json` | Wszystko inne: rozmiar mapy, fale wrogów, ekonomia, AI, kamera |

### Jak to działa technicznie:
```
JSON plik → JsonParser.parse() → Map<String,Object> → ConfigLoader → GameConfig
```

`JsonParser` to własny parser JSON bez zewnętrznych zależności (~150 linii).
`ConfigLoader` zamienia surowe `Map<String,Object>` na typowane rekordy (`TileCfg`, `UnitCfg` itd.).
`GameConfig` to singleton – raz załadowany, dostępny wszędzie przez `GameConfig.get()`.

### Jak dodać nowy typ kafelka:
1. Dopisz obiekt do `data/tiles.json`
2. Jeśli to zasób: dodaj `resourceType`, `harvestTicks`, `harvestRemnant`
3. Jeśli ma być budowalny: dopisz do `data/buildings.json` z kosztem
4. Restart gry – wszystko załaduje się automatycznie

### Jak dodać nową jednostkę:
1. Dopisz do `data/units.json`
2. Opcjonalnie dopisz do `data/recruitment.json` żeby była rekrutowalna
3. Jeśli ma inną mechanikę AI: dodaj obsługę w `UnitAI.tickPlayer()` po sprawdzeniu `classId`

---

## Mapa Świata (WorldMap.java)

Mapa to siatka `WIDTH × HEIGHT` kafelków (domyślnie 120×80).

### Generacja proceduralna:
```
1. Wypełnij wszystko trawą (GRASS)
2. Narysuj rzekę (wężyk przez całą mapę)
3. Dla każdego typu zasobu: posadź N klastrów losowo
4. Wyczyść obszar startowy twierdzy (setRadius kafelków DIRT)
5. Postaw magazyn i ognisko startowe
```

### Klaster (placeCluster):
Losowy punkt (cx, cy), losowy promień r.
Dla każdego kafelka w kwadracie cx±r, cy±r:
jeśli odległość od centrum ≤ r × (0.6..1.1) → zamień na dany typ.
Mnożnik losowy daje naturalne, nierówne kształty.

### Culling (nie rysuj niewidocznych kafelków):
```java
int x0 = cam.getFirstTileX();  // pierwsze widoczne
int x1 = cam.getLastTileX();   // ostatnie widoczne
for (int ty = y0; ty <= y1; ty++)
    for (int tx = x0; tx <= x1; tx++)
        drawTile(...)  // rysuj tylko widoczne
```
Przy mapie 120×80=9600 kafelków, ale widać ~55×40=2200 – duże przyspieszenie.

---

## System Pokoi (RoomSystem.java)

Pokój to zamknięty obszar kafelków FLOOR otoczony WALLami.

### Jak wykrywa pokoje (rescan):
```
1. Dla każdego kafelka FLOOR którego jeszcze nie odwiedzono:
2.   BFS: rozlewa się po sąsiednich FLOOR i meblach
3.   Jeśli natrafia na kafelek inny niż WALL/DOOR/mebel/FLOOR → enclosed = false
4.   Jeśli enclosed = true i jest dość kafelków:
5.     Porównaj znalezione meble z wymaganiami każdego RoomDef
6.     Pierwszy pasujący RoomDef → stwórz Room
```

### System sąsiedztwa:
```
Po wykryciu wszystkich pokoi:
  Dla każdej pary pokoi A i B:
    Sprawdź czy jakiś kafelek A sąsiaduje przez WALL z kafelkiem B
    Jeśli tak → są sąsiadami
    Zastosuj bonusy z rooms.json (adjacencyBonuses)
```

Przykład z `rooms.json`:
```json
{
  "id": "barracks",
  "adjacencyBonuses": [
    { "neighborId": "armory", "attackBuff": 3 }
  ]
}
```
Koszary obok Zbrojowni dają +3 ATK jednostkom w koszarach.

---

## Jednostki i AI (Unit.java, UnitAI.java)

### Pozycja zmiennoprzecinkowa:
Jednostka ma `double x, y` (nie int). Dzięki temu poruszanie jest płynne.
`getTileX() = (int) Math.round(x)` – do logiki gry (AI, kolizje).

### Maszyna stanów AI (UnitAI.tickPlayer):
```
Priorytet 1: jeśli jednostka umie walczyć i wróg jest blisko → ATTACKING
Priorytet 2: jeśli gracz wydał rozkaz (hasTarget) → MOVING do celu
Priorytet 3: domyślne zachowanie klasy:
  WORKER/CRAFTSMAN → tickWorker (zbierz zasób lub buduj)
  GUARD/SOLDIER    → tickGuard (patroluj i atakuj)
  TRADER/MERCHANT  → TRADING (logika w GameState.tick)
```

### Zbieranie zasobów (tickWorker):
```
1. Czy jest zadanie budowania w pobliżu? → idź do niego i buduj (60 ticków)
2. Czy jest zasób w zasięgu searchRadius? → idź do niego i zbieraj
3. Nic do roboty → IDLE
```

Zbieranie: robot stoi obok kafelka zasobu i wywołuje `tile.tickHarvest()` co tick.
Po `harvestTicks` tickach zasób znika, gracz dostaje surowce, kafelek zamienia się na remnant.

### Walka:
```java
int damage = Math.max(1, attacker.attack - target.defense/2)
             + random(0, damage*0.25)  // ±25% losowości
```
Cooldown między atakami: `attackCooldownTicks` z `game_config.json`.

---

## Kamera i Zoom (Camera.java)

`tileSize` (piksele na kafelek) kontroluje zoom.
- Mały tileSize → widzisz więcej mapy (oddalony widok)
- Duży tileSize → widzisz mniej, ale więcej detali

### Zoom zachowujący punkt pod kursorem:
```java
void zoom(delta, mouseX, mouseY) {
    worldX = screenToWorld(mouseX)  // punkt świata pod kursorem
    tileSize += delta * zoomStep    // zmień zoom
    camX = worldX - mouseX/tileSize // przesuń kamerę tak żeby ten punkt
    camY = worldY - mouseY/tileSize // nadal był pod kursorem
}
```

### Konwersja ekran ↔ świat:
```java
worldX = mouseX / tileSize + camX   // piksel → kafelek świata
screenX = (worldX - camX) * tileSize // kafelek → piksel
```

---

## Zaznaczanie Obszaru (GamePanel + GameController)

### Drag detection:
```
mousePressed  → zapisz startX, startY (w pikselach)
mouseDragged  → jeśli przesunięto >5px: dragging=true, aktualizuj endX,endY
mouseReleased → jeśli dragging: onAreaSelect(); else: onLeftClick()
```

### onAreaSelect w trybie budowania:
```java
for (ty = y1..y2)
    for (tx = x1..x2)
        placeTile(tx, ty, buildMode)  // stawia kafelek i pobiera surowce
```
Możesz np. zaznaczyć prostokąt i postawić 20 ścian jednym ruchem.

---

## Fale Wrogów (WaveManager.java)

```
PEACE    → odliczaj ticksPeace (z game_config.json: firstWaveDelayTicks)
  ↓ po czasie
WARNING  → odliczaj ticksToWave (warnBeforeTicks = 600 ≈ 10s przy 60fps)
  ↓ po czasie
ACTIVE   → spawnWave() – wrogowie pojawiają się na krawędziach mapy
  ↓ gdy wszyscy wrogowie zginą
CLEARED  → odliczaj betweenWavesTicks → wróć do PEACE
```

### Spawn na krawędziach:
Zbiera wszystkie przejezdne kafelki na obrzeżach mapy co 4 kafelki,
losuje pozycję dla każdego wroga.

---

## Panel Boczny (SidePanel.java)

Layout:
```
JPanel (BorderLayout)
├── NORTH: przycisk Pauza
├── CENTER: JScrollPane (VERTICAL only, no horizontal)
│     └── JPanel (BoxLayout Y)
│           ├── "BUDOWANIE" label
│           ├── przyciski budowania (z buildings.json)
│           ├── "REKRUTACJA" label
│           ├── przyciski rekrutacji (z recruitment.json)
│           ├── "JEDNOSTKI" label
│           └── lista jednostek (odświeżana co 200ms)
└── SOUTH: skróty klawiszowe
```

Kluczowe dla braku poziomego scrolla:
- `JScrollPane.HORIZONTAL_SCROLLBAR_NEVER`
- Każdy przycisk: `setMaximumSize(new Dimension(Integer.MAX_VALUE, height))`
- Panel: `setPreferredSize(new Dimension(200, 0))`

---

## Jak rozszerzać grę

### Nowy pokój (np. "Laboratorium"):
1. Dopisz do `data/rooms.json`:
```json
{
  "id": "lab",
  "label": "Laboratorium",
  "requiredFurniture": ["BOOKSHELF", "ANVIL"],
  "minSize": 6,
  "selfSpeedBuff": 10,
  ...
}
```
2. Gotowe – system automatycznie wykryje pokój na mapie.

### Nowy typ wroga:
1. Dopisz do `data/units.json` z `"isEnemy": true`
2. W `WaveManager.spawnWave()` dodaj warunek kiedy ma się pojawić

### Nowy surowiec (np. "Kryształ"):
1. Dopisz do `ResourceType.java` w bloku static: `register("CRYSTAL", "Kryształ", color)`
2. Dodaj kafelek źródła do `data/tiles.json` z `"resourceType": "CRYSTAL"`
3. Dodaj do `data/game_config.json → startingResources` jeśli chcesz na start

### Zmiana balansu gry:
Edytuj `data/game_config.json` – wszystkie liczby są tam.
Np. `"firstWaveDelayTicks": 7200` daje 2 minuty zamiast 1 na przygotowanie.
