# Requirements-Dokument: Schach-Coaching-Anwendung

Version 1.0 · Technische Spezifikation für einen Coding Agenten · Sprache der Anwendung: Deutsch

## 1. Projektziel

Entwickelt werden soll eine browserbasierte Schach-Anwendung, in der ein Benutzer gegen einen Computer spielt und nach jedem eigenen Zug eine ausführliche, nachvollziehbare Analyse erhält.

Die Anwendung soll nicht nur mitteilen, ob ein Zug gut oder schlecht war. Sie muss insbesondere erklären, warum ein Zug bewertet wurde, welche taktischen und strategischen Folgen er hat, welche Alternativen zur Verfügung standen und wie der Spieler daraus lernen kann.

Das System besteht aus:

- Frontend: Vue 3, Quasar und TypeScript.
- Backend: Java mit Spring Boot.
- Schach-Engine: eine eingebundene, lokal ausgeführte Java-Engine mit UCI-Unterstützung.
- Coaching-System: eigenständige Java-Komponenten mit regelbasierter Analyse.
- Speicherung: ausschließlich im Browser, ohne Benutzerkonten und ohne persistente serverseitige Partiedaten.
- Darstellung: klassische Schachfiguren als SVG-Grafiken.
- Dokumentation und Qualitätssicherung: automatisierte Tests, Installationsanleitung, Architektur- und API-Dokumentation.

Das Werk Chess Strategy von Edward Lasker dient als fachliche Grundlage für die strategischen Coaching-Regeln. Das Buch selbst wird weder in der Anwendung eingebettet noch zur Laufzeit geladen. Seine relevanten Prinzipien werden als eigenständige, testbare Regeln implementiert.

## 2. Grundlegende Architekturentscheidungen

Vue 3 + Quasar

Schachbrett · Spielsteuerung · Zugliste · Analyseansicht · Einstellungen

REST API · JSON

Spring Boot Backend

Spielverwaltung · Schachregeln · Analyse-Orchestrierung · Coaching-Logik

Schachbibliothek

Legale Züge, FEN, SAN, Spielzustand, Matt und Remis

UCI-Schach-Engine

Stellungsbewertung, beste Züge, Varianten und Computerzüge

Coaching-Pipeline

Taktik und Regeln → strategische Bewertung → didaktische Erklärung

### 2.1 Verbindliche technische Entscheidungen

1. Das Frontend darf keine eigenen Schachregeln implementieren, die von den Regeln des Backends abweichen könnten.
2. Die Schach-Engine muss im ausgelieferten Projekt enthalten und lokal ausführbar sein. Ein externer Cloud-Schachdienst ist nicht zulässig.
3. Der Computerzug wird durch die Engine berechnet. Die Coaching-Logik verwendet dieselbe Engine für die Analyse, ist aber eine eigenständige Implementierung.
4. Das Backend verwaltet die laufende Partie während der Sitzung im Arbeitsspeicher. Nach einem Reload kann das Frontend die Partie aus dem lokalen Browser-Speicher wiederherstellen.
5. Das Backend darf keine dauerhafte Speicherung von Partien, Spielhistorien oder Benutzerprofilen voraussetzen.
6. Strategische und didaktische Aussagen müssen auf konkreten Merkmalen der Stellung, Engine-Varianten oder expliziten Coaching-Regeln beruhen.
7. Die Anwendung darf keine erfundenen taktischen Drohungen, Varianten oder Stellungsbewertungen als Tatsachen darstellen.

## 3. Bibliotheks- und Technologieauswahl

Die Anwendung soll vorhandene, ausreichend ausgereifte Bibliotheken nutzen. Eigenentwicklungen sind auf die fachliche Coaching-Logik, die Integration und die Benutzerführung zu konzentrieren.

### 3.1 Schach-Engine und Schachbibliothek

