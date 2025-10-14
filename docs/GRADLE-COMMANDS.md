./gradlew bootRun --args='--spring.profiles.active=dev'
./gradlew bootRun

java \
  -Xms1g -Xmx4g \                     # początkowy 1GB, maksymalny 4GB
  -XX:+UseG1GC \                      # nowoczesny garbage collector
  -XX:+HeapDumpOnOutOfMemoryError \   # zrzut pamięci przy OOM
  -Dspring.profiles.active=prod \     # profil produkcyjny
  -Dlogging.file.path=/var/log/backend \  # katalog logów
  -jar springboot-backend-0.0.1-SNAPSHOT.jar

nohup java -Xms1g -Xmx4g -XX:+UseG1GC \
  -Dspring.profiles.active=prod \
  -jar springboot-backend-0.0.1-SNAPSHOT.jar \
  > /var/log/backend/app.log 2>&1 &


# 🧰 Gradle – Podstawowe Komendy (Spring Boot / Java)

Zbiór najczęściej używanych komend do pracy z projektem **Spring Boot (Gradle)**.
Dotyczy zarówno systemów Linux / macOS (`./gradlew`) jak i Windows (`gradlew.bat`).

---

## 🚀 Uruchamianie aplikacji

| Cel                            | Komenda                                                   | Opis                                           |
| ------------------------------ | --------------------------------------------------------- | ---------------------------------------------- |
| Uruchom aplikację Spring Boot  | `./gradlew bootRun`                                       | Odpala serwer z klasą `@SpringBootApplication` |
| Uruchom z czystym buildem      | `./gradlew clean bootRun`                                 | Czyści i odpala od nowa                        |
| Uruchom z profilem (np. `dev`) | `./gradlew bootRun --args='--spring.profiles.active=dev'` | Ustawia środowisko                             |

---

## 🏗️ Budowanie projektu

| Cel                | Komenda                   | Opis                   |
| ------------------ | ------------------------- | ---------------------- |
| Zbuduj projekt     | `./gradlew build`         | Kompiluje kod i testy  |
| Zbuduj bez testów  | `./gradlew build -x test` | Pomija testy           |
| Wyczyść build      | `./gradlew clean`         | Usuwa katalog `build/` |
| Odbuduj całkowicie | `./gradlew clean build`   | Czysty build od zera   |

---

## 📦 Tworzenie JAR-a

| Cel                    | Komenda                                                          | Opis                                  |
| ---------------------- | ---------------------------------------------------------------- | ------------------------------------- |
| Stwórz plik `.jar`     | `./gradlew bootJar`                                              | Buduje plik aplikacji do uruchomienia |
| Uruchom gotowy JAR     | `java -jar build/libs/<nazwa>.jar`                               | Odpala gotowy serwer                  |
| Uruchom JAR z profilem | `java -jar build/libs/<nazwa>.jar --spring.profiles.active=prod` | Wersja produkcyjna                    |

---

## 🧪 Testowanie

| Cel                   | Komenda                               | Opis                     |
| --------------------- | ------------------------------------- | ------------------------ |
| Uruchom testy         | `./gradlew test`                      | Wykonuje testy JUnit     |
| Pomiń testy           | `./gradlew build -x test`             | Budowanie bez testów     |
| Sprawdź raport testów | `build/reports/tests/test/index.html` | Lokalizacja raportu HTML |

---

## 🔁 Zależności

| Cel                           | Komenda                                                   | Opis                       |
| ----------------------------- | --------------------------------------------------------- | -------------------------- |
| Odśwież zależności            | `./gradlew build --refresh-dependencies`                  | Pobiera biblioteki od nowa |
| Pokaż wszystkie zależności    | `./gradlew dependencies`                                  | Wypisuje drzewo zależności |
| Sprawdź nieużywane zależności | `./gradlew dependencies --configuration runtimeClasspath` | Analiza konfiguracji       |

---

## 🧹 Naprawianie i czyszczenie środowiska

| Cel                       | Komenda                                                        | Opis                          |
| ------------------------- | -------------------------------------------------------------- | ----------------------------- |
| Usuń cache Gradle         | `rm -rf ~/.gradle/caches` *(Linux/macOS)*                      | Czyści lokalne cache          |
| Wyczyść i odbuduj projekt | `./gradlew clean build --refresh-dependencies`                 | Naprawia błędy w bibliotekach |
| Przeładuj projekt w IDE   | *(IntelliJ)* → Prawy klik na `build.gradle` → „Reload Project” | Odświeża konfigurację         |

---

## ⚙️ DevTools i Live Reload

| Cel                | Komenda                           | Opis                                   |
| ------------------ | --------------------------------- | -------------------------------------- |
| Uruchom z DevTools | `./gradlew bootRun`               | Automatyczny restart przy zmianie kodu |
| Restart ręczny     | `Ctrl + F5` *(IntelliJ / VSCode)* | Restartuje aplikację                   |
| Logi DevTools      | `logs/spring.log` lub terminal    | Wyświetla restart i reload info        |

---

## 🧾 Dodatkowo (produkcyjne)

| Cel                     | Komenda                                               | Opis                           |
| ----------------------- | ----------------------------------------------------- | ------------------------------ |
| Uruchom w tle           | `nohup ./gradlew bootRun &`                           | Serwer w tle (Linux)           |
| Zatrzymaj proces        | `pkill -f 'gradlew'` lub `pkill -f 'java'`            | Zatrzymuje uruchomiony backend |
| Uruchom JAR jako usługę | `nohup java -jar build/libs/app.jar > app.log 2>&1 &` | W tle z logowaniem             |

---

## 🧠 Skrótowe podsumowanie

| Co chcesz zrobić      | Komenda                                        |
| --------------------- | ---------------------------------------------- |
| 🔹 Start dev serwera  | `./gradlew bootRun`                            |
| 🔹 Czysty build       | `./gradlew clean build`                        |
| 🔹 Build bez testów   | `./gradlew build -x test`                      |
| 🔹 Uruchom JAR        | `java -jar build/libs/app.jar`                 |
| 🔹 Odśwież zależności | `./gradlew clean build --refresh-dependencies` |

---
