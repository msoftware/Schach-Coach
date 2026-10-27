# Architektur

## Überblick

```
┌──────────────────────── Browser ────────────────────────┐
│ Vue 3 + Quasar (frontend/)                              │
│  GamePage ─ ChessBoard · MoveList · GameControls        │
│           ─ CoachingSummary · CoachingDetails ·         │
│             CandidateMoves · VariationViewer            │
│  Pinia: gameStore · analysisStore · settingsStore       │
│  IndexedDB: games · settings · learningStats ·          │
│             analysisCache   (+ localStorage Settings)   │
└───────────────┬─────────────────────────────────────────┘
                │ REST/JSON (/api, RFC 9457 bei Fehlern)
┌───────────────▼──────────── Spring Boot (backend/) ─────┐
│ api/          Controller, ApiMapper (DTOs)              │
│ application/  GameService · MoveService · ComputerPlayer│
│               AnalysisService (Warteschlange) ·         │
│               MoveAnalyzer (Pipeline)                   │
│ chess/        ChessRulesService · ChessPositionAdapter  │──► Chesslib 1.3.7
│ engine/       ChessEngine · UciChessEngine ·            │
│               CalvinChessEngine · UciProcessManager ·   │──► Calvin-Prozesse
│               EngineAnalysisParser · ChessEngineFactory │    ("play", "analysis")
│ coaching/     TacticalRuleEngine (TACT-001…016) ·       │
│               StrategicRuleEngine (STRAT-001…012) ·     │
│               MistakeClassifier · ExplanationBuilder    │
│ domain/       position (Stellungsmodell, SEE, Bauern-   │
│               struktur, Linienmotive), game, analysis,  │
│               coaching (Befunde, Erklärungen)           │
│ infrastructure/ config (Properties, CORS, Rate-Limit),  │
│               error (GlobalExceptionHandler)            │
└─────────────────────────────────────────────────────────┘
```

## Verantwortlichkeiten

| Komponente | Aufgabe |
|---|---|
| `GameService` | Partiezustand im Arbeitsspeicher (`GameStore`), Start/Wiederherstellung, Undo/Redo, Aufgabe, Einstellungen, Positionsversion |
| `MoveService` | Zugvalidierung, Ausführung, Idempotenz, Computerzug, Anstoßen der Analyse, Tipp |
| `ComputerPlayer` | Spielstärke über Engine-Parameter (Knoten/Zeit, bei schwachen Stufen Auswahl aus zufälliger Teilmenge legaler Züge, jeweils von der Engine bewertet) |
| `AnalysisService` | Einreihen, Ausführen, Abbrechen von Analysen auf der Analyse-Engine (eine Analyse gleichzeitig, begrenzte Warteschlange) |
| `MoveAnalyzer` | Pipeline P₀ → Vorauswahl → Kandidaten (P_u, P_b, …) → Varianten prüfen → Ebenen 1–3 |
| `ChessRulesService` | einzige Quelle für Schachregeln (Chesslib-Adapter, FEN-Validierung, Variantenprüfung) |
| `UciChessEngine`/`CalvinChessEngine` | UCI-Dialog, Parsen, Timeouts, Neustart, Legalitätsprüfung von `bestmove` |
| `UciProcessManager` | Prozessstart mit festem Kommando, begrenztes Lesen der Ausgabe, Beenden |
| Regel-Engines | fehlertolerante Auswertung aller aktivierten Regeln |
| `MistakeClassifier` | Qualitätsklasse (inkl. Matt, Swing, Gewinnerwartung, Unsicherheit) |
| `ExplanationBuilder` | Erklärungstyp, sieben Leitfragen, Quellenangaben, Vorrang der Taktik |

## Wichtige Abläufe

**Benutzerzug:** `POST /moves` prüft unter der Sitzungssperre die erwartete Positionsversion, Spielende, Zugrecht und
Legalität, führt den Zug aus (Version + 1) und reiht eine Analyse ein (`analysisStatus = PENDING`). Das Frontend fragt
`GET /analysis/{ply}` ab. Standardmäßig wartet der Computer, bis das Coaching fertig ist (einstellbar); danach fordert das
Frontend `POST /engine-move` an.

