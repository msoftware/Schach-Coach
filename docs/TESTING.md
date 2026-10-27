# Teststrategie

Schachregeln, Engine-Integration und Erklärungsqualität werden getrennt abgesichert. Alle Tests laufen ohne externe
Schachdienste; Engine-Tests verwenden die mitgelieferte Calvin-Datei.

## Backend (`cd backend && ./gradlew check checkLicense`)

| Bereich | Tests |
|---|---|
| Schachregeln | `ChessRulesServiceTest`: legale/illegale Züge, SAN inkl. `#`, Rochade (auch durch Schach verboten), En passant, Umwandlung in alle Figuren, Matt, Patt, unzureichendes Material, dreifache Wiederholung, 50-Züge-Regel, Rücknahme per Replay, FEN-Validierung, Variantenprüfung |
| Stellungsmodell | `PositionModelTest`: FEN, Feldfarben (h1 hell), Angriffe, Rochade/En passant/Umwandlung, statische Abtauschbewertung, Bauernstruktur, Fesselung/Spieß, Spielphasen |
| Engine-Protokoll | `EngineAnalysisParserTest`; `UciEngineProtocolTest` mit einer Test-Engine als echtem Prozess: Handshake, `go`/`stop`, Teilergebnis nach `stop`, Timeout mit Neustart, Absturz und Neustart, illegaler/unlesbarer `bestmove`, fehlende Bewertung, fehlendes `uciok`, nicht vorhandenes Programm, keine Suche in Endstellungen, keine Zeilenumbrüche in Kommandos |
| Calvin (Tag `engine`) | `CalvinEngineIT`: Identität/Version, legaler Bestzug, Tiefenumrechnung, Mattbewertung, Perspektive der Bewertung, `stop` |
| Coaching Ebene 1 | `TacticalRulesTest`: jede Regel TACT-001…016 mit positiven Fällen, Gegenfällen und Prüfung von Feldern, Gewichtung, Beweiskette |
| Coaching Ebene 2 | `StrategicRulesTest`: jede Regel STRAT-001…012 inkl. Gegenfällen (z. B. nicht rochierter König wird nicht pauschal kritisiert) |
| Coaching Ebene 3 | `MistakeClassifierTest` (Schwellen, Matt, Swing, Dämpfung, erzwungen, unsicher, Perspektivenumrechnung); `ExplanationQualityTest` (kuratierter Katalog, s. u.) |
| Pipeline/Spielstärke (Tag `engine`) | `DifficultyAndAnalysisIT`: echte Abstufung der Stufen, Analyse eines groben Fehlers und eines guten Zuges, nur legale Varianten, Abbruch |
| API (Tag `engine`) | `GameFlowIT`: Partie starten, Zug, Idempotenz, Analyse der richtigen Stellung/Version, Computerzug, Versionskonflikt (409 + aktueller Zustand), illegaler Zug (422), Undo verwirft Analyse, Redo, veraltete Analysen nach Einstellungsänderung, Aufgabe, Wiederherstellung, Matt, Validierungsfehler, Health/Engine-Status/Regelkatalog, OpenAPI-Pfade (schreibt `docs/openapi.json`) |
| Engine-Ausfall | `EngineUnavailableTest`: Spielstart wird mit 503 abgelehnt, Status `FAILED`/`DEGRADED`, Rate-Limit |

Engine-Tests lassen sich mit `./gradlew test -PskipEngineTests` ausschließen.

**Abdeckung:** JaCoCo-Regel in `check`: mindestens 80 % Line Coverage für `de.chesscoach.coaching*` und
`de.chesscoach.domain.position*`. Letzter Lauf: `coaching` 88,9 %, `coaching.tactical` 84,9 %, `coaching.strategic` 85,4 %,
`domain.position` 93,5 % (Bericht: `backend/build/reports/jacoco/test/html`). 100 % der Regeln haben eigene Tests.

### Katalog für Erklärungsqualität (Requirements 13.5)

`ExplanationQualityTest` enthält kuratierte Stellungen mit FEN, Variante (Testdaten), erwarteter Klasse, erwartetem
Erklärungstyp, erwarteten Regel-IDs, verbotenen negativen Regel-IDs sowie zulässigen und verbotenen Aussagen.
Für jede Stellung gelten zusätzlich die Invarianten:

