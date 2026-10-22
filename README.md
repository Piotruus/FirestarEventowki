# MoneyCode Eventówki — Paper 1.21.4

**To projekt źródłowy, nie skompilowany ani przetestowany plik JAR.**

## Budowanie
Wymaga Java 21 i Gradle 8.10+ oraz dostępu do repozytoriów Paper i EngineHub.
Uruchom `gradle build` w katalogu projektu; wynik powinien znaleźć się w `build/libs/`.
Zależności na serwerze: Paper 1.21.4, WorldGuard, WorldEdit.

## Obsługa
`/mceventowki` — GUI dla OP lub `moneycode.eventowki.admin`. Kliknięcie ikony daje przedmiot.
Regiony: nazwa zawiera `pvp` = aktywne; nazwa zawiera `spawn` = zablokowane (priorytet).
Przedmioty: Topór Gromu, Gruszka, Wyrzutnia Otchłani, Klepsydra Czasu, Hak Otchłani.
CustomModelData: 12001–12005. Wymagany zgodny MoneyCode Texture Pack.

## Ostrzeżenia przed wdrożeniem
Nie testowano kompilacji ani działania na żywym serwerze. Klatka korzysta z tymczasowego szkła; należy przetestować integracje z innymi pluginami, ucieczkę teleportacją, ochronę bloków, restart i obciążenie. Przed wdrożeniem wykonaj backup mapy. Nie deklarujemy pełnej niezniszczalności w każdym możliwym pluginie ani niezawodności przy awarii procesu.


## Ważne: błąd ładowania
Wymagane działające pluginy WorldGuard i WorldEdit zgodne z Paper 1.21.4. Sprawdź pełne `Caused by:` w konsoli, gdy ładowanie się nie powiedzie. Repo zawiera GitHub Actions (`.github/workflows/build.yml`) do kompilacji JAR. Nie testowano działania na serwerze.
