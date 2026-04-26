# Gra Strategiczna Turowa – Java Swing

## Opis gry

Turowa gra strategiczna inspirowana serią **Civilization / Humankind**.
Twoim celem jest podbicie świata – zniszcz wszystkie jednostki i miasta wroga,
lub zdobądź ich więcej niż AI w ciągu 30 tur.

Gra toczy się na losowo generowanej mapie z różnymi rodzajami terenu.
Zarządzasz miastami, szkolisz jednostki wojskowe i toczysz bitwy z przeciwnikiem sterowanym przez AI.

---

## Instrukcja obsługi

### Sterowanie

| Akcja | Jak wykonać |
|---|---|
| Zaznacz jednostkę | Kliknij lewym przyciskiem myszy na swojego żołnierza |
| Przesuń jednostkę | Po zaznaczeniu kliknij sąsiednie wolne pole |
| Atakuj wroga | Po zaznaczeniu kliknij wrogą jednostkę obok |
| Szkol jednostkę | Przycisk „Wyszkolej jednostkę" w panelu bocznym |
| Zakończ turę | Przycisk „Zakończ turę →" w panelu bocznym |
| Nowa gra | Menu Gra → Nowa gra |

### Zasoby

- **Złoto** – produkowane przez miasta (5 × poziom miasta na turę)
- **Wyszkolenie jednostki** – kosztuje 20 złota, tworzy ją obok wybranego miasta

### Teren

| Teren | Kolor | Koszt ruchu |
|---|---|---|
| Równina | Zielony | 1 |
| Las | Ciemnozielony | 2 |
| Góry | Brązowy | 3 |
| Pustynia | Żółty | 2 |
| Woda | Niebieski | nieprzejezdna |

### Walka

- Każda jednostka ma **100 punktów życia** i zadaje **30 obrażeń**
- Obrońca odpowiada **15 obrażeniami**
- Atak kończy ruch jednostki w tej turze
- Wejście na pole z miastem wroga **przejmuje je**

### Warunki zakończenia

- **Wygrana** – wróg traci wszystkie jednostki i miasta
- **Przegrana** – tracisz wszystkie jednostki i miasta
- **Po 30 turach** – wygrywa ten, kto ma więcej miast (lub remis)

---

## Lista mechanik

- Losowo generowana mapa z 5 rodzajami terenu
- System ruchu oparty na punktach (koszt zależny od terenu)
- Walka turowa z odpowiedzią obrońcy
- Przejmowanie miast przez wkroczenie
- Produkcja złota przez miasta (rosnąca z poziomem)
- Szkolenie jednostek za złoto
- Awans poziomu miast w czasie
- Prosta AI sterująca przeciwnikiem (ruch ku celowi + szkolenie jednostek)
- Warunki zwycięstwa: podbój lub przewaga po limicie tur

---

## Struktura projektu

```
StrategiaGra/
├── src/
│   ├── Main.java          – punkt startowy
│   ├── OknoGry.java       – główne okno JFrame, menu, instrukcja
│   ├── PanelMapy.java     – rysowanie mapy, obsługa kliknięć
│   ├── PanelBoczny.java   – panel z zasobami i przyciskami
│   ├── SilnikGry.java     – cała logika gry (oddzielona od widoku)
│   ├── Mapa.java          – siatka pól, generowanie terenu
│   ├── Pole.java          – pojedyncze pole mapy
│   ├── Teren.java         – enum rodzajów terenu
│   ├── Gracz.java         – gracz (ludzki lub AI), zasoby
│   ├── Miasto.java        – miasto, produkcja, poziom
│   └── Jednostka.java     – jednostka wojskowa, ruch, walka
└── README.md
```

## Uruchomienie w IntelliJ IDEA

1. Otwórz IntelliJ → **Open** → wybierz folder `StrategiaGra`
2. Ustaw **Sources Root** na folder `src` (prawy klik → Mark Directory As → Sources Root)
3. Upewnij się że masz skonfigurowane **JDK 17+** (File → Project Structure → SDK)
4. Uruchom klasę `Main` (prawy klik → Run 'Main')

## Uruchomienie z linii poleceń

```bash
cd StrategiaGra
mkdir out
javac -d out src/*.java
java -cp out Main
```

---

## Znane ograniczenia

- Jednostki poruszają się tylko o jedno pole na raz (kliknięcie = jeden krok)
- Brak animacji przejść
- AI nie planuje strategicznie – podchodzi do najbliższego celu
- Brak zapisu/wczytywania stanu gry