- keine als betroffen genannte Figur, die nicht auf dem Feld steht;
- Beweisketten nur aus legalen Zügen;
- kein Materialgewinn ohne Bestätigung durch die Variante;
- keine positive strategische Heuristik als Hauptbegründung bei nachgewiesener taktischer Widerlegung;
- Heuristiken ohne Formulierungen wie „erzwingt“/„zwingend“; unsichere Analysen als „vorläufig“ gekennzeichnet.

## Frontend (`cd frontend && npm run typecheck && npm test`)

| Datei | Inhalt |
|---|---|
| `utils/fen.spec.ts` | Figurenstellung, Brettorientierung (Weiß unten, h1 hell), deutsche Figurenbezeichnungen |
| `components/ChessBoard.spec.ts` | SVG-Figuren mit Alternativtext, Auswahl und Zielfelder, Zug per Klick, ungültige Züge, nicht interaktiver Zustand, Umwandlungsauswahl, Kennzeichnung letzter Zug/Schach, Tastaturbedienung, gedrehtes Brett |
| `components/coaching.spec.ts` | Zusammenfassung (Klasse, Begründung, Alternative, Bewertung), laufende/unsichere/veraltete/fehlgeschlagene Analysen, Kandidaten, Variantenanzeige, Zugliste |
| `stores/stores.spec.ts` | Partiestart und lokale Speicherung, Ablauf Zug → Analyse → Computerzug, Versionskonflikt, Backend nicht erreichbar, Wiederherstellung mit FEN-Vergleich, Undo verwirft Analysen, Varianten ändern die Partie nicht, ehrliche Meldung bei fehlgeschlagener Speicherung, Analyse-Cache |
| `services/validate.spec.ts` | zentrale Validierung der API-Antworten |

## End-to-End (`cd frontend && npx playwright test`)

Voraussetzung: `backend/build/libs/chess-coach-backend.jar` (`./gradlew bootJar`). Playwright startet Backend und Vite
selbst (bzw. verwendet laufende Server). Projekte: Chromium, Firefox, mobil (Pixel 7).

1. Anwendung öffnen und neue Partie starten · 2. legalen Zug spielen · 3. auf das Coaching warten ·
4. Erklärung öffnen und Kandidatenvariante abspielen (Brett unverändert danach) · 5. Computerzug ·
6. letzten Zug zurücknehmen · 7. neu laden und wiederherstellen (`game.spec.ts`) · 8. Partie bis zum Matt spielen und
Ergebnis prüfen · Bauernumwandlung · mobile Darstellung ohne horizontales Scrollen und Touch-Bedienung
(`responsive.spec.ts`).

## Ergebnisse des letzten vollständigen Laufs

Siehe Abschnitt „Verifikation“ in der Abschlussübersicht des Projekts; Stand dieses Dokuments:
Backend `check checkLicense` erfolgreich, Frontend 31 Vitest-Tests erfolgreich, Playwright 7/7 (Chromium, Firefox, mobil).
Safari wurde nicht getestet (keine macOS-Umgebung verfügbar).

## Referenzmessung (NFR-03)

Referenzsystem: AMD Ryzen AI Max+ PRO 395, 96 GB RAM, Windows 11, Oracle JDK 21.0.10, Standardkonfiguration
(Profil NORMAL, eine Analyse-Engine mit einem Thread). Zeit von Analysebeginn bis Ergebnis (`engine.elapsedMillis`):

| Zug | Klasse | Zeit | Knoten |
|---|---|---|---|
| 1.e4 (Grundstellung) | Bestzug | 1 111 ms | 897 644 |
| 3.Nxe5 (nach 1.e4 e5 2.Nf3 Nc6) | Grober Fehler (kritisch, doppelte Kandidatensuche) | 2 750 ms | 1 457 860 |
| 7.Bg5 (Damengambit-Stellung) | Gut | 1 775 ms | 1 280 152 |
| 5.O-O (Italienisch) | Bestzug | 1 260 ms | 705 192 |

Das Ziel „erste Coaching-Rückmeldung in höchstens fünf Sekunden“ wird auf dem Referenzsystem erreicht. Die erste Analyse
nach dem Start kann wegen JIT-Aufwärmphase einige hundert Millisekunden länger dauern.
