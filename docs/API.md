# REST-API (Version 1)

Basis-URL: `/api`. Alle Antworten sind JSON (UTF-8). Der maschinenlesbare Vertrag wird aus dem Code erzeugt:
`GET /v3/api-docs` (Laufzeit) bzw. [openapi.json](openapi.json) (wird von `GameFlowIT` beim Testlauf aktualisiert);
Swagger-UI unter `/swagger-ui.html` (nicht im Profil `prod`).

Konventionen:

- Züge werden als **UCI** übertragen (`e2e4`, Umwandlung `e7e8q`), **SAN** dient der Anzeige.
- Bewertungen haben immer einen Typ: `{"type":"CP"|"MATE","value":…,"display":"+0.35","perspective":"USER"}` und sind
  aus Sicht des Benutzers angegeben. `MATE` = Züge bis Matt (positiv: Benutzer setzt matt).
- Jede Änderung der Partie erhöht `positionVersion`. Schreibende Anfragen senden `expectedPositionVersion`.
- Keine Authentifizierung (keine Konten). `gameId` ist eine zufällige 128-Bit-Sitzungskennung.

## Endpunkte

| Methode | Pfad | Zweck |
|---|---|---|
| GET | `/api/health` | Gesundheitsstatus (`UP`/`DEGRADED`) |
| GET | `/api/engine/status` | Verfügbarkeit und Version beider Engine-Instanzen |
| POST | `/api/games` | Neue Partie starten oder lokal gespeicherte Partie wiederherstellen |
| GET | `/api/games/{gameId}` | Aktuelle Partie |
| POST | `/api/games/{gameId}/moves` | Benutzerzug |
| POST | `/api/games/{gameId}/engine-move` | Computerzug (synchron berechnet) |
| POST | `/api/games/{gameId}/undo` | Letzten eigenen Zug (und Computerantwort) zurücknehmen |
| POST | `/api/games/{gameId}/redo` | Zurückgenommene Züge wiederherstellen (Ergänzung) |
| POST | `/api/games/{gameId}/restart` | Partie neu starten |
| POST | `/api/games/{gameId}/resign` | Aufgeben (Ergänzung, FR-06) |
| POST | `/api/games/{gameId}/hint` | Tipp: bester Zug laut Engine (Ergänzung) |
| GET | `/api/games/{gameId}/analysis/{ply}` | Analyse eines Benutzerzuges |
| POST | `/api/games/{gameId}/analysis/{ply}[?force=true]` | Analyse anfordern/aktualisieren (202) |
| POST | `/api/games/{gameId}/analysis/{ply}/variations` | Kandidaten und Varianten vertiefen (202) |
| PUT | `/api/settings` | Spielstärke/Analyseprofil der laufenden Sitzung |
| GET | `/api/settings/options` | Spielstärken mit Engine-Parametern, Analyseprofile, Schwellenwerte |
| GET | `/api/coaching/rules` | Katalog der Coaching-Regeln |

## Beispiele

**Neue Partie**

```http
POST /api/games
{"playerColor":"WHITE","difficulty":"MEDIUM"}
```
```json
{"apiVersion":"1","gameId":"5a14901df98bc037e98db08267cc3cda","status":"IN_PROGRESS","playerColor":"WHITE",
 "sideToMove":"WHITE","fen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","positionVersion":1,
 "engineStatus":"READY","legalMoves":[{"uci":"e2e4","san":"e4","from":"e2","to":"e4","promotion":null}, "…"],
 "moves":[],"canUndo":false,"canRedo":false,"…":"…"}
```

**Wiederherstellung:** zusätzlich `"initialFen": "…", "moves": ["e2e4","e7e5", …]`. Der Server prüft jeden Zug; bei
einem illegalen Zug antwortet er mit `422 INVALID_RESTORE`.

**Benutzerzug** – Antwortvariante: zunächst `PENDING`, die Analyse läuft asynchron.

```http
POST /api/games/{gameId}/moves
{"uciMove":"e2e4","expectedPositionVersion":1}
```
```json
{"gameId":"…","positionVersion":2,"ply":1,"playedMove":"e2e4","san":"e4",
 "fen":"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1","status":"IN_PROGRESS",
 "analysisStatus":"PENDING","replayed":false,"game":{"…":"…"}}
```

