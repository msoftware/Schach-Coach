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

Die **Zugliste** zeigt jeden Halbzug in einer eigenen Zeile und in Worten, z. B.
„2. Weiß (du): Springer von g1 nach f3“ oder „3… Schwarz: Springer von c6 schlägt Springer auf e5“. Dahinter steht in
Klammern die übliche Kurznotation (z. B. `Nf3`) und bei deinen Zügen die Bewertung (z. B. „Bestzug“, „Fehler“).
„3.“ steht für einen Zug von Weiß, „3…“ für die Antwort von Schwarz im selben Zugpaar.

Du kannst jeden Zug anklicken oder mit den Pfeilen blättern; das Brett zeigt dann die damalige
Stellung (nur Ansicht). „Zur aktuellen Stellung“ (⏭) kehrt zum Spiel zurück.

## Auf dem Handy

- **Ziehen:** Figur antippen, dann das Zielfeld antippen – oder die Figur mit dem Finger ziehen (sie wird dabei
  vergrößert über dem Finger angezeigt). Wischen über leere Felder scrollt die Seite.
- **Aktionsleiste unten** (im Daumenbereich): Neu, Zurück, Vor, Tipp und „Mehr“ (Brett drehen, Einstellungen,
  Aufgeben).
- **Direkt unter dem Brett** stehen die Bewertungsleiste und eine Kurzfassung des Coachings (Qualitätsklasse und
  Begründung). Antippen der Kurzfassung springt zur ausführlichen Analyse; der runde Pfeil-Knopf führt zurück zum Brett.
- **Quer gehalten** stehen Brett und Coaching nebeneinander.
- **Als App:** Über „Zum Startbildschirm hinzufügen“ im Browsermenü lässt sich der Schach-Coach wie eine App starten
  (ohne Adressleiste).

## Das Coaching

Nach jedem eigenen Zug startet automatisch die Analyse. Die **Zusammenfassung** zeigt:

- **Qualitätsklasse:** Bestzug, Sehr gut, Gut, Ungenau, Fehler, Grober Fehler, Erzwungen oder Unsicher;
- deinen Zug in Worten (z. B. „Springer von f3 schlägt Bauer auf e5“);
- die **wichtigste Begründung** („Warum?“);
- die **bessere Alternative**, ebenfalls in Worten;
- die **Bewertung in Worten** vor und nach deinem Zug (z. B. „ausgeglichen“, „du stehst klar besser“,
  „der Gegner steht deutlich besser“) und wie viel du gegenüber dem besten Zug verschenkt hast
  (z. B. „etwa 2½ Bauern“);
- einen Hinweis, wenn sich der Computer nicht sicher ist („vorläufig“).

Unter der Zusammenfassung zeigt die **Bewertungsleiste** („Wer liegt vorne?“), wie die Partie gerade steht: links Weiß
(helle Fläche), rechts Schwarz (dunkle Fläche). Der blaue Schieber wandert zu der Seite, die besser steht; die rote
Markierung in der Mitte bedeutet „ausgeglichen“. Darunter steht die Lage in Worten, z. B. „Weiß liegt leicht vorne
(+0.80)“ oder „Schwarz setzt in 3 Zügen matt“, und ob du oder der Computer vorne liegt. Die Leiste wird nach jedem Zug
aktualisiert – nach deinem und nach dem des Computers, ebenso nach Zurücknehmen und Wiederherstellen.

„**Details ansehen**“ öffnet:

- **Erklärung:** Antworten auf sieben Fragen – Was war gut, was war schlecht? Was passiert durch deinen Zug? Wie kann
  der Gegner antworten? Was wäre besser gewesen? Warum ist das besser? Wie geht es weiter? Was kannst du dir merken?
  Alle Züge werden in Worten beschrieben, die Kurznotation steht in Klammern. Hinter jeder Aussage steht, woher sie
  kommt („Computer“ = vom Schachcomputer berechnet; „Regel …“ = aus den Coaching-Regeln abgeleitet).
- **Andere Züge:** weitere Züge, die der Computer geprüft hat, mit Bewertung in Worten und Vergleich mit deinem Zug.
  Ein Klick zeigt die **Fortsetzung auf dem Brett**; mit den Pfeilen spielst du sie Zug für Zug durch.
  Das verändert die Partie nicht. „Variante verlassen“ kehrt zur Partie zurück.
- **Erkenntnisse:** Taktik (direkte Drohungen und Schlagzüge) und Strategie (langfristige Vor- und Nachteile nach
  Laskers Prinzipien). „nachgerechnet“ heißt: durch Züge bestätigt; „Faustregel“ heißt: allgemeine Erfahrungsregel.
  Beim Überfahren werden die betroffenen Felder markiert.
- **Pläne:** was du und der Gegner als Nächstes vorhaben könnt, jeweils mit Bezug auf die Stellung.
- **Zahlen:** die genauen Werte für Fortgeschrittene (Bewertungen in Bauerneinheiten, Rechentiefe, Suchgrenzen);
  „Varianten vertiefen“ startet eine gründlichere Analyse.

Hinweis: Die gezeigten Fortsetzungen sind Vorschläge des Computers, keine erzwungenen Zugfolgen – der Gegner kann auch
anders spielen.

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
