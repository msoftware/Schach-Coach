# Entwicklung

## Projektstruktur

```
backend/                     Spring Boot, Gradle (Kotlin DSL), Java 21
  engine/                    Calvin-JAR + Lizenz (SHA-256 im Build geprüft)
  config/allowed-licenses.json
  src/main/java/de/chesscoach/
    api/                     REST-Controller, ApiMapper
    application/game/        GameService, MoveService, ComputerPlayer, GameStore, AnalysisQueryService
    application/analysis/    AnalysisService, MoveAnalyzer, AnalysisEntry
    chess/                   ChessRulesService, ChessPositionAdapter (Chesslib)
    coaching/                Regel-Engines, MistakeClassifier, ExplanationBuilder, PositionContext, EngineLine
    coaching/tactical/       TACT-001 … TACT-016
    coaching/strategic/      STRAT-001 … STRAT-012
    domain/                  position, game, analysis, coaching (Records/Enums)
    engine/                  ChessEngine, UciChessEngine, CalvinChessEngine, UciProcessManager, Parser, Factory, Registry
    dto/request|response/    API-Verträge
    infrastructure/          Konfiguration, CORS, Rate-Limit, Fehlerbehandlung
frontend/                    Vue 3 + Quasar + TypeScript (Vite)
  src/app, pages, components, stores, services, models, utils, assets/chess-pieces
  e2e/                       Playwright
docs/                        Dokumentation, openapi.json
licenses/                    mitgelieferte Lizenztexte
```

## Versionen (fixiert)

| Bereich | Version |
|---|---|
| Java | 21 (Gradle-Toolchain) |
| Gradle | 9.8.0 (Wrapper) |
| Spring Boot | 4.1.2 (Spring Framework 7.0.10) |
| springdoc-openapi | 3.2.0 |
| Chesslib | 1.3.7 (JitPack) |
| Calvin | 6.1.1 |
| JaCoCo | 0.8.14 |
| Vue / Quasar / Pinia / Vue Router | 3.5.44 / 2.35.1 / 3.0.4 / 4.6.4 |
| Vite / Vitest / TypeScript / vue-tsc | 8.3.4 / 4.1.12 / 5.9.3 / 3.3.13 |
| Playwright | 1.64.0 |

Bibliotheksprüfung (Requirements 3.2): Calvin 6.1.1 ist das aktuelle Release (MIT, GitHub-Release-JAR, Java ≥ 17 laut
Manifest `Build-Jdk-Spec: 17`); läuft mit Java 21. Chesslib 1.3.7 ist nur über JitPack verfügbar (nicht auf Maven
Central); im Build ist JitPack ausschließlich für `com.github.bhlangonijr` freigegeben. Erkennung von Schach, Matt, Patt,
Rochade, En passant, Umwandlung und Remis ist in `ChessRulesServiceTest` geprüft, der UCI-Dialog in
`UciEngineProtocolTest`/`CalvinEngineIT`.

## Häufige Befehle

```bash
cd backend
./gradlew test -PskipEngineTests       # schnell, ohne Engine
./gradlew test --tests "*TacticalRulesTest"
./gradlew check checkLicense           # alles inkl. Coverage-Regel und Lizenzprüfung
./gradlew bootRun

cd frontend
npm run dev | npm test | npm run typecheck | npx vite build | npx playwright test
```

## Neue Coaching-Regel hinzufügen

1. Klasse in `coaching/tactical` bzw. `coaching/strategic` anlegen, `AbstractRule` erweitern und `TacticalRule` bzw.
   `StrategicRule` implementieren, mit `@Component` registrieren.
2. ID, Titel, Zweck, Eingabemerkmale und Lernprinzip im Konstruktor angeben; `matches` als schnelle Vorprüfung.
3. In `evaluate` Befunde mit `FindingBuilder` erzeugen: Wirkung, Gewichtung, `verified()` nur bei Bestätigung durch legale
   Züge oder die Engine-Variante, betroffene Felder/Figuren, Beweiskette.
4. Eigene Tests mit kuratierten Stellungen (`Contexts.of(fen, züge…)`) inkl. Gegenfall; ggf. Fall im
   `ExplanationQualityTest`-Katalog.
5. `chesscoach.coaching.rules-version` erhöhen (Analyse-Cache im Browser wird dadurch ungültig) und
   `docs/COACHING_RULES.md` ergänzen.

Strategische Regeln dürfen keine Engine-Klassen verwenden; die Engine-Integration muss für neue Regeln nicht geändert werden.

## Konventionen

- Die Benutzeroberfläche und alle Erklärungen sind deutsch; Code-Bezeichner englisch.
- Das Frontend implementiert keine Schachregeln (nur FEN-Darstellung).
- Keine generierten Bewertungen außerhalb von Testdaten; Testvarianten werden in den Tests auf Legalität geprüft.