Eine identische Wiederholung (gleiche erwartete Version, gleicher Zug) führt den Zug nicht erneut aus und liefert
`"replayed": true`.

**Analyse** (`GET /api/games/{gameId}/analysis/1`, gekürzt; Werte stammen aus einem echten Lauf):

```json
{"ply":1,"playedMove":"e4","status":"COMPLETED","positionVersion":2,"currentPositionVersion":2,"outdated":false,
 "classification":"BEST","classificationLabel":"Bestzug","explanationType":"BEST_MOVE",
 "mainReason":"Dein Bauer besetzt das Zentrumsfeld e4. …","mainReasonSources":["STRAT-002"],
 "scoreBefore":{"type":"CP","value":32,"display":"+0.32","perspective":"USER"},
 "scoreAfter":{"type":"CP","value":29,"display":"+0.29","perspective":"USER"},
 "evaluationLoss":{"centipawns":0,"winProbabilityLoss":0.0,"reliable":true},
 "bestMove":"e4","principalVariation":["e4","e5","Nf3","Nc6","Bb5","a6"],
 "candidates":[{"san":"Nf3","score":{"display":"+0.23"},"depth":17,"line":[{"numbered":"1.Nf3"},"…"],
                "motifs":[],"strategicPros":["Figur entwickelt"],"comparison":"Um 6 Centipawns schlechter als dein Zug.",
                "searchLine":true}],
 "findings":[{"ruleId":"STRAT-002","category":"STRATEGIC","source":"COACHING_RULE","severity":"LOW",
              "polarity":"POSITIVE","certainty":"HEURISTIC","title":"Zentrumskontrolle","explanation":"…",
              "affectedSquares":["d5"],"evidence":{"…":"…"},"score":2}],
 "sections":[{"key":"assessment","question":"Was war an meinem Zug gut oder schlecht?","statements":[{"text":"…","sources":["ENGINE"]}]}],
 "analysisConfidence":"HIGH","engine":{"name":"Calvin","version":"6.1.1","nodes":897644,"elapsedMillis":1111}}
```

Felder des Ergebnisses sind nur bei `status = COMPLETED` gesetzt (sonst `null`); bei `PENDING`/`RUNNING` liefern
`stage` und `progressPercent` den Fortschritt. `FAILED` enthält eine verständliche `errorMessage`.

## Fehlerformat (RFC 9457)

```json
{"type":"urn:chess-coach:error:illegal-move","title":"Ungültiger Zug","status":422,
 "detail":"Der angeforderte Zug ist in der aktuellen Stellung nicht legal.","code":"ILLEGAL_MOVE",
 "positionVersion":7,"currentState":{"…":"aktuelle Partie"}}
```

| Code | Status | Bedeutung |
|---|---|---|
| `VALIDATION_FAILED` | 400 | ungültige Anfrage |
| `GAME_NOT_FOUND`, `ANALYSIS_NOT_FOUND` | 404 | unbekannte Partie bzw. keine Analyse |
| `POSITION_VERSION_CONFLICT` | 409 | veraltete Version; `currentState` enthält den aktuellen Zustand |
| `NOT_YOUR_TURN`, `GAME_FINISHED`, `ENGINE_BUSY`, `NOTHING_TO_UNDO`, `NOTHING_TO_REDO`, `ANALYSIS_NOT_AVAILABLE` | 409 | Aktion im aktuellen Zustand nicht möglich |
| `ILLEGAL_MOVE`, `INVALID_FEN`, `INVALID_RESTORE` | 422 | fachlich ungültige Eingabe |
| `RATE_LIMITED` | 429 | zu viele rechenintensive Anfragen |
| `ENGINE_ERROR` | 502 | ungültige Engine-Ausgabe (Zug wurde nicht ausgeführt) |
| `ENGINE_UNAVAILABLE`, `TOO_MANY_GAMES` | 503 | Engine nicht verfügbar / Kapazität erreicht |
| `ENGINE_TIMEOUT` | 504 | Engine antwortete nicht rechtzeitig |
| `INTERNAL_ERROR` | 500 | unerwarteter Fehler (ohne interne Details) |
