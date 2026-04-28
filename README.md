# Turowa gra strategiczna

___

## Opis

Na losowo generowanej mapie buduj miasta aby przejąć teren i walcz z wrogimi botami.
Gra kończy się gdy wszystkie wrogie jednostki zostaną zniszczone lub po określonej liczbie tur, wygrywa ten kto zajął najwięcej terenów

---

## Sterowanie
- Lewy przycisk – zaznacz / rusz się / atakuj
- Scroll – zoom
- Scroll + przesunięcie – przesuwanie mapy
- Spacja - koniec tury
- budowa miasta, szkolenie i ulepszanie jednostek w oknie po prawej

---

## Lista mechanik

- Losowo generowana mapa
- System ruchu oparty na punktach (koszt zależny od terenu)
- Prosta walka turowa
- Ekonomia oparta na budowaniu miast
- Proste AI sterująca przeciwnikiem
- Warunki zwycięstwa

---

## Uruchomienie w IntelliJ IDEA

1. Otwórz IntelliJ
2. Otwórz folder z projektem
4. Uruchom klasę "Main"

## Uruchomienie z linii poleceń

- cd (folder z projektem)
- mkdir out
- javac -d out src/*.java
- java -cp out Main

---

## Znane błędy

- dla czytelności nie dodawłem grafiki jednostek
- boty są czasem nieogarnięte i się zacinają, trzeba wtedy zrestartować mapę
