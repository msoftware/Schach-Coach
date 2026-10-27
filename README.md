# Schach-Coach

Browserbasierte Schach-Anwendung: Du spielst gegen eine lokal laufende Engine und bekommst nach jedem eigenen Zug
eine ausführliche, nachvollziehbare Analyse – mit Qualitätsklasse, besserer Alternative, Kandidatenzügen,
taktischen Motiven, strategischen Prinzipien (nach Edward Lasker, *Chess Strategy*) und konkreten Lernhinweisen.

- **Frontend:** Vue 3, Quasar, TypeScript, Pinia, Vite (`frontend/`)
- **Backend:** Java 21, Spring Boot 4.1, Gradle 9.8 (`backend/`)
- **Engine:** Calvin 6.1.1 (Java, UCI, MIT), mitgeliefert unter `backend/engine/`
- **Schachregeln:** Chesslib 1.3.7 (Apache-2.0)
- **Speicherung:** nur im Browser (IndexedDB/localStorage); keine Konten, keine serverseitige Partiespeicherung

## Schnellstart (lokal)

Voraussetzungen: **JDK 21** (inkl. Modul `jdk.incubator.vector`, in jedem regulären JDK enthalten), **Node.js 22+**.

```bash
# 1. Backend (Port 8080) – prüft die Engine-Prüfsumme und startet zwei Calvin-Prozesse
cd backend
./gradlew bootRun            # Windows: gradlew.bat bootRun

# 2. Frontend (Port 5173, leitet /api an das Backend weiter)
cd frontend
npm ci
npm run dev
```

Danach <http://localhost:5173> öffnen.

**Ein einziges Artefakt:** `cd frontend && npm ci && npx vite build`, dann
`cd backend && ./gradlew bootJar -PwithFrontend` und `java -jar build/libs/chess-coach-backend.jar` → <http://localhost:8080>.

**Docker Compose:** `docker compose up --build` → <http://localhost:8081> (siehe [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)).

## Tests

```bash
cd backend  && ./gradlew check checkLicense      # Unit-, Integrations- und Engine-Tests, Coverage-Regel, Lizenzprüfung
cd frontend && npm run typecheck && npm test      # Typecheck, Vitest
cd frontend && npx playwright install chromium firefox && npx playwright test   # End-to-End (benötigt gebautes Backend-JAR)
```

## Dokumentation

| Dokument | Inhalt |
|---|---|
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | Anforderungen (Originalspezifikation) |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Architektur, Komponenten, Abläufe, Entscheidungen |
| [docs/API.md](docs/API.md) | REST-API, Fehlerformat, Beispiele; Vertrag: [docs/openapi.json](docs/openapi.json) |
| [docs/COACHING_RULES.md](docs/COACHING_RULES.md) | Alle 28 Coaching-Regeln mit Erkennung, Begründung und Tests |
| [docs/ENGINE_INTEGRATION.md](docs/ENGINE_INTEGRATION.md) | Engine-Version, UCI, Suchparameter, Spielstärken, Fehlerbehandlung |
| [docs/TESTING.md](docs/TESTING.md) | Teststrategie, Ergebnisse, Abdeckung, Referenzmessung |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | Betrieb, Docker, Konfiguration, Systemanforderungen |
| [docs/USER_GUIDE.md](docs/USER_GUIDE.md) | Benutzerhandbuch |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | Entwicklung, Projektstruktur, neue Regeln |
| [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) | Bibliotheken, Engine, Figuren – Versionen und Lizenzen |

## Lizenz

Eigener Code: MIT ([LICENSE](LICENSE)). Engine, Bibliotheken und Figuren: siehe [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
