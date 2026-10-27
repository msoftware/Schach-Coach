# Betrieb und Deployment

## Systemanforderungen

| Komponente | Anforderung |
|---|---|
| Java | JDK 21 (getestet: Oracle JDK 21.0.10; in Docker Eclipse Temurin 21.0.12). Ein JDK wird benötigt, weil Calvin das Modul `jdk.incubator.vector` nutzt. |
| Node.js | 22 oder neuer (nur zum Bauen des Frontends; getestet mit 24.14) |
| Speicher | ca. 1,5–2 GB RAM (Backend + zwei Engine-Prozesse mit je max. 512 MB Heap) |
| CPU | 2 Kerne empfohlen (je ein Thread für Spiel- und Analyse-Engine) |
| Betriebssysteme | Windows 10/11, Linux (x86-64), macOS – überall, wo ein JDK 21 läuft; getestet: Windows 11 |
| Browser | aktuelle Chromium-basierte Browser und Firefox (getestet), Safari (nicht getestet) |

Es ist keine separate Installation einer Schach-Engine nötig; Calvin liegt unter `backend/engine/`.

## Variante A: lokal (Entwicklung)

```bash
cd backend && ./gradlew bootRun
cd frontend && npm ci && npm run dev     # http://localhost:5173
```

## Variante B: ein ausführbares JAR

```bash
cd frontend && npm ci && npx vite build
cd ../backend && ./gradlew bootJar -PwithFrontend
java -jar build/libs/chess-coach-backend.jar --chesscoach.engine.calvin.jar-path=engine/calvin-chess-engine-6.1.1.jar
# http://localhost:8080
```

Das JAR sucht die Engine relativ zum Arbeitsverzeichnis (`engine/…`) und ersatzweise unter `backend/engine/…`.

## Variante C: Docker Compose

```bash
docker compose up --build      # http://localhost:8081
```

- `backend`: Gradle-Build im Container, Laufzeit Eclipse Temurin 21 (JDK), Engine unter `/app/engine`, Profil `prod`
  (Swagger-UI aus), läuft als unprivilegierter Benutzer.
- `frontend`: Vite-Build, ausgeliefert von nginx; `/api` wird an `backend:8080` weitergeleitet (gleicher Ursprung).
- Hinweis: Die Docker-Dateien wurden erstellt, ein Image-Build konnte in der Entwicklungsumgebung mangels laufendem
  Docker-Daemon nicht ausgeführt werden. Die verwendeten Basis-Image-Tags wurden auf Existenz geprüft.

## Konfiguration (`backend/src/main/resources/application.yml`)

Alle Werte lassen sich per Umgebungsvariable überschreiben (z. B. `CHESSCOACH_ANALYSIS_CANDIDATE_NODES=120000`).

| Schlüssel | Bedeutung | Standard |
|---|---|---|
| `chesscoach.engine.type` | `CALVIN` oder `UCI` | `CALVIN` |
| `chesscoach.engine.calvin.jar-path` | Pfad zum Engine-JAR | `engine/calvin-chess-engine-6.1.1.jar` |
| `chesscoach.engine.calvin.max-heap-mb` | Heap je Engine-Prozess | 512 |
| `chesscoach.engine.hash-mb` / `threads` | UCI-Optionen | 32 / 1 |
| `chesscoach.engine.start-timeout` / `search-grace` / `max-search-time` | Zeitgrenzen | 30 s / 3 s / 20 s |
| `chesscoach.analysis.*` | Suchgrenzen der Analyse | siehe ENGINE_INTEGRATION.md |
| `chesscoach.difficulty.levels.*` | Engine-Parameter je Spielstärke | siehe ENGINE_INTEGRATION.md |
| `chesscoach.coaching.classification.*` | Schwellen der Qualitätsklassen | 10/25/60/120/250 cp |
| `chesscoach.coaching.disabled-rules` | deaktivierte Regel-IDs | leer |
| `chesscoach.api.allowed-origins` | CORS-Ursprünge | `http://localhost:5173`, `http://127.0.0.1:5173`, `http://localhost:8080` |
| `chesscoach.api.rate-limit-per-minute` | rechenintensive Anfragen je Client | 120 |
| `chesscoach.api.max-games` / `game-idle-timeout` | Partien im Arbeitsspeicher | 500 / 6 h |

## Betriebshinweise

- Partien liegen nur im Arbeitsspeicher; nach einem Neustart des Backends stellt das Frontend die Partie aus dem
  Browser-Speicher wieder her.
- Gesundheitsprüfung: `GET /api/health` (`UP`, `DEGRADED` bei nicht verfügbarer Engine).
- Logs enthalten keine Partien; protokolliert werden Engine-Start, Neustarts und Fehler.
