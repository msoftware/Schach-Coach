# Engine-Integration

## Engine

| Feld | Wert |
|---|---|
| Engine | Calvin 6.1.1 (Dan Kelsey) – https://github.com/kelseyde/calvin-chess-engine |
| Datei | `backend/engine/calvin-chess-engine-6.1.1.jar` (offizielles Release-JAR) |
| SHA-256 | `dab9383b478e41718251da2cf738f4f95df2130eb2a42baffc07ede27bb405fe` – Gradle-Task `verifyEngine` (läuft vor jedem Testlauf) bzw. `downloadEngine` (lädt bei Fehlen nach) |
| Lizenz | MIT |
| Java | läuft mit derselben Java-21-Laufzeit wie das Backend; benötigt `--add-modules jdk.incubator.vector` |

Startkommando (fest, `CalvinChessEngine.command`):
`<java.home>/bin/java -Xmx512m -XX:+UseSerialGC --add-modules jdk.incubator.vector -jar <jar>`.
Calvin legt beim Start eine 256-MB-Hashtabelle an, bevor `setoption` greift; mit weniger als ca. 384 MB Heap bricht der Start
mit `OutOfMemoryError` ab (geprüft). Nach dem Handshake wird `Hash` auf 32 MB gesetzt.

## Abstraktion

`ChessEngine` (Pflichtschnittstelle: `start`, `getEngineInfo`, `analyze`, `findBestMove`, `stopAnalysis`, `shutdown`) mit
den Implementierungen `UciChessEngine` (generisch), `CalvinChessEngine` (Calvin-Besonderheiten) und `UnavailableEngine`
(Engine nicht startbar – meldet Fehler, simuliert nie). `ChessEngineFactory` wählt per `chesscoach.engine.type`
(`CALVIN` oder `UCI`). Stockfish kann als austauschbare Alternative über `type: UCI` und
`chesscoach.engine.uci.command: [/pfad/zu/stockfish]` eingebunden werden (GPL-3.0: Lizenzpflichten vor Weitergabe prüfen;
nicht mitgeliefert).

Es laufen **zwei Instanzen**: `play` (Computerzüge, Tipps) und `analysis` (Coaching). Suchen einer Instanz werden über einen
fairen Lock serialisiert.

## UCI-Dialog

1. `uci` → `id name`, `id author`, `option …`, `uciok`
2. `setoption name Hash value 32`, `setoption name Threads value 1` (nur angebotene Optionen)
3. vor jeder Suche: ungelesene Ausgabe verwerfen, `isready` → `readyok` (synchronisiert nach `stop`)
4. `position fen <fen>` und `go …`
5. `info …` wird von `EngineAnalysisParser` gelesen (depth, seldepth, score cp/mate inkl. lower/upperbound, nodes, time,
   pv, multipv); verwendet wird die letzte **exakte** Info-Zeile mit Hauptvariante
6. `bestmove <zug> [ponder …]`; `(none)`/`0000` → kein Zug

## Geprüfte Besonderheiten von Calvin 6.1.1

| Beobachtung (manuell und in `CalvinEngineIT` geprüft) | Umgang |
|---|---|
| Keine Option `MultiPV`, `searchmoves` wird ignoriert | Kandidaten werden über die Folgestellungen bewertet (`MoveAnalyzer`) |
| `go depth N` sucht nur bis Tiefe N−1; `go depth 1` liefert einen beliebigen Zug ohne Info | `CalvinChessEngine` sendet N+1 (Test `depthLimitIsTranslatedForCalvin`) |
| In Stellungen ohne legale Züge (Matt/Patt) oder bei ungültiger FEN liefert Calvin einen veralteten `bestmove` | solche Stellungen werden nie gesendet (Regelprüfung vorab); jeder `bestmove` wird gegen die legalen Züge geprüft |
| Sehr kurze Suchen enden gelegentlich ohne exakte Bewertung | Wiederholung mit vierfachem Knotenlimit; in der Vorauswahl wird der Zug notfalls ausgelassen |
| `go nodes N` ist ein weiches Limit (Überschreitung um einige Prozent) | dokumentiert; Grenzen dienen der Abstufung, nicht der exakten Messung |

## Suchparameter

Analyse (`chesscoach.analysis`, Profil NORMAL; FAST ×0,5, DEEP ×2; Vertiefung ×3 und +2 Kandidaten):

| Parameter | Standard |
|---|---|
| `root-nodes` (P₀) | 150 000 |
| `screening-nodes` (Vorauswahl je legalem Zug) | 3 000 |
| `candidate-nodes` (P_u, P_b und weitere Kandidaten) | 80 000, in kritischen Stellungen ×2 |
| `max-candidates` | 4 (+ gespielter und Engine-Bestzug) |
| `min-reliable-depth` | 9 |

Spielstärken (`chesscoach.difficulty.levels`, abrufbar über `GET /api/settings/options`):

| Stufe | Parameter |
|---|---|
| Anfänger | zufällige Auswahl von 25 % der legalen Züge (mind. 2), jeder Zug mit 200 Knoten bewertet, bester davon |
| Leicht | Auswahl von 50 % (mind. 3), 1 500 Knoten je Zug |
| Mittel | volle Suche, 20 000 Knoten |
| Stark | volle Suche, 250 000 Knoten |
| Maximal | volle Suche, 3 000 ms |

Die Abstufung ist durch `DifficultyAndAnalysisIT.difficultyLevelsAreGenuinelyGraded` belegt (sechs Taktikaufgaben,
normiert auf drei Durchläufe, Zufallsstartwert 42): Anfänger 2/18, Leicht 8/18, Mittel 18/18, Stark 18/18.
Eine Elo-Zahl wird nicht angegeben, da sie nicht empirisch ermittelt wurde.

## Fehlerbehandlung

| Fall | Verhalten |
|---|---|
| Engine startet nicht (Datei fehlt, Prozess stirbt, kein `uciok`) | Status `FAILED`; `POST /api/games` → 503 `ENGINE_UNAVAILABLE`; beim nächsten Aufruf neuer Startversuch |
| Timeout (Suchzeit + `search-grace` bzw. `max-search-time`) | `stop`, Wartezeit `search-grace`, danach Prozess beenden und neu starten; Fehler `ENGINE_TIMEOUT` |
| Absturz während der Suche | Fehler; nächster Aufruf startet den Prozess neu |
| Illegaler oder unlesbarer `bestmove` | Zug wird nicht ausgeführt, Prozess wird neu gestartet, Fehler `ENGINE_ERROR` |
| Unvollständige Ausgabe (keine Bewertung) | Analyse wird als unvollständig markiert bzw. wiederholt; nie als gesichert ausgegeben |
| Abbruch (Undo/Neustart) | `stop`, Ergebnis verworfen |

Getestet in `UciEngineProtocolTest` (Test-Engine als echter Prozess), `CalvinEngineIT` und `EngineUnavailableTest`.
