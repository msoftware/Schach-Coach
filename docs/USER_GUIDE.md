# Benutzerhandbuch

## Partie spielen

- Beim Öffnen wird die zuletzt gespeicherte Partie wiederhergestellt oder eine neue Partie gestartet. Standardmäßig
  spielst du Weiß gegen den Computer auf der Stufe „Mittel“.
- **Ziehen:** Figur anklicken bzw. antippen – die legalen Zielfelder werden markiert (Punkt bzw. Ring bei Schlagzügen) –,
  dann das Zielfeld wählen. Alternativ die Figur mit Maus oder Finger ziehen.
- **Tastatur:** Mit Tab auf das Brett, Pfeiltasten bewegen den Fokus, Enter/Leertaste wählt aus bzw. zieht, Esc bricht ab.
- **Umwandlung:** Erreicht ein Bauer die letzte Reihe, wählst du Dame, Turm, Läufer oder Springer.
- Markierungen: letzter Zug (gelb mit Rahmen), ausgewählte Figur (blauer Rahmen), König im Schach (rot mit „!“).
- Während der Computer rechnet, steht „Der Computer rechnet …“ unter dem Brett; Züge sind dann gesperrt.

## Steuerung

| Schaltfläche | Funktion |
|---|---|
| Neu | neue Partie (mit Rückfrage, wenn eine Partie läuft) |
| Zurück | eigenen letzten Zug und die Antwort des Computers zurücknehmen |
| Vor | zurückgenommene Züge wiederherstellen |
| Tipp | 1. Klick: welche Figur ziehen; 2. Klick: konkreter Zug (laut Engine) |
| Aufgeben | Partie verloren geben (die Analyse bleibt verfügbar) |
| ⇅ | Brett drehen |
| ⚙ | Einstellungen |

In der **Zugliste** kannst du jeden Zug anklicken oder mit den Pfeilen blättern; das Brett zeigt dann die damalige
Stellung (nur Ansicht). „Zur aktuellen Stellung“ (⏭) kehrt zum Spiel zurück.

## Das Coaching

Nach jedem eigenen Zug startet automatisch die Analyse. Die **Zusammenfassung** zeigt:

- **Qualitätsklasse:** Bestzug, Sehr gut, Gut, Ungenau, Fehler, Grober Fehler, Erzwungen oder Unsicher;
- die **wichtigste Begründung** („Warum?“);
- die **bessere Alternative** mit Bewertung;
- die **Bewertung vorher → nachher** (aus deiner Sicht, z. B. „+0.35“ = leichter Vorteil für dich; „M3“ = Matt in 3);
- die **Zuverlässigkeit** der Analyse. „vorläufig“ bedeutet: Die Suche war nicht tief genug für eine sichere Einordnung.

„**Details ansehen**“ öffnet:

- **Erklärung:** Antworten auf sieben Fragen – Was war gut/schlecht? Welche Konsequenz? Was hätte der Gegner gespielt?
  Welcher Zug wäre besser? Warum? Welche Pläne? Welches Prinzip merken? Jede Aussage nennt ihre Quelle
  („Engine“ = berechnet von der Schach-Engine; „Taktik-/Strategieregel …“ = abgeleitet von der Coaching-Logik).
- **Kandidaten:** mehrere Züge mit Bewertung, Suchtiefe, Hauptvariante, taktischen Motiven und strategischen Vor- und
  Nachteilen. Ein Klick zeigt die **Variante auf dem Brett**; mit den Pfeilen spielst du sie Zug für Zug durch.
  Varianten verändern die Partie nicht. „Variante verlassen“ kehrt zur Partie zurück.
- **Befunde:** taktische Motive (Ebene 1) und strategische Bewertung nach Laskers Prinzipien (Ebene 2), jeweils mit
  Kennzeichnung „belegt“ oder „Heuristik“. Beim Überfahren werden die betroffenen Felder markiert.
- **Pläne:** langfristige Pläne für dich und den Gegner, jeweils mit dem Stellungsmerkmal, auf dem sie beruhen.
- **Engine:** Bewertungen, Verlust gegenüber dem besten Zug, Suchgrenzen, Knoten, Zeit, Regelversion;
  „Varianten vertiefen“ startet eine gründlichere Analyse.

Hinweis: Varianten sind die von der Suche gefundenen Hauptvarianten, keine erzwungenen Zugfolgen.

## Einstellungen

- **Spielstärke:** Anfänger, Leicht, Mittel, Stark, Maximal – mit Angabe der Engine-Parameter.
- **Gründlichkeit der Analyse:** Schnell, Normal, Gründlich. Nach einer Änderung werden ältere Analysen als „veraltet“
  markiert und können neu berechnet werden.
- **Meine Farbe** (gilt ab der nächsten Partie), **Computer wartet auf das Coaching**, **Koordinaten anzeigen**.
- **Lernstatistik:** Anzahl analysierter Züge und wiederkehrende Fehlermuster – nur in diesem Browser gespeichert.

## Datenschutz

Es gibt keine Konten. Partien, Einstellungen, Analysen und Lernstatistik werden ausschließlich in deinem Browser
gespeichert (IndexedDB/localStorage). Die Engine läuft lokal auf dem Server dieser Anwendung; es werden keine Daten an
externe Dienste übertragen und keine Telemetrie erhoben. Der Server hält die laufende Partie nur im Arbeitsspeicher.
Unter Einstellungen → „Lokale Daten löschen“ entfernst du alle gespeicherten Daten. Kann der Browser nicht speichern
(z. B. im privaten Modus), erscheint ein Hinweis – die Partie läuft dann nur bis zum Neuladen.