**Analyse (Requirements 6.1):** P₀ wird mit festen Knoten bewertet. Alle legalen Züge werden in einer schnellen Vorauswahl
über ihre Folgestellungen bewertet; die besten Kandidaten sowie gespielter und Engine-Bestzug werden anschließend
**unter identischen Suchgrenzen** bewertet. Der Verlust ist L = E(P_b) − E(P_u) aus Sicht des Benutzers. In taktisch
kritischen Stellungen (Schlag-/Schachkandidaten, große Bewertungsspreizung, Mattwerte) werden die Kandidaten mit
doppeltem Limit gerechnet. Varianten werden mit Chesslib geprüft und beim ersten illegalen Zug gekürzt.

**Undo/Neustart:** Zurückgenommene Züge werden auf einen Redo-Stapel gelegt, deren Analysen abgebrochen (laufende Suche:
`stop`) und verworfen. Analyseergebnisse sind an die Zug-ID gebunden; das Frontend verwirft Ergebnisse, deren Zug-ID nicht
mehr passt.

**Computerzug:** läuft außerhalb der Sitzungssperre; vor dem Übernehmen wird geprüft, ob Version und Stellung
unverändert sind und der Zug legal ist. Parallele identische Anfragen warten auf dieselbe Berechnung.

## Architekturentscheidungen

| Entscheidung | Begründung |
|---|---|
| Zwei Engine-Prozesse (play/analysis) | Computerzüge haben Vorrang und werden nie durch Coaching-Analysen blockiert oder verfälscht |
| Kandidaten über Folgestellungen statt MultiPV | Calvin 6.1.1 unterstützt weder MultiPV noch `searchmoves` (geprüft); so liegen P_u und P_b zudem unter identischen Suchbedingungen vor |
| Knotenlimits statt Zeitlimits für die Analyse | reproduzierbarer und hardwareunabhängiger; Zeitlimit nur für die Stufe „Maximal“ |
| Eigenes, unveränderliches Stellungsmodell für die Coaching-Logik | deterministische, engine- und bibliotheksunabhängige Regeln; alle Züge stammen trotzdem aus Chesslib (`MoveOracle`) |
| Asynchrone Analyse mit Statusabfrage | erste Rückmeldung ohne blockierende HTTP-Anfrage; Analyse abbrechbar |
| Positionsversion + Idempotenz | doppelte oder veraltete Anfragen verändern die Partie nicht (409 mit aktuellem Zustand) |
| Wiederherstellung über Zughistorie | der Server rekonstruiert die Stellung regelkonform; das Frontend vergleicht mit der gespeicherten FEN |
| Kein Rule-Code im Frontend | legale Züge, Schach, Partieende kommen ausschließlich vom Backend |

## Datenmodelle

Backend-Domainmodelle (Records): `MoveRecord`, `GameSnapshot` (GameState), `EngineAnalysis`, `MoveAnalysis`
(MoveAssessment), `CandidateEvaluation`, `CoachingFinding`, `CoachingTip`, `StrategicPlan`, `Score` (Typ CP/MATE).
Frontend-Typen: `frontend/src/models/*.ts`; Antworten werden zentral in `services/validate.ts` geprüft.

## Sicherheit und Datenschutz

- Feste Engine-Kommandos, keine Benutzereingaben in Prozessargumenten; UCI-Kommandos ohne Zeilenumbrüche.
- FEN, UCI-Züge und Parameter werden validiert; Ausgabezeilen der Engine in Länge und Anzahl begrenzt.
- Rate-Limit für rechenintensive POST-Anfragen (`chesscoach.api.rate-limit-per-minute`), begrenzte Analysewarteschlange,
  maximale Partienzahl und Leerlauf-Timeout.
- CORS nur für konfigurierte Ursprünge; im Profil `prod` ist Swagger-UI deaktiviert; keine Stacktraces in Antworten.
- Keine Telemetrie, keine externen Dienste; Partien werden serverseitig nicht dauerhaft gespeichert und nicht geloggt.