[Justin Fagnani (@justinfagnani) on X](https://images.openai.com/static-rsc-4/PHiJhURqpvEdESnx7LBUtdll42DqdzzI4cgxOe3EgIHWzRJPEF2hXjnlU1ZFDjndVJtALHCkT3SPdCy4K6dMqB9G9tOtaCmmU9z2_pxmRK3Nk-7R_uYtEos5tXKFGoVwSKkJTTt4Xhpd-NgyLFPqxNP09866RAi48qgCPAUvSG4?purpose=inline)

### 1. Calvin Chess Engine – bevorzugter Kandidat

Java · UCI · MIT-Lizenz laut Repository

[GitHub: kelseyde/calvin-chess-engine](https://github.com/kelseyde/calvin-chess-engine)

Calvin ist eine vollständig in Java entwickelte Schach-Engine mit Alpha-Beta-Suche, iterativer Vertiefung, Transpositionstabelle und neuronaler Stellungsbewertung. Sie unterstützt UCI und kann als eigenständige JAR-Datei gestartet werden.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub

+1



Entscheidung: Als erste Engine-Implementierung verwenden, sofern der Coding Agent die Kompatibilität, Leistungsfähigkeit und Lizenz der konkret festgelegten Version verifiziert. Calvin benötigt laut Dokumentation Java 17 oder neuer sowie bei den entsprechenden Releases das Modul `jdk.incubator.vector`.

[Play Chess Against the Computer – Stockfish 18 Engine, All Levels](https://images.openai.com/static-rsc-4/NfqsKk4yVCURBpNPJTYl0zcFVdw0akDb7bJS8wyW6c5VHDVzKlXpO3Dj2pTpsmELWl1pwFxYCyyxZn87nE3gmzP7zja8ErVmq6U4CFJeDfX2uJJ8vHJudJl0b5N3SM6GHHEqKTih6RE7zDFVr_EpW7osQW6B3fj1Ke791eeqMrw?purpose=inline)

### 2. Stockfish – leistungsstarke Alternative

C++ · UCI · GPL-3.0

[GitHub: official-stockfish/Stockfish](https://github.com/official-stockfish/Stockfish?utm_source=chatgpt.com)

Stockfish ist eine etablierte, sehr starke Schach-Engine mit UCI-Schnittstelle. Die offizielle Distribution steht unter GPL-3.0. Die Weitergabe der Engine unterliegt deshalb Lizenzpflichten, insbesondere hinsichtlich Lizenztext und Quellcode beziehungsweise des vorgeschriebenen Quellcodezugangs.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub

+1



Entscheidung: Als austauschbare Alternative unterstützen. Bei Verwendung sind die GPL-Anforderungen vor der Distribution rechtlich zu prüfen.

[Tabula Chess](https://images.openai.com/static-rsc-4/AFlRXi7UTW50vcKQvxMerFSuvjl6JMcGnPvYeqAx3Y0X7hagojo4Yrfnz9HgwvbfvTj9AkTcgS57Ti6ZsN-pnKFPOCdFRBJak20g4-HKdLMZdNTw1fWYTFMlLxpWscc5_jBJiTtLjV5n0wzY_Pmp1KksDleBbpLnorztsuS9f9M?purpose=inline)

### 3. Chesslib – Regel- und Brettverwaltung

Java · Apache-2.0 laut Repository

[GitHub: bhlangonijr/chesslib](https://github.com/bhlangonijr/chesslib?utm_source=chatgpt.com)

Chesslib bietet Funktionen für legale Züge, Stellungsvalidierung sowie FEN- und PGN-Verarbeitung. Damit kann die Anwendung auf eine geprüfte Schachregelimplementierung zurückgreifen.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub

+1



Entscheidung: Für die Verwaltung der Partie und die Validierung der Züge einsetzen, sofern die API der festgelegten Version die benötigten Funktionen vollständig abdeckt.

### 3.2 Integrationsanforderungen

Der Coding Agent muss vor der endgültigen Auswahl:

- die aktuelle stabile Version und Lizenz jeder Bibliothek dokumentieren;
- die Verfügbarkeit über Maven Central oder ein anderes reproduzierbares Repository prüfen;
- die tatsächliche Java-Kompatibilität mit der gewählten Spring-Boot-Version testen;
- die Erkennung von Schach, Matt, Patt, Rochade, En passant, Bauernumwandlung und Remis überprüfen;
- den UCI-Dialog der Engine automatisiert testen;
- alle Lizenztexte und erforderlichen Hinweise im Projekt mitliefern.

Die Architektur darf nicht direkt an Calvin-spezifische Klassen gekoppelt sein.

Verbindliche Schnittstelle:

```
public interface ChessEngine {

    void start();

    EngineInfo getEngineInfo();

    EngineAnalysis analyze(
        String fen,
        AnalysisLimits limits
    );

    EngineMove findBestMove(
        String fen,
        EngineLimits limits
    );

    void stopAnalysis();

    void shutdown();
}
```

Die Signaturen dienen als Architekturvorgabe. Der Coding Agent darf sie an die konkrete asynchrone Implementierung anpassen, muss aber dieselben Verantwortlichkeiten erhalten.

Zusätzlich sind folgende Komponenten erforderlich:

- `CalvinChessEngine`: UCI-Integration für Calvin.
- `UciProcessManager`: Start, Überwachung und Beendigung des Engine-Prozesses.
- `ChessEngineFactory`: Auswahl der konfigurierten Engine.
- `ChessRulesService`: Adapter für Chesslib.
- `EngineAnalysisParser`: Verarbeitung von Bewertung, Tiefe, Varianten und `bestmove`.

Die Engine muss beim Start initialisiert werden. Bei nicht verfügbarer Engine darf das Backend keine Computerzüge oder Analysen simulieren, sondern muss einen verständlichen Fehler melden.

### 3.3 Weitere empfohlene Technologien

| Bereich          | Technologie                   | Zweck                                               |
| ---------------- | ----------------------------- | --------------------------------------------------- |
| Frontend         | Vue 3, Quasar, TypeScript     | Benutzeroberfläche                                  |
| Build Frontend   | Vite                          | Entwicklung und Produktionsbuild                    |
| Frontend-Tests   | Vitest, Vue Test Utils        | Komponenten- und Logiktests                         |
| Browser-Tests    | Playwright                    | End-to-End-Tests                                    |
| Backend          | Java, Spring Boot             | REST API und Anwendungslogik                        |
| API-Vertrag      | OpenAPI 3                     | Dokumentierte Schnittstellen                        |
| Backend-Tests    | JUnit 5, Mockito              | Unit- und Integrationstests                         |
| Regelprüfung     | Chesslib                      | Schachregeln und Stellungsverwaltung                |
| Engine           | Calvin, austauschbar über UCI | Zugberechnung und Bewertung                         |
| Frontend-Zustand | Pinia                         | Partie und UI-Zustand                               |
| Speicherung      | IndexedDB                     | Partien, Einstellungen und Lernstatistik im Browser |
| Deployment       | Docker Compose                | Lokaler und reproduzierbarer Betrieb                |

Die konkreten Versionsnummern sind im Build zu fixieren. Der Coding Agent muss eine konsistente, unterstützte Versionskombination auswählen und in der Projektdokumentation festhalten.

## 4. Funktionale Anforderungen

Die Anforderungen sind als überprüfbare Muss-Anforderungen formuliert.

### FR-01: Neue Partie starten

Der Benutzer kann jederzeit eine neue Partie beginnen.

- Standardstellung ist die reguläre Ausgangsstellung.
- Der Benutzer spielt standardmäßig Weiß.
- Der Computer spielt Schwarz.
- Eine neue Partie setzt Brett, Zugliste, Analysezustand und Ergebnis zurück.
- Vor dem Verwerfen einer laufenden Partie muss bei Bedarf eine Bestätigung angezeigt werden.
- Ein Neustart darf keine vorherige Partie unbemerkt überschreiben, wenn diese gespeichert werden soll.

### FR-02: Schachbrett und Zugausführung

- Das Brett wird mit klassischen SVG-Schachfiguren dargestellt.
- Weiße Figuren stehen in der Standardorientierung unten.
- Das Feld h1 ist hell und a1 dunkel.
- Alle legalen Züge werden zuverlässig verarbeitet.
- Bei Auswahl einer Figur werden legale Zielfelder hervorgehoben.
- Ungültige Züge werden verhindert.
- Bauernumwandlung ermöglicht die Auswahl von Dame, Turm, Läufer oder Springer.
- Rochade, En passant, Schach, Schachmatt, Patt und Remis werden regelkonform behandelt.
- Die Zugliste verwendet korrekte algebraische Notation (SAN).
- Der Benutzer kann Züge zurücknehmen, erneut vorwärts navigieren und die Partie neu starten.
- Brettzustand, Zugliste und Analyse müssen stets dieselbe Stellung repräsentieren.

### FR-03: Computergegner

- Der Computer führt ausschließlich legale Züge aus.
- Der Computerzug wird mit der integrierten Schach-Engine berechnet.
- Die Spielstärke kann vor einer Partie ausgewählt werden.
- Die Einstellungen umfassen mindestens Anfänger, leicht, mittel, stark und maximale Engine-Stärke.
- Die Stufen müssen durch überprüfbare Engine-Parameter definiert werden, beispielsweise Suchzeit, Suchtiefe oder Knotenzahl.
- Die Oberfläche zeigt an, wenn der Computer rechnet.
- Während der Berechnung dürfen widersprüchliche Spielzüge nicht angenommen werden.
- Nach einem Computerzug wird der Zugzustand aktualisiert und die Partie bei Bedarf beendet.

Die Spielstärke-Stufen dürfen nicht lediglich unterschiedliche Bezeichnungen für dieselbe Engine-Konfiguration sein. Eine echte Abstufung muss durch Tests belegt werden. Eine feste Elo-Zahl darf nicht versprochen werden, wenn sie nicht empirisch abgesichert ist.

### FR-04: Coaching nach jedem eigenen Zug

Nach jedem legalen Zug des Benutzers muss automatisch eine Analyse ausgelöst werden.

Die Analyse muss mindestens enthalten:

1. Bewertung des tatsächlich gespielten Zuges.
2. Vergleich mit der Bewertung der Stellung vor dem Zug.
3. Besten alternativen Zug.
4. Mindestens zwei sinnvolle Kandidatenzüge, sofern die Stellung dies zulässt.
5. Konkrete taktische Folgen und mögliche gegnerische Antworten.
6. Strategische Bewertung anhand der integrierten Regelbasis.
7. Verständliche Begründung für jede wesentliche Bewertung.
8. Konkrete Empfehlung für zukünftige ähnliche Stellungen.

Die Coaching-Ausgabe muss unterscheiden, welche Erkenntnisse von der Schach-Engine berechnet und welche von der eigenen Java-Coaching-Logik abgeleitet wurden.

### FR-05: Detailanalyse

Jede Analyse enthält eine übersichtliche Zusammenfassung und eine ausführliche Detailansicht.

Die Zusammenfassung zeigt:

- gespielten Zug;
- Qualitätsklasse;
- wichtigste Begründung;
- beste Alternative;
- relevante Veränderung der Stellungsbewertung.

Die Detailansicht zeigt:

- Bewertung vor und nach dem Zug;
- Verlust gegenüber dem besten Zug, soweit zuverlässig berechenbar;
- beste Alternative mit Variante;
- weitere Kandidatenzüge;
- taktische Motive;
- Materialveränderungen;
- strategische Regelverstöße;
- langfristige Pläne für beide Seiten;
- konkrete Erklärungen und Lernhinweise.

Bei taktisch kritischen Stellungen muss die Engine-Analyse mit ausreichender Tiefe oder Suchzeit durchgeführt werden. Die Anwendung darf eine oberflächliche Analyse nicht als endgültige Wahrheit darstellen.

### FR-06: Partieende

Die Anwendung muss erkennen und korrekt darstellen:

- Schachmatt;
- Patt;
- Remis durch unzureichendes Material;
- Remis durch Stellungswiederholung;
- Remis nach den geltenden Zugregeln;
- freiwilliges Aufgeben durch den Benutzer.

Nach Partieende darf kein weiterer Zug ausgeführt werden. Die Partieanalyse bleibt verfügbar.

### FR-07: Lokale Speicherung

Es gibt keine Benutzerkonten und keine serverseitige dauerhafte Partienspeicherung.

Der Browser speichert lokal:

- aktuelle Partie;
- Zugliste;
- Einstellungen;
- abgeschlossene Partien;
- optionale Lernstatistik.

IndexedDB ist für strukturierte Partiedaten zu verwenden. Einstellungen können alternativ in `localStorage` liegen.

Die Anwendung muss bei nicht verfügbarer lokaler Speicherung verständlich reagieren. Es darf nicht behauptet werden, dass Daten dauerhaft gesichert wurden, wenn das Speichern fehlgeschlagen ist.

### FR-08: Datenschutz

- Es werden keine Konten benötigt.
- Es werden keine Partien an externe Dienste gesendet.
- Die Schach-Engine läuft lokal im Backend.
- Es gibt keine verpflichtenden Analyse- oder Telemetriedienste von Drittanbietern.
- Die Datenschutzhinweise müssen den tatsächlichen Betrieb korrekt beschreiben.
- Der Browser-Speicher kann vom Benutzer geleert werden.
- Fehlerprotokolle dürfen keine vollständigen Partien oder sonstigen Nutzerdaten unkontrolliert übertragen.

### FR-09: Bedienbarkeit

- Die Anwendung ist auf Desktop und Tablet benutzbar und auf mobilen Geräten sinnvoll bedienbar.
- Die Partie kann vollständig per Maus und Touch gespielt werden.
- Alle Schachfiguren und relevanten Bedienelemente müssen ausreichend kontrastreich sein.
- Letzter Zug, ausgewählte Figur, legale Zielfelder und Schachgebote müssen visuell erkennbar sein.
- Analyse und Zugliste müssen auch bei langen Partien navigierbar bleiben.
- Die Benutzeroberfläche ist vollständig auf Deutsch.
- Die Anwendung benötigt keinen Login und keine Konfiguration externer Dienste.

## 5. Fachliche Coaching-Architektur: Ebenen 1–3

Die Coaching-Logik ist der zentrale Eigenentwicklungsanteil. Sie muss unabhängig von der Engine, deterministisch testbar und erweiterbar sein.

### 5.1 Ebene 1 – Schachregeln und Taktik

Ziel: Objektiv erkennbare taktische und regelbezogene Konsequenzen eines Zuges identifizieren.

Die Analyse muss mindestens folgende Prüfungen umfassen:

| Regel-ID | Analysebereich     | Zu erkennende Situation                                                                                      |
| -------- | ------------------ | ------------------------------------------------------------------------------------------------------------ |
| TACT-001 | Materialgewinn     | Eine Figur oder ein Bauer kann gewonnen werden.                                                              |
| TACT-002 | Materialverlust    | Der Zug ermöglicht einen relevanten Materialverlust.                                                         |
| TACT-003 | Ungedeckte Figuren | Eine Figur ist angegriffen und unzureichend verteidigt.                                                      |
| TACT-004 | Figurenangriffe    | Der Zug erzeugt oder ignoriert einen konkreten Angriff.                                                      |
| TACT-005 | Doppelangriff      | Eine Figur bedroht mehrere relevante Ziele gleichzeitig.                                                     |
| TACT-006 | Fesselung          | Eine Figur kann aufgrund eines Angriffs auf eine höherwertige Figur oder den König nicht frei ziehen.        |
| TACT-007 | Spieß              | Eine wertvolle Figur wird angegriffen und muss ausweichen, wodurch dahinterliegendes Material gefährdet ist. |
| TACT-008 | Abzugsangriff      | Durch das Wegziehen einer Figur wird eine dahinterstehende Angriffslinie geöffnet.                           |
| TACT-009 | Zwischenzug        | Ein taktisch relevanter Zwischenzug kann die unmittelbare Reaktion übertreffen.                              |
| TACT-010 | Mattdrohung        | Ein Zug erzeugt eine konkrete, nachweisbare Mattdrohung.                                                     |
| TACT-011 | Mattvermeidung     | Ein Zug übersieht eine unmittelbare oder erzwungene Mattgefahr.                                              |
| TACT-012 | Königssicherheit   | Ein Zug öffnet gefährliche Linien zum eigenen König.                                                         |
| TACT-013 | Verteidigung       | Eine kritische Verteidigungsfigur wird entfernt oder überlastet.                                             |
| TACT-014 | Freibauer          | Ein Bauer kann unter Berücksichtigung gegnerischer Antworten zur Umwandlung gelangen.                        |
| TACT-015 | Gabel              | Eine Figur greift zwei oder mehr relevante Ziele gleichzeitig an.                                            |
| TACT-016 | Abwicklung         | Ein erzwungener Abtausch verändert Material oder Stellung wesentlich.                                        |

Nicht jede Regel muss ausschließlich durch einen einfachen Angriffstest entschieden werden. Taktische Motive müssen anhand legaler Züge und – sofern nötig – durch Engine-Varianten bestätigt werden.

Anforderung: Jede erkannte Taktik enthält eine konkrete Beweiskette, zum Beispiel:

- Ausgangsstellung;
- auslösender Zug;
- gegnerische Antwort;
- Folgezüge;
- resultierende Materialbilanz oder Mattfolge;
- Konfidenz der Erkennung.

Eine mögliche Regeldefinition:

```
public interface TacticalRule {

    String id();

    boolean matches(PositionContext context);

    TacticalFinding evaluate(PositionContext context);
}
```

`TacticalFinding` enthält nicht nur einen Text, sondern strukturierte Daten wie Motiv, beteiligte Felder, betroffene Figuren, Varianten und Begründung.

### 5.2 Ebene 2 – Strategische Prinzipien nach Edward Lasker

Ziel: Die langfristige Qualität eines Zuges beurteilen. Dabei sollen die Ideen des Buches Chess Strategy by Edward Laskerin in konkrete, nachvollziehbare Prüfungen übersetzt werden. Das Buch kann online unter https://www.gutenberg.org/cache/epub/5614/pg5614-images.html gefunden werden.

#### STRAT-001: Entwicklung der Figuren

Prüfen:

- Wie viele Leichtfiguren sind entwickelt?
- Werden bereits entwickelte Figuren wiederholt gezogen, obwohl andere Figuren noch unentwickelt sind?
- Unterstützt der Zug die Entwicklung?
- Verliert der Spieler durch den Zug Entwicklungsvorsprung?

Begründung: Der Coach muss erklären, ob der Zug die Einsatzbereitschaft der Figuren verbessert oder den Gegner schneller entwickeln lässt.

#### STRAT-002: Zentrumskontrolle

Prüfen:

- Kontrolle und Besetzung der zentralen Felder;
- Einfluss auf zentrale Bauernhebel;
- Möglichkeit, gegnerische Zentralbauern zu blockieren oder anzugreifen;
- Auswirkungen des Zuges auf die Kontrolle des Zentrums.

Begründung: Eine bloße Anzahl kontrollierter Felder reicht nicht. Der Coach muss relevante zentrale Felder und konkrete Figuren oder Bauern benennen.

#### STRAT-003: Königssicherheit

Prüfen:

- Stellung des Königs und Rochademöglichkeiten;
- offene und halboffene Linien in Königsnähe;
- verfügbare Verteidiger;
- konkrete Angriffsmöglichkeiten;
- Gefahren durch gelockerte Bauernstellung.

Begründung: Die Erklärung muss konkrete Angriffslinien und Angreifer nennen. Ein nicht rochierter König ist nicht automatisch schlecht gestellt.

#### STRAT-004: Figurenaktivität und Mobilität

Prüfen:

- Anzahl sinnvoller legaler Züge;
- aktive und passive Figuren;
- blockierte Läufer und Türme;
- besetzte offene Linien;
- kontrollierte Felder und mögliche Eindringfelder.

Begründung: Der Coach soll erklären, welche Figur eingeschränkt ist und welche Verbesserung möglich wäre.

#### STRAT-005: Bauernstruktur

Prüfen:

- isolierte Bauern;
- verdoppelte Bauern;
- rückständige Bauern;
- hängende Bauern;
- schwache Felder;
- Bauernmehrheiten;
- Freibauern;
- dauerhaft angreifbare Bauern.

Begründung: Die Erklärung muss die konkrete Struktur, die Angriffsrichtung und mögliche langfristige Folgen nennen.

#### STRAT-006: Schwache Felder und Vorposten

Prüfen:

- Felder, die von gegnerischen Bauern nicht mehr angegriffen werden können;
- stabile Vorposten für Springer;
- Felder vor rückständigen Bauern;
- mögliche Eindringfelder für Schwerfiguren.

Begründung: Es muss klar werden, welche Figur ein Feld nutzen könnte und warum es schwer zu vertreiben ist.

#### STRAT-007: Offene Linien und Diagonalen

Prüfen:

- offene und halboffene Linien;
- Kontrolle dieser Linien;
- Blockaden;
- offene Diagonalen für Läufer;
- mögliche Invasion auf der siebten oder achten Reihe.

Begründung: Der Coach muss den strategischen Nutzen eines Turm- oder Läuferzugs in Bezug auf die konkrete Stellung erklären.

#### STRAT-008: Material und positionelle Kompensation

Prüfen:

- Materialbilanz;
- Figurenwerte als grobe Vergleichsgröße;
- positionelle Kompensation für Materialopfer;
- Entwicklungsvorsprung;
- Initiative;
- Königssicherheit;
- Freibauern.

Begründung: Ein Materialnachteil darf nicht automatisch als schlechter Zug gewertet werden, wenn die Engine eine ausreichende Kompensation erkennt.

#### STRAT-009: Figurenabtausch

Prüfen:

- Welche Figuren werden getauscht?
- Welche Seite profitiert vom Abtausch?
- Bleiben günstige oder ungünstige Läufer erhalten?
- Entstehen bessere oder schlechtere Endspielstrukturen?
- Wird ein aktiver Angreifer entfernt oder ein wichtiger Verteidiger beseitigt?

Begründung: Der Coach muss erklären, wie der Tausch die konkrete Stellung verändert.

#### STRAT-010: Initiative und Tempo

Prüfen:

- Entsteht eine direkte Drohung?
- Muss der Gegner reagieren?
- Geht ein Tempo durch wiederholte Züge verloren?
- Wird ein gegnerischer Plan verhindert?
- Kann eine Initiative in Materialgewinn oder einen langfristigen Vorteil umgewandelt werden?

Begründung: Initiative darf nicht allein aus der Anzahl der Angriffe abgeleitet werden. Es muss eine konkrete, relevante Drohung erkennbar sein.

#### STRAT-011: Langfristige Pläne

Für beide Seiten soll die Coaching-Logik aus Stellung und Engine-Varianten mögliche Pläne ableiten, beispielsweise:

- einen schwachen Bauern angreifen;
- eine offene Linie besetzen;
- eine Figur verbessern;
- einen Bauernhebel vorbereiten;
- einen Freibauern bilden;
- den König aktivieren;
- einen vorteilhaften Figurenabtausch anstreben.

Jeder Plan muss an konkrete Merkmale der Stellung gebunden sein. Der Coach darf keine universell gültigen Pläne ohne Stellungsbezug ausgeben.

#### STRAT-012: Endspielprinzipien

Prüfen:

- Aktivität des Königs;
- Bildung und Unterstützung von Freibauern;
- Bauernmehrheiten;
- Opposition und relevante Königskonstellationen;
- Turmaktivität;
- Umwandlungsrennen;
- technische Gewinn- und Remischancen.

Für präzise Endspielbewertungen ist die Engine maßgeblich. Die Coaching-Regeln sollen erklären, welche Merkmale den Plan unterstützen.

### 5.3 Anforderungen an strategische Regeln

Jede strategische Regel muss:

- eine eindeutige ID besitzen;
- ihre Eingabemerkmale dokumentieren;
- einen Score oder eine abgestufte Bewertung liefern;
- eine nachvollziehbare Begründung generieren;
- relevante Felder und Figuren referenzieren;
- zwischen gesicherter Erkenntnis und strategischer Heuristik unterscheiden;
- durch Unit-Tests abgedeckt sein.

Die Bewertung soll nicht allein durch eine Summe willkürlicher Regel-Scores entstehen. Strategische Merkmale ergänzen die Engine-Bewertung und erklären sie. Bei Widersprüchen hat die nachgewiesene taktische Konsequenz Vorrang vor einer allgemeinen strategischen Heuristik.

### 5.4 Ebene 3 – Pädagogische Erklärungen

Ziel: Aus Engine-Ergebnissen und erkannten Regeln eine verständliche, lehrreiche Erklärung erstellen.

Die Erklärung wird aus strukturierten Analyseergebnissen zusammengesetzt. Eine generative KI ist für die erste Version nicht erforderlich.

Jede Erklärung muss, soweit relevant, folgende Fragen beantworten:

1. Was war an meinem Zug gut oder schlecht?
2. Welche konkrete Konsequenz hatte er?
3. Was hätte der Gegner darauf spielen können?
4. Welcher Zug wäre besser gewesen?
5. Warum ist dieser alternative Zug besser?
6. Welche langfristigen Pläne ergeben sich daraus?
7. Welches Schachprinzip sollte ich mir merken?

Beispiel für eine Erklärungsvorlage:

### Beispiel: Ein strategisch unvorteilhafter Zug

Beispiel, keine echte Engine-Analyse

Bewertung: Der Zug ist positionell ungenau.

Was ist passiert? Mit deinem letzten Zug hast du deinen Springer von einem aktiven Feld auf ein passives Feld zurückgezogen.

Warum ist das problematisch? Der Springer kontrolliert jetzt weniger zentrale Felder. Gleichzeitig kann dein Gegner einen Bauernhebel vorbereiten, ohne dass du genügend Druck im Zentrum aufbaust.

Was wäre besser? Ein Zug, der deinen Turm auf der offenen Linie aktiviert, ist laut Analyse vorzuziehen.

Was kannst du daraus lernen? Beurteile Figuren nicht nur danach, ob sie sicher stehen. Entscheidend ist auch, welche Felder sie kontrollieren und welchen Plan sie unterstützen.

Die tatsächlichen Erklärungen müssen dynamisch aus der jeweiligen Stellung und den konkreten Engine-Varianten entstehen. Der Beispieltext ist kein fest zu verwendender Standardtext.

#### Erklärungstypen

Die Coaching-Logik muss mindestens folgende Typen unterstützen:

- `TACTICAL_BLUNDER`: taktischer Fehler;
- `MATERIAL_LOSS`: vermeidbarer Materialverlust;
- `MISSED_WIN`: Gewinnchance verpasst;
- `MISSED_DEFENSE`: Verteidigung übersehen;
- `STRATEGIC_INACCURACY`: strategische Ungenauigkeit;
- `GOOD_MOVE`: guter Zug mit nachvollziehbarer Begründung;
- `BEST_MOVE`: bester oder einer der besten Züge;
- `FORCED_MOVE`: weitgehend erzwungener Zug;
- `DRAWING_RESOURCE`: Remisressource;
- `ENDGAME_TECHNIQUE`: Endspieltechnik;
- `UNCERTAIN_ANALYSIS`: keine ausreichend zuverlässige Einordnung möglich.

#### Verbindliche Qualitätsregeln

- Jede wesentliche Aussage muss auf eine konkrete Regel oder Engine-Analyse zurückführbar sein.
- Varianten dürfen nur aus legalen Zügen bestehen.
- Die Erklärung darf nicht behaupten, ein Zug sei erzwungen, wenn mehrere gleichwertige Antworten existieren.
- Bei unsicherer Bewertung muss die Unsicherheit sichtbar sein.
- Ein guter Zug benötigt ebenfalls eine Erklärung; das Coaching darf nicht ausschließlich Fehler kommentieren.
- Wiederkehrende Fehler sollen als Lernmuster erfasst werden, ohne ein Benutzerkonto oder einen Server zu benötigen.
- Die Erklärung soll fachlich präzise, auf Deutsch und ohne unnötige Schachterminologie formuliert sein.

## 6. Engine-Analyse und Qualitätsbewertung

### 6.1 Analyseverfahren

Nach dem Benutzerzug muss die Anwendung mindestens drei Stellungen unterscheiden:

- \\(P_0\\): Stellung vor dem Zug;
- \\(P_u\\): Stellung nach dem tatsächlich gespielten Zug;
- \\(P_b\\): Stellung nach dem besten gefundenen Zug.

Die Engine analysiert die Stellungen mit vergleichbaren Suchbedingungen. Ein einfacher Vergleich zweier Bewertungen aus unterschiedlichen Suchtiefen ist nicht ausreichend, um einen verlässlichen Fehlerwert zu bestimmen.

Die Bewertung wird immer aus der Perspektive des Benutzers normalisiert. Interne Engine-Scores, die aus Sicht der Seite am Zug ausgegeben werden, müssen korrekt umgerechnet werden.

### 6.2 Verlust gegenüber dem besten Zug

Bei normalen Stellungsbewertungen kann der ungefähre Bewertungsverlust wie folgt berechnet werden:

\\[ L = E(P_b) - E(P_u) \\]

Dabei müssen beide Bewertungen aus derselben Perspektive und in derselben Einheit vorliegen.

- Positive Werte bedeuten, dass der beste Kandidat vorteilhafter ist als der gespielte Zug.
- Nahe null liegende Werte sprechen bei ausreichender Suchtiefe für einen ungefähr gleichwertigen Zug.
- Mattbewertungen müssen gesondert behandelt werden.
- Ein Wechsel von einer gewonnenen Stellung zu einer verlorenen Stellung darf nicht wie ein gewöhnlicher kleiner Centipawn-Unterschied behandelt werden.

Die Klassifizierung darf nicht ausschließlich auf festen Centipawn-Grenzen beruhen. Matt, erzwungene taktische Folgen, die Zuverlässigkeit der Analyse und die vorherige Stellung müssen ebenfalls berücksichtigt werden.

### 6.3 Qualitätsklassen

Mindestens folgende Klassen sind erforderlich:

| Klasse        | Bedeutung                                                                                           |
| ------------- | --------------------------------------------------------------------------------------------------- |
| Bestzug       | Der Zug entspricht dem besten gefundenen Zug oder liegt innerhalb der definierten Gleichwertigkeit. |
| Sehr gut      | Praktisch gleichwertig mit dem besten Zug.                                                          |
| Gut           | Solider Zug mit höchstens geringem Nachteil.                                                        |
| Ungenau       | Ein relevanter Vorteil geht teilweise verloren.                                                     |
| Fehler        | Ein wesentlicher Vorteil geht verloren oder ein Nachteil entsteht.                                  |
| Grober Fehler | Ein erheblicher Nachteil entsteht, etwa durch Materialverlust oder eine taktische Wendung.          |
| Erzwungen     | Die Stellung lässt nur wenige oder praktisch keine gleichwertigen Alternativen zu.                  |

Die konkreten Schwellenwerte sind konfigurierbar und müssen anhand definierter Teststellungen überprüft werden.

### 6.4 Kandidatenzüge und Varianten

Die Detailanalyse soll mindestens drei unterschiedliche Kandidatenzüge enthalten, sofern die Stellung solche Alternativen aufweist.

Für jeden Kandidaten sind zu liefern:

- Zugnotation;
- Stellungsbewertung;
- Suchtiefe;
- Hauptvariante;
- erkennbare taktische Motive;
- strategische Vor- und Nachteile;
- Vergleich mit dem tatsächlich gespielten Zug.

Die Anwendung muss klar kennzeichnen, wenn eine Variante nur eine vom Suchalgorithmus gefundene Hauptvariante ist und keine erzwungene Zugfolge darstellt.

### 6.5 Performance-Anforderungen an die Engine

- Die Computerzüge haben Vorrang vor umfangreichen Coaching-Analysen.
- Analyseprozesse müssen abbrechbar sein.
- Engine-Prozesse müssen kontrolliert und wiederverwendbar verwaltet werden.
- Gleichzeitige Analysen derselben Partie dürfen sich nicht gegenseitig verfälschen.
- Suchzeit und Suchtiefe müssen konfigurierbar sein.
- Die UI muss Analysefortschritt anzeigen können, sofern die Engine geeignete Informationen liefert.
- Ergebnisse müssen einer eindeutigen Partieversion und Stellung zugeordnet sein.

Für den MVP ist ein Zielwert von höchstens fünf Sekunden für die erste Coaching-Rückmeldung auf einem definierten Referenzsystem anzustreben. Umfangreiche Varianten dürfen anschließend weiter verfeinert werden. Die konkrete Suchdauer und Engine-Stärke sind messbar zu dokumentieren.

## 7. Frontend-Anforderungen mit Vue 3 und Quasar

### 7.1 Hauptansicht

Die Hauptansicht ist eine klassische Schachoberfläche mit drei Bereichen:

### Schach-Coach

Computergegner · Mittel

[File:Chess pieces and board improved.svg - Wikimedia Commons](https://images.openai.com/static-rsc-4/gOnFqGqzNqv1SwVvmh5uhjWCsHc3BEbDqxq3vppYs9Yg54lxWruB3E8moAB5HsxF1Cpsfe3kRy-kAk95jp_6vgudiEfcE8Z14OJwEvM1aqqz6Pm7RjBReg2XHTd7BkUCmDoVGukTmHILGsJSN1ZrKl14GBw9vsXkruUoDxXwu1A?purpose=inline)

Interaktives 8×8-Brett · Letzter Zug hervorgehoben

Neu

Zurück

Tipp

### Zug-Coaching

Ungenau

18\. h3

Warum?

Die konkrete Analyse erklärt die Folgen deines Zuges und vergleicht bessere Alternativen.

Bessere Alternative

18\. Rad1

Hauptvariante und weitere Kandidaten in der Detailansicht

Konzeptionelles Layout, keine fertige Anwendung und keine echte Partiebewertung.

Die tatsächliche Darstellung soll sich an der verfügbaren Bildschirmbreite orientieren. Auf kleinen Geräten werden Brett und Analyse untereinander angeordnet.

### 7.2 Komponentenstruktur

```
frontend/src/
├── app/
│   ├── App.vue
│   └── router.ts
├── pages/
│   └── GamePage.vue
├── components/
│   ├── ChessBoard.vue
│   ├── ChessPiece.vue
│   ├── MoveList.vue
│   ├── GameControls.vue
│   ├── EngineStatus.vue
│   ├── CoachingSummary.vue
│   ├── CoachingDetails.vue
│   ├── CandidateMoves.vue
│   ├── VariationViewer.vue
│   ├── StrategicFindings.vue
│   └── SettingsDialog.vue
├── stores/
│   ├── gameStore.ts
│   ├── analysisStore.ts
│   └── settingsStore.ts
├── services/
│   ├── gameApi.ts
│   ├── analysisApi.ts
│   └── localGameStorage.ts
├── models/
│   ├── game.ts
│   ├── analysis.ts
│   └── coaching.ts
└── assets/
    └── chess-pieces/
```

### 7.3 Schachfiguren als SVG

Es sind klassische Schachfiguren aus einem konsistenten SVG-Satz zu verwenden. Die Wikimedia-Commons-Kategorie [SVG chess pieces](https://commons.wikimedia.org/wiki/Category:SVG_chess_pieces?utm_source=chatgpt.com) enthält geeignete Figuren-Sammlungen.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://commons.wikimedia.org\&sz=32)

Wikimedia Commons

+1



Verbindliche Anforderungen:

- Zwölf SVG-Dateien: sechs weiße und sechs schwarze Figuren.
- SVG-Dateien lokal im Projekt ablegen; keine Laufzeitabhängigkeit von Wikimedia.
- Quelle, Urheber und Lizenz für jede verwendete Datei dokumentieren.
- `THIRD_PARTY_NOTICES.md` und die jeweils erforderlichen Lizenztexte mitliefern.
- SVGs hinsichtlich Darstellung, ViewBox und Skalierung vereinheitlichen.
- Keine externen Referenzen oder aktiven Skripte innerhalb der SVG-Dateien.
- Figurengröße automatisch an die Feldgröße anpassen.
- Drag-and-drop und Touch-Eingaben unterstützen.
- Alternative Bedienung per Auswahl und Zielfeldklick ermöglichen.

Die Lizenz der konkreten Dateien ist einzeln zu prüfen. Die Zugehörigkeit zu Wikimedia Commons allein bedeutet nicht, dass alle Dateien derselben Lizenz unterliegen. Für Cburnett-Figuren sind beispielsweise mehrere Lizenzmodelle dokumentiert.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://commons.wikimedia.org\&sz=32)

Wikimedia Commons

+1



### 7.4 Interaktion mit der Analyse

- Ein Klick auf einen Kandidatenzug zeigt die entsprechende Variante auf dem Brett.
- Eine Variante darf die aktuelle Partie nicht dauerhaft verändern.
- Der Benutzer kann eine Variante Schritt für Schritt vorwärts und rückwärts durchlaufen.
- Ein Wechsel zwischen Kandidaten setzt die Analyseansicht auf den jeweiligen Variantenanfang zurück.
- Ein Klick auf einen vergangenen Zug zeigt dessen Analyse, sofern sie vorhanden ist.
- Die Anwendung kennzeichnet Analysen, die nach einer Änderung der Engine-Einstellungen veraltet sind.
- Ein laufender Analyseprozess darf keine neuere Partieversion überschreiben.
- Die vollständige Erklärung ist ohne externe Links oder weitere Dienste verfügbar.

## 8. Backend-Architektur mit Spring Boot

### 8.1 Package-Struktur

```
backend/src/main/java/de/chesscoach/
├── api/
│   ├── GameController.java
│   ├── MoveController.java
│   ├── AnalysisController.java
│   └── SettingsController.java
├── application/
│   ├── GameService.java
│   ├── MoveService.java
│   └── AnalysisService.java
├── domain/
│   ├── game/
│   ├── position/
│   ├── analysis/
│   └── coaching/
├── engine/
│   ├── ChessEngine.java
│   ├── CalvinChessEngine.java
│   ├── UciProcessManager.java
│   └── EngineAnalysisParser.java
├── chess/
│   ├── ChessRulesService.java
│   └── ChessPositionAdapter.java
├── coaching/
│   ├── TacticalRuleEngine.java
│   ├── StrategicRuleEngine.java
│   ├── ExplanationBuilder.java
│   └── MistakeClassifier.java
├── infrastructure/
│   ├── config/
│   ├── error/
│   └── monitoring/
└── dto/
    ├── request/
    └── response/
```

Die genaue Package-Aufteilung kann der Coding Agent anpassen, solange Domain-Logik, Infrastruktur, REST-API und Engine-Integration getrennt bleiben.

### 8.2 Zuständigkeiten

GameService

Verwaltet den Zustand einer laufenden Partie, die Zugreihenfolge, das Partieende und die Gültigkeit eingehender Aktionen.

MoveService

Validiert einen Zug, aktualisiert die Partie und stößt bei einem eigenen Zug die Analyse an.

AnalysisService

Orchestriert Engine-Aufrufe, vergleicht Bewertungen, ermittelt Kandidatenzüge und führt die Coaching-Pipeline aus.

TacticalRuleEngine

Wertet taktische Regeln aus und erzeugt strukturierte Befunde.

StrategicRuleEngine

Wertet positionelle Merkmale aus und erzeugt strategische Befunde.

ExplanationBuilder

Erstellt aus den strukturierten Befunden und den Engine-Ergebnissen die deutschsprachige Erklärung.

UciProcessManager

Verwaltet Engine-Prozesse, Timeouts, Abbruch und Neustart.

### 8.3 Verarbeitung eines Spielerzuges

Coaching-LogikUCI EngineSchachregelnSpring BootVue / QuasarCoaching-LogikUCI EngineSchachregelnSpring BootVue / Quasar#chatgpt-mermaid-\_r_o6\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_o6\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_o6\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_o6\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_o6\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_o6\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_o6\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_o6\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_o6\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_o6\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_o6\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_o6\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_o6\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_o6\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_o6\_ p{margin:0;}#chatgpt-mermaid-\_r_o6\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_o6\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_o6\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_o6\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_o6\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_o6\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_o6\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_o6\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_o6\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_o6\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_o6\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_o6\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_o6\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_o6\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_o6\_ .labelText,#chatgpt-mermaid-\_r_o6\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_o6\_ .loopText,#chatgpt-mermaid-\_r_o6\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_o6\_ .sectionTitle,#chatgpt-mermaid-\_r_o6\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_o6\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_o6\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_o6\_ .noteText,#chatgpt-mermaid-\_r_o6\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_o6\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_o6\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_o6\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_o6\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_o6\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_o6\_ .actor-man circle,#chatgpt-mermaid-\_r_o6\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_o6\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_o6\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_o6\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_o6\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_o6\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_o6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_o6\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_o6\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_o6\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_o6\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_o6\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_o6\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_o6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_o6\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_o6\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_o6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_o6\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_o6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_o6\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}SpielerEigenen Zug ausführenPOST /api/games/{id}/movesZug validierenLegaler Zug + neue StellungStellung vor und nach dem Zug analysierenBewertungen + Kandidaten + VariantenTaktik- und Strategieregeln anwendenBefunde + ErklärungenZugresultat + CoachingBewertung und Warum anzeigenAnalyse ansehenPOST /api/games/{id}/engine-moveComputerzug berechnenLegaler KandidatenzugComputerzugSpieler

Die tatsächliche Ausführungsreihenfolge ist so zu gestalten, dass der Benutzer nach seinem Zug eine Coaching-Rückmeldung erhält und der Computer anschließend regelkonform antwortet. Eine asynchrone Ausführung ist erlaubt, sofern der Zustand eindeutig bleibt und die UI den Rechenstatus anzeigt.

Die API muss verhindern, dass derselbe Zug mehrfach verarbeitet wird, wenn der Browser eine Anfrage wiederholt.

## 9. REST-API

Das Backend stellt eine versionierte REST-API unter `/api` bereit.

### 9.1 Endpunkte

| Methode | Endpunkt                                        | Zweck                                                |
| ------- | ----------------------------------------------- | ---------------------------------------------------- |
| `GET`   | `/api/health`                                   | Gesundheitsstatus der Anwendung                      |
| `GET`   | `/api/engine/status`                            | Verfügbarkeit und Version der Engine                 |
| `POST`  | `/api/games`                                    | Neue Partie starten                                  |
| `GET`   | `/api/games/{gameId}`                           | Aktuelle Partie abrufen                              |
| `POST`  | `/api/games/{gameId}/moves`                     | Benutzerzug ausführen                                |
| `POST`  | `/api/games/{gameId}/engine-move`               | Computerzug anfordern                                |
| `POST`  | `/api/games/{gameId}/undo`                      | Letzten Zug zurücknehmen                             |
| `POST`  | `/api/games/{gameId}/restart`                   | Partie neu starten                                   |
| `GET`   | `/api/games/{gameId}/analysis/{ply}`            | Analyse eines Zuges abrufen                          |
| `POST`  | `/api/games/{gameId}/analysis/{ply}`            | Analyse anfordern oder aktualisieren                 |
| `POST`  | `/api/games/{gameId}/analysis/{ply}/variations` | Kandidaten und Varianten vertiefen                   |
| `PUT`   | `/api/settings`                                 | Einstellungen für die laufende Sitzung aktualisieren |

Die API benötigt keine Authentifizierung, da keine Benutzerkonten existieren. Sie muss trotzdem Eingaben validieren und gegen übermäßige Analyseanforderungen abgesichert sein.

### 9.2 Beispiel: Neue Partie starten

Anfrage:

```
{
  "playerColor": "WHITE",
  "difficulty": "MEDIUM"
}
```

Antwort:

```
{
  "gameId": "generated-game-id",
  "status": "IN_PROGRESS",
  "playerColor": "WHITE",
  "sideToMove": "WHITE",
  "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
  "engineStatus": "READY"
}
```

Die `gameId` ist eine zufällig erzeugte Sitzungskennung und keine Benutzerkennung. Sie darf keine persönlichen Informationen enthalten.

### 9.3 Beispiel: Benutzerzug

Anfrage:

```
{
  "uciMove": "e2e4",
  "expectedPositionVersion": 0
}
```

Antwort:

```
{
  "gameId": "generated-game-id",
  "positionVersion": 1,
  "playedMove": "e2e4",
  "san": "e4",
  "fen": "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
  "status": "IN_PROGRESS",
  "analysisStatus": "PENDING"
}
```

Die Antwort kann synchron um Analyseergebnisse ergänzt werden oder zunächst `PENDING` liefern. Die gewählte Variante muss dokumentiert sein. Für längere Analysen ist ein asynchrones Verfahren mit Statusabfrage vorzuziehen.

### 9.4 Beispiel: Coaching-Ergebnis

```
{
  "ply": 10,
  "playedMove": "Nf3",
  "classification": "INACCURACY",
  "scoreBefore": {
    "type": "CP",
    "value": 35
  },
  "scoreAfter": {
    "type": "CP",
    "value": 12
  },
  "bestMove": "d4",
  "bestMoveExplanation": "Der Zug verbessert die Kontrolle über das Zentrum.",
  "principalVariation": ["d4", "exd4", "Nxd4"],
  "findings": [
    {
      "ruleId": "STRAT-002",
      "category": "STRATEGIC",
      "severity": "MEDIUM",
      "title": "Zentrumskontrolle",
      "explanation": "Der gespielte Zug verbessert den Einfluss auf das Zentrum weniger als die Alternative."
    }
  ],
  "analysisConfidence": "MEDIUM"
}
```

Dieses Beispiel beschreibt nur das API-Format. Die Werte und Varianten sind keine Bewertung einer konkreten Stellung.

### 9.5 API-Vertragsregeln

- JSON-Felder sind versioniert und dokumentiert.
- Züge werden intern vorzugsweise als UCI-Notation übertragen.
- SAN wird für die Anzeige und Zugliste verwendet.
- Bewertungen enthalten einen Typ, damit Centipawn- und Mattwerte nicht verwechselt werden.
- Jede Analyse referenziert eine eindeutige Stellung und Partieverson.
- Die API gibt keine internen Java-Objekte direkt aus.
- Ungültige Eingaben führen zu strukturierten Fehlerantworten.
- Die OpenAPI-Spezifikation wird aus dem Projekt generiert oder als überprüfbarer Vertrag gepflegt.

## 10. Datenmodelle

### 10.1 Backend-Domainmodelle

GameState

- `gameId`
- `initialFen`
- `currentFen`
- `moves`
- `sideToMove`
- `status`
- `playerColor`
- `difficulty`
- `positionVersion`
- `createdAt`
- `updatedAt`

MoveRecord

- `ply`
- `uci`
- `san`
- `fenBefore`
- `fenAfter`
- `player`
- `timestamp`
- `analysisStatus`
- `analysisId`

EngineAnalysis

- `analysisId`
- `fen`
- `engineName`
- `engineVersion`
- `depth`
- `nodes`
- `elapsedMillis`
- `score`
- `bestMove`
- `principalVariation`
- `candidateMoves`
- `completed`

MoveAssessment

- `classification`
- `scoreBefore`
- `scoreAfter`
- `evaluationLoss`
- `confidence`
- `summary`
- `findings`
- `alternatives`
- `strategicPlans`

CoachingFinding

- `ruleId`
- `category`
- `severity`
- `title`
- `explanation`
- `affectedSquares`
- `affectedPieces`
- `evidence`
- `relatedVariationIds`

CoachingTip

- `title`
- `observation`
- `why`
- `betterAlternative`
- `expectedConsequence`
- `learningPrinciple`
- `confidence`

Alle Modelle sind als typisierte Java-Klassen oder Records zu implementieren. Mutable Domainobjekte dürfen nicht direkt zwischen konkurrierenden Analyseprozessen geteilt werden.

### 10.2 Frontend-Modelle

Das Frontend benötigt entsprechende TypeScript-Typen für Partie, Zug, Bewertung, Variante, Regelbefund, Engine-Status und Einstellungen.

Die API-Antworten werden an einer zentralen Stelle validiert. Die UI darf keine nicht vorhandenen Felder voraussetzen und muss fehlende oder unvollständige Analysen korrekt darstellen.

### 10.3 Lokale Speicherung

IndexedDB verwendet mindestens folgende Stores:

- `games`
- `settings`
- `learningStats`
- `analysisCache`

Der Analyse-Cache wird anhand von FEN, Engine-Version, Analyseparametern und Regelversion identifiziert. Ein veralteter Cache darf nicht als aktuelle Bewertung angezeigt werden.

Die Anwendung muss beim Wiederherstellen einer Partie die Stellung aus der Zughistorie rekonstruieren und mit dem gespeicherten FEN vergleichen. Bei Inkonsistenzen muss sie eine kontrollierte Wiederherstellung oder einen verständlichen Fehler anbieten.

## 11. Fehlerbehandlung und Robustheit

### 11.1 Fehlerfälle

| Fehler                                 | Erwartetes Verhalten                                                                 |
| -------------------------------------- | ------------------------------------------------------------------------------------ |
| Engine kann nicht gestartet werden     | Spielstart mit Computergegner verhindern und verständliche Meldung anzeigen.         |
| Engine antwortet nicht rechtzeitig     | Analyse abbrechen, Engine kontrolliert beenden oder neu starten und Status anzeigen. |
| Engine liefert ungültigen Zug          | Zug nicht ausführen, Engine-Fehler protokollieren und gegebenenfalls neu starten.    |
| UCI-Ausgabe ist unvollständig          | Keine fehlerhafte Analyse ausgeben; begrenzte Wiederholung oder Fehlerstatus.        |
| Ungültiger Benutzerzug                 | Zug ablehnen; Stellung und Zugliste unverändert lassen.                              |
| Veraltete Positionsversion             | Anfrage mit Konfliktstatus ablehnen und aktuelle Stellung zurückgeben.               |
| Analyse läuft nach einem Undo weiter   | Veraltete Analyse abbrechen oder verwerfen.                                          |
| Browser-Speicherung schlägt fehl       | Fehlermeldung anzeigen und Partie möglichst im Arbeitsspeicher fortsetzen.           |
| Backend ist nicht erreichbar           | Verbindungsstatus und Wiederholungsmöglichkeit anzeigen.                             |
| Partie ist beendet                     | Weitere Züge blockieren und Endstellung beibehalten.                                 |
| Keine verlässliche Bewertung verfügbar | Analyse als unsicher kennzeichnen; keine definitive Qualitätsklasse vortäuschen.     |

### 11.2 Fehlerformat

Alle API-Fehler sollen einem einheitlichen Format folgen, vorzugsweise dem RFC-9457-Format `application/problem+json`.

Beispiel:

```
{
  "type": "urn:chess-coach:error:illegal-move",
  "title": "Ungültiger Zug",
  "status": 422,
  "detail": "Der angeforderte Zug ist in der aktuellen Stellung nicht legal.",
  "code": "ILLEGAL_MOVE",
  "positionVersion": 7
}
```

Stacktraces und interne Prozessinformationen dürfen nicht an das Frontend ausgegeben werden.

### 11.3 Nebenläufigkeit

- Jede Partie besitzt eine eindeutige Positionsversion.
- Ein Zug wird atomar gegen die erwartete Positionsversion geprüft.
- Eine Analyse gehört immer zu einer konkreten FEN und Versionsnummer.
- Nach einem neuen Zug werden alte Analyseergebnisse nicht mehr als aktuell angezeigt.
- Engine-Aufrufe müssen begrenzt und kontrolliert werden.
- Die Anwendung muss doppelte Requests sicher behandeln.
- Eine Partie darf nicht durch parallele Zuganfragen in einen inkonsistenten Zustand geraten.

### 11.4 Sicherheit

- Eingaben, FEN und Engine-Parameter werden validiert.
- Keine vom Benutzer gelieferten Strings dürfen als Betriebssystembefehle interpretiert werden.
- Engine-Starts verwenden feste, konfigurierte Programme und Argumente.
- Prozessausgaben werden begrenzt und kontrolliert verarbeitet.
- Die REST-API begrenzt Analysezeit, Suchparameter und Anfragefrequenz.
- CORS wird auf die vorgesehenen Frontend-Ursprünge beschränkt.
- Produktionskonfigurationen dürfen keine unnötigen Debug-Endpunkte offenlegen.

## 12. Nichtfunktionale Anforderungen

### NFR-01: Wartbarkeit

- Klare Trennung zwischen Spielregeln, Engine, Coaching und API.
- Keine Engine-spezifischen Abhängigkeiten in strategischen Regeln.
- Keine umfangreiche Coaching-Logik in Vue-Komponenten.
- Alle Regeln sind über eindeutige IDs auffindbar.
- Technische Entscheidungen sind dokumentiert.

### NFR-02: Erweiterbarkeit

Neue Coaching-Regeln müssen ohne Änderungen am Kern der Engine-Integration hinzugefügt werden können. Regeln sollen registriert, aktiviert, deaktiviert und separat getestet werden können.

### NFR-03: Performance

- Erste Coaching-Rückmeldung möglichst innerhalb von fünf Sekunden auf einem dokumentierten Referenzsystem.
- Lange Variantenberechnungen laufen asynchron.
- Engine-Prozesse werden wiederverwendet, soweit die Implementierung dies zuverlässig ermöglicht.
- Die Oberfläche bleibt während der Analyse bedienbar.
- Analyse-Cache und Suchparameter müssen konfigurierbar sein.

### NFR-04: Zuverlässigkeit

- Keine illegalen Computerzüge.
- Keine inkonsistenten Zuglisten oder Stellungen.
- Fehlerhafte Analyseprozesse dürfen die Partie nicht beschädigen.
- Ein fehlgeschlagener Analyseaufruf darf den gültigen Spielzustand nicht zurücksetzen.

### NFR-05: Browser-Kompatibilität

Die Anwendung muss mit aktuellen stabilen Versionen von Chromium-basierten Browsern und Firefox getestet werden. Safari sollte nach Möglichkeit ebenfalls unterstützt werden.

### NFR-06: Barrierefreiheit

- Bedienbare Buttons mit verständlichen Beschriftungen.
- Sichtbarer Tastaturfokus.
- Nicht ausschließlich farbbasierte Hervorhebung.
- Ausreichender Farbkontrast.
- Statusmeldungen für Schach, Matt, laufende Analyse und Fehler.
- Schachfiguren besitzen passende alternative Beschriftungen.

### NFR-07: Reproduzierbarkeit

- Abhängigkeiten und Versionen sind fixiert.
- Build und Tests funktionieren in einer frischen Umgebung.
- Die Engine-Version wird dokumentiert.
- Die Tests dürfen nicht von externen Schachdiensten abhängen.
- CI muss Frontend-Build, Typechecks, Backend-Tests und Lizenzprüfung ausführen.

### NFR-08: Lokaler Betrieb

Die Anwendung muss lokal ausführbar sein. Eine mögliche Zielarchitektur ist ein Docker-Compose-Setup mit Backend und Frontend beziehungsweise ausgeliefertem Frontend. Die Engine wird zusammen mit dem Backend-Paket ausgeliefert.

Es darf keine zusätzliche Installation einer externen Schach-Engine vorausgesetzt werden. Systemanforderungen wie Java-Version und unterstützte Betriebssysteme müssen dokumentiert sein.

## 13. Teststrategie

Die Teststrategie muss die Schachregeln, die Engine-Integration und die Qualität der Erklärungen getrennt absichern.

### 13.1 Unit-Tests

Schachregeln

- legale und illegale Züge;
- Rochade;
- En passant;
- Bauernumwandlung;
- Schach und Schachmatt;
- Patt;
- Zugwiederholung;
- Remisregeln;
- Rücknahme und Wiederherstellung eines Zuges.

Engine-Integration

- Prozessstart;
- UCI-Initialisierung;
- `uci`, `isready`, `position`, `go` und `stop`;
- Parsen von `info` und `bestmove`;
- Timeout;
- Prozessabbruch;
- Neustart nach Absturz;
- ungültige Engine-Ausgabe.

Coaching

- jede Regel einzeln;
- Erkennung der passenden Stellung;
- korrekte Begründung;
- betroffene Felder und Figuren;
- Priorisierung taktischer gegenüber strategischen Befunden;
- Umgang mit unsicheren Bewertungen;
- Klassifizierung von guten und schlechten Zügen.

### 13.2 Integrationstests

- Eine Partie wird gestartet und legal fortgesetzt.
- Der Benutzer spielt einen Zug.
- Die Analyse wird korrekt der Stellung zugeordnet.
- Die Coaching-Regeln liefern strukturierte Ergebnisse.
- Der Computer spielt einen legalen Zug.
- Ein Undo verwirft nachfolgende veraltete Analysen.
- Ein Engine-Ausfall führt zu einem kontrollierten Fehler.
- Wiederholte Requests erzeugen keine doppelten Züge.

### 13.3 Frontend-Tests

- Figuren auswählen und ziehen;
- legale Zielfelder anzeigen;
- Bauernumwandlung auswählen;
- Zugliste aktualisieren;
- Coaching-Ergebnis anzeigen;
- Varianten durchspielen und verlassen;
- Partie zurücknehmen und neu starten;
- lokale Speicherung und Wiederherstellung;
- Backend-Ausfall darstellen;
- responsive Darstellung prüfen.

### 13.4 End-to-End-Tests

Mit Playwright müssen mindestens folgende Abläufe automatisiert werden:

1. Anwendung öffnen und neue Partie starten.
2. Einen legalen Zug spielen.
3. Auf das Coaching-Ergebnis warten.
4. Die Erklärung öffnen und eine Kandidatenvariante abspielen.
5. Den Computerzug ausführen lassen.
6. Den letzten Zug zurücknehmen.
7. Die Seite neu laden und die Partie wiederherstellen.
8. Eine Partie bis zum Matt oder Remis spielen und das Ergebnis prüfen.

### 13.5 Tests für die Erklärungsqualität

Die Coaching-Logik darf nicht nur darauf getestet werden, ob ein Text ausgegeben wird. Die Aussagen müssen inhaltlich überprüfbar sein.

Dazu ist ein Katalog aus kuratierten Teststellungen zu erstellen. Jede Teststellung enthält:

- FEN;
- erwartetes taktisches oder strategisches Motiv;
- relevante legale Kandidatenzüge;
- erwartete Regel-IDs;
- erwartete Material- oder Stellungsfolgen;
- zulässige Erklärungsaussagen;
- Aussagen, die ausdrücklich nicht erzeugt werden dürfen.

Die Tests müssen insbesondere verhindern, dass:

- eine nicht existente Figur als angegriffen bezeichnet wird;
- eine illegale Variante ausgegeben wird;
- ein Materialgewinn behauptet wird, obwohl die Variante ihn nicht bestätigt;
- eine strategische Heuristik eine nachgewiesene taktische Widerlegung übergeht;
- eine unsichere Analyse als zwingendes Ergebnis formuliert wird.

### 13.6 Testabdeckung

Als Mindestziel gelten:

- mindestens 80 % Line Coverage für die eigene Coaching-Logik;
- 100 % der implementierten Coaching-Regeln durch dedizierte Tests abgedeckt;
- alle kritischen Engine- und Spielzustandsübergänge durch Tests abgesichert;
- keine Mindestabdeckungsanforderung an externen Bibliothekscode.

Die Prozentwerte allein gelten nicht als Nachweis der fachlichen Qualität.

## 14. Implementierungsreihenfolge für den Coding Agenten

Die Implementierung muss inkrementell erfolgen. Der Agent darf nicht zunächst die gesamte UI erstellen und die Schachlogik erst später integrieren. Jede Phase muss in einem lauffähigen Zustand enden.

1. Phase 1 – Projektgrundlage und Bibliotheksprüfung
   - Vue-3-/Quasar-Projekt mit TypeScript anlegen.
   - Spring-Boot-Projekt mit Java anlegen.
   - Calvin und Chesslib in einem Proof of Concept integrieren.
   - UCI-Kommunikation und Java-Kompatibilität testen.
   - Lizenztexte und Drittanbieterabhängigkeiten dokumentieren.
   Ergebnis: Reproduzierbares Build und funktionsfähiger Engine-Prototyp.
2. Phase 2 – Schachregeln und Partieverwaltung
   - Adapter für Chesslib implementieren.
   - Partie- und Zugmodelle erstellen.
   - Legale Züge, Rochade, En passant, Umwandlung und Partieende testen.
   - REST-Endpunkte für Partie und Züge implementieren.
   Ergebnis: Eine Partie kann zuverlässig über die API gespielt werden.
3. Phase 3 – Computergegner
   - UCI-Engine-Adapter fertigstellen.
   - Engine-Lebenszyklus und Fehlerbehandlung implementieren.
   - Einstellbare Spielstärke realisieren.
   - Computerzüge integrieren und auf Legalität prüfen.
   Ergebnis: Eine vollständige Partie gegen den Computer ist möglich.
4. Phase 4 – Frontend
   - Schachbrett und SVG-Figuren integrieren.
   - Zugliste, Spielsteuerung und Einstellungen erstellen.
   - Partie- und Analysezustand über Pinia verwalten.
   - Backend-API integrieren.
   - Responsive Darstellung und lokale Speicherung umsetzen.
   Ergebnis: Vollständig spielbare Browseranwendung ohne Coaching.
5. Phase 5 – Engine-Analyse
   - Bewertungen vor und nach einem Zug berechnen.
   - Kandidatenzüge und Hauptvarianten ermitteln.
   - Bewertungsperspektive normalisieren.
   - Mattbewertungen, Analyseabbrüche und Unsicherheit behandeln.
   - Analyseergebnisse mit der jeweiligen Partieverson verknüpfen.
   Ergebnis: Technisch belastbare Grundlage für das Coaching.
6. Phase 6 – Coaching-Ebene 1
   - Taktische Regeln implementieren.
   - Materialveränderungen und konkrete taktische Folgen ermitteln.
   - Taktische Befunde mit Engine-Varianten verifizieren.
   - Kuratierte Teststellungen erstellen.
   Ergebnis: Nachvollziehbare taktische Fehlererkennung.
7. Phase 7 – Coaching-Ebene 2
   - Strategische Regeln nach den beschriebenen STRAT-IDs implementieren.
   - Stellungsm erkmalen, Bauernstruktur und Figurenaktivität auswerten.
   - Langfristige Pläne aus konkreten Stellungsmerkmalen ableiten.
   - Strategische Bewertungen gegen taktische Befunde priorisieren.
   Ergebnis: Eigenständige strategische Coaching-Logik auf Grundlage von Laskers Prinzipien.
8. Phase 8 – Coaching-Ebene 3
   - Regelbefunde und Engine-Ergebnisse in Erklärungen umwandeln.
   - Erklärungstypen und Qualitätsklassen implementieren.
   - Begründungen, alternative Züge und langfristige Pläne anzeigen.
   - Unsichere Analysen korrekt kennzeichnen.
   Ergebnis: Vollständige deutschsprachige Zugerklärungen.
9. Phase 9 – Qualitätssicherung und Dokumentation
   - Unit-, Integrations- und End-to-End-Tests vervollständigen.
   - Performance auf einem Referenzsystem messen.
   - Docker-Setup und CI konfigurieren.
   - OpenAPI, Architektur, Installation, Lizenzhinweise und Benutzerhandbuch fertigstellen.
   Ergebnis: Auslieferbares Gesamtprojekt.

## 15. Abnahmekriterien

Das Projekt gilt erst als abgeschlossen, wenn alle folgenden Punkte erfüllt sind.

## Abnahme-Checkliste

0 / 18

Das gesamte Projekt lässt sich anhand der Dokumentation reproduzierbar bauen.

Die Anwendung startet ohne externe Schachdienste und ohne Benutzerkonto.

Die mitgelieferte Java-Engine kann initialisiert werden und liefert legale Computerzüge.

Der Benutzer kann eine vollständige Partie mit legalen Zügen spielen.

Alle relevanten Sonderregeln einschließlich Rochade, En passant und Bauernumwandlung funktionieren.

Nach jedem eigenen Zug wird automatisch eine Analyse angefordert.

Die Analyse enthält Bewertung, Qualitätsklasse, beste Alternative und nachvollziehbare Varianten.

Es werden mehrere Kandidatenzüge und konkrete taktische sowie strategische Begründungen angezeigt.

Alle drei Coaching-Ebenen sind implementiert und durch eigene Tests abgesichert.

Strategische Regeln sind im Java-Code integriert und benötigen das Buch nicht zur Laufzeit.

Der Coach erklärt ausdrücklich, warum eine Bewertung zustande kommt.

Unsichere Analysen werden gekennzeichnet und nicht als gesicherte Tatsachen dargestellt.

Partien und Einstellungen können lokal im Browser gespeichert und wiederhergestellt werden.

Es existiert keine verpflichtende serverseitige Speicherung von Partien oder Benutzerprofilen.

Engine-Ausfälle, Timeouts und veraltete Analyseergebnisse werden kontrolliert behandelt.

SVG-Figuren, Lizenzhinweise und Drittanbieterinformationen sind vollständig dokumentiert.

Automatisierte Backend-, Frontend- und End-to-End-Tests sind vorhanden und erfolgreich.

Installations-, Architektur-, API- und Entwicklungsdokumentation sind vollständig.

## 16. Dokumentationspflichten

Der Coding Agent muss mindestens folgende Dateien bereitstellen:

```
README.md
docs/
├── REQUIREMENTS.md
├── ARCHITECTURE.md
├── API.md
├── COACHING_RULES.md
├── ENGINE_INTEGRATION.md
├── TESTING.md
├── DEPLOYMENT.md
├── USER_GUIDE.md
└── DEVELOPMENT.md

LICENSE
THIRD_PARTY_NOTICES.md
```

`COACHING_RULES.md` muss jede implementierte Regel mit ID, Zweck, Eingabemerkmalen, Erkennungsverfahren, Begründungslogik und Testfällen dokumentieren. Die Dokumentation soll die Regeln selbst beschreiben, nicht das vollständige Buch wiedergeben.

`ENGINE_INTEGRATION.md` dokumentiert die Engine-Version, UCI-Optionen, Startparameter, Lizenz, Suchparameter und den Umgang mit Engine-Fehlern.

`THIRD_PARTY_NOTICES.md` enthält die tatsächlich verwendeten Bibliotheken und Assets, deren Versionen, Quellen und Lizenzbedingungen.

## 17. Zusätzliche Vorgaben an den Coding Agenten

Der Coding Agent soll sich an folgende Regeln halten:

1. Keine Scheinimplementierungen. Nicht implementierte Funktionen dürfen nicht als fertig ausgegeben werden.
2. Keine Platzhalter für die zentrale Logik. Insbesondere müssen die Coaching-Regeln und ihre Begründungen tatsächlich implementiert sein.
3. Keine erfundenen Engine-Ergebnisse. Beispielbewertungen sind ausschließlich in Testdaten erlaubt.
4. Keine versteckte externe Abhängigkeit. Engine und Schachbibliotheken müssen reproduzierbar eingebunden sein.
5. Keine unnötige Eigenentwicklung. Für Schachregeln und Zugberechnung sind geprüfte Bibliotheken zu verwenden.
6. Klare Trennung. Die UI darf keine strategischen Regeln implementieren. Die Coaching-Logik darf nicht direkt von Vue oder Quasar abhängen.
7. Tests parallel zur Entwicklung. Jede Coaching-Regel benötigt eigene Tests, bevor sie als abgeschlossen gilt.
8. Lizenzprüfung vor Distribution. Die konkret verwendeten Engine-Binaries und SVG-Dateien müssen mit ihren jeweiligen Lizenzpflichten ausgeliefert werden.
9. Dokumentation muss zum Code passen. API-Verträge, Konfigurationen und Installationsanweisungen dürfen nicht veraltet sein.
10. Keine ungeprüften Abschlussmeldungen. Der Agent darf nur Tests und Builds als erfolgreich melden, die tatsächlich ausgeführt wurden.

## 18. Definition of Done

Die Anwendung ist fertig, wenn sie lokal gestartet werden kann, der Benutzer gegen die mitgelieferte Engine spielt und nach jedem eigenen Zug eine technisch fundierte, ausführliche und verständliche Erklärung erhält.

Der wesentliche Qualitätsmaßstab ist nicht allein die Spielstärke der Engine. Entscheidend ist, dass die Anwendung die Unterschiede zwischen einem taktischen Fehler, einer strategischen Ungenauigkeit und einem guten Zug verständlich macht und jeweils erklärt, welche konkrete Eigenschaft der Stellung die Bewertung verursacht hat.

Die strategischen Prinzipien aus Edward Laskers Buch müssen als eigenständige, versionierte und testbare Coaching-Regeln umgesetzt sein. Die Erklärungen müssen auf überprüfbaren Stellungseigenschaften und Engine-Analysen beruhen. Damit entsteht eine wartbare Coaching-Anwendung statt einer bloßen Schachoberfläche mit einer eingeblendeten Engine-Bewertung.