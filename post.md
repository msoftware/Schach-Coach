# Warum war mein Zug schlecht? – Ein Schach-Coach, der nicht nur Zahlen ausspuckt

Wer als Amateur Schach spielt und besser werden will, kennt das Problem: Man lässt die Partie durch eine Engine laufen,
und sie meldet „−1.8, besser war Sf3“. Stimmt sicher. Aber *warum*? Was hätte ich sehen müssen? Und was nehme ich für
die nächste Partie mit?

Genau dafür ist der **Schach-Coach** gebaut: eine Browser-App, in der du gegen eine lokal laufende Engine spielst und
nach **jedem eigenen Zug** eine Erklärung in verständlichem Deutsch bekommst – mit Qualitätsklasse, besserer
Alternative, konkreter Variante und einer Lernregel zum Mitnehmen.

Dieser Beitrag richtet sich an zwei Gruppen: an **Schach-Amateure**, die wissen wollen, was der Coach kann und wie man
mit ihm übt, und an **Softwareentwickler**, die sich für die Technik dahinter interessieren – und für die Fallstricke,
wenn man eine Engine-Bewertung in menschliche Sprache übersetzt.

---

## Für Schachspieler: Was der Coach dir zeigt

Du spielst eine ganz normale Partie gegen den Computer, in fünf Spielstärken von „Anfänger“ bis „Maximum“. Nach
jedem deiner Züge erscheint rechts (auf dem Handy direkt unter dem Brett) das **Zug-Coaching**:

- **Qualitätsklasse:** Bestzug, Sehr gut, Gut, Ungenau, Fehler, Grober Fehler – oder „Erzwungen“, wenn es praktisch nur
  einen sinnvollen Zug gab.
- **Warum?** Die wichtigste Begründung in einem Satz, z. B.: *„Dein Springer e5 ist in Gefahr: Der Gegner kann die
  Figur mit Springer c6 schlagen, und keine deiner Figuren schützt sie.“*
- **Die Alternative** – ehrlich abgestuft: „Bessere Alternative“, „Etwas genauer wäre gewesen“ oder „Gleich gute
  Alternative“, wenn dein Zug genauso stark war.
- **Wie viel hat es gekostet?** In Worten statt Centipawns: „Verschenkt gegenüber dem besten Zug: etwa 2½ Bauern.“
- **Details:** Was passiert durch deinen Zug? Wie kann der Gegner antworten? Wie geht es weiter (Pläne für beide
  Seiten)? Und: *Was kannst du dir merken?*
- **Kandidatenzüge und Varianten** zum Durchklicken – auf einem eigenen Brett, ohne die Partie zu verändern.

Unter dem Coaching zeigt eine **Bewertungsleiste**, wer gerade wie weit vorne liegt. Sie wird nach jedem Zug
aktualisiert – auch nach dem des Computers.

### Die Erklärungen kommen aus Schachwissen, nicht nur aus der Engine

Die Engine liefert Bewertungen und Varianten. Die *Begründung* kommt aus 28 Coaching-Regeln in zwei Ebenen:

- **Taktik:** Materialgewinn und -verlust, ungedeckte Figuren, Gabeln, Fesselungen, Spieße, Abzugsangriffe,
  Mattdrohungen, Zwischenzüge, Abwicklungen und mehr.
- **Strategie** nach Edward Laskers Klassiker *Chess Strategy*: Entwicklung, Zentrum, Königssicherheit,
  Bauernstruktur, offene Linien, Vorposten, Initiative, Endspieltechnik und langfristige Pläne.

Taktik hat dabei Vorrang: Wenn dein Zug eine Figur einstellt, lobt dich der Coach nicht dafür, dass du „das Zentrum
kontrollierst“.

### So übst du damit am besten

1. **Spiel gegen eine Stufe, die dich fordert, aber nicht erdrückt.** „Leicht“ und „Mittel“ machen Fehler, die du
   ausnutzen kannst – genau dabei lernst du am meisten.
2. **Lass „Auf Coaching warten“ eingeschaltet.** Der Computer zieht erst, wenn die Analyse deines Zuges fertig ist. So
   liest du die Erklärung, solange die Stellung noch vor dir steht.
3. **Lies bei Fehlern die Details.** Der Abschnitt „Wie kann der Gegner antworten?“ zeigt die Widerlegung Zug für Zug.
4. **Nimm Züge zurück und probiere die Alternative.** Mit „Zurück“ und „Vor“ kannst du vergleichen, was passiert wäre.
5. **Achte auf wiederkehrende Lernregeln.** Wenn du dreimal hintereinander „Prüfe vor jedem Zug, ob deine Figuren
   gedeckt sind“ liest, weißt du, woran du arbeiten musst.

Alles bleibt bei dir: Es gibt keine Konten, Partien und Statistiken liegen nur im Browser. Auf dem Handy lässt sich der
Coach mit einer Hand bedienen und per „Zum Startbildschirm hinzufügen“ wie eine App starten.

---

## Für Entwickler: Wie das Ganze gebaut ist

```
Vue 3 + Quasar + Pinia (TypeScript)        Spring Boot 4.1 (Java 21)
┌──────────────────────────────┐  REST   ┌───────────────────────────────────────────┐
│ Brett, Coaching, Bewertungs- │ ──────▶ │ GameService ── ChessRulesService (Chesslib)│
│ leiste, Zugliste             │         │ AnalysisService ── MoveAnalyzer            │
│ IndexedDB: Partien, Cache    │         │   ├─ Calvin-Engine (UCI, eigener Prozess) │
└──────────────────────────────┘         │   ├─ Taktik-Regeln  (TACT-001 … 016)      │
                                         │   ├─ Strategie-Regeln (STRAT-001 … 012)   │
                                         │   └─ ExplanationBuilder → deutsche Texte  │
                                         └───────────────────────────────────────────┘
```

- **Engine:** [Calvin](https://github.com/kelseyde/calvin-chess-engine) 6.1.1, eine Java-Engine (MIT-Lizenz), die als
  eigener Prozess per UCI angesprochen wird. Es laufen zwei Instanzen: eine für Computerzüge, eine für Analysen.
- **Regeln:** Chesslib prüft jede Stellung und jeden Zug – auch jeden Zug, den die Engine vorschlägt. Was nicht legal
  ist, wird nie angezeigt.
- **Spielstärke** wird ausschließlich über nachprüfbare Engine-Parameter gesteuert (Knoten, Tiefe, Zeit; bei den
  schwächsten Stufen eine zufällige Teilmenge der legalen Züge, aus der die Engine den besten wählt). Es gibt keine
  „gespielten Patzer“ ohne Rechnung.
- **Analyse in Stufen:** Bewertung der Stellung vor dem Zug, Vorauswahl aller legalen Züge mit kleinem Budget, dann
  tiefe Bewertung der besten Kandidaten plus des gespielten Zuges – alles unter gleichen Suchbedingungen, damit der
  Vergleich fair ist.
- **Regeln als Spring-Komponenten:** Jede Regel liefert strukturierte `CoachingFinding`s mit Regel-ID, Polarität,
  Gewichtung und Beweiskette. Der `ExplanationBuilder` entscheidet, welcher Befund die Hauptbegründung wird.

### Lektionen aus dem Probespielen

Unit-Tests mit kuratierten Stellungen sind gut. Aber die interessantesten Fehler zeigten sich erst, als ich eine echte
Partie im Browser gespielt und jede Empfehlung mit einer deutlich tieferen Engine-Rechnung gegengeprüft habe. Drei
davon sind lehrreich über dieses Projekt hinaus.

#### 1. Ein flacher Vergleich kann dem Spieler einen schlechteren Zug empfehlen

In einer Stellung bewertete der Coach die Rochade nur als „Gut“ und empfahl stattdessen `6.Lb3`. Bei 80 000 Knoten je
Kandidat sah `Lb3` besser aus. Erst ab etwa Tiefe 20 zeigt sich die Widerlegung `6…Sg4!` mit Angriff auf h2 und f2.
Gegenüber dem Spieler ist das der schlimmste Fall: Der Coach redet ihm einen guten Zug aus.

Die Lösung ist keine pauschal tiefere Suche, denn die würde jede Analyse verlangsamen. Stattdessen werden **knappe
Entscheidungen nachgeprüft**: Will der Coach einen anderen Zug empfehlen und liegt dieser höchstens einen Bauern vorn,
rechnet er beide Züge mit vierfachem Budget nach. Und zwar so lange, bis der Zug, der am Ende vorn liegt, unter
denselben Bedingungen geprüft ist wie der gespielte:

```java
while (verified.size() < MAX_VERIFICATIONS) {
    Evaluated played = deep.get(playedUci);
    Evaluated best = bestOf(deep);
    if (best == played || gap(best, played) > verifyMargin) break;
    String next = !verified.contains(playedUci) ? playedUci : best.uci();
    if (verified.contains(next)) break;           // beide gleich gründlich geprüft
    deep.put(next, evaluate(next, verifyLimits)); // 4-faches Budget
    verified.add(next);
}
```

Der erste Entwurf prüfte nur den gespielten Zug und die beiden damals besten Konkurrenten nach. Und prompt rutschte einer
davon ab, und ein nur flach bewerteter Zug rückte nach vorn. Der gewann dann den Vergleich mit ungleichen Mitteln.
Wichtig ist also nicht nur *mehr* Rechnung, sondern **gleiche Bedingungen für die Züge, die man vergleicht**.

#### 2. Zurückschlagen ist kein Materialgewinn

Der Gegner schlägt deinen Läufer, du schlägst seinen Springer zurück. Die Regel „Materialgewinn“ verglich die Bilanz
mit der Stellung *direkt vor deinem Zug* und gratulierte: *„Mit diesem Zug gewinnst du so viel wie eine Figur.“* Für
einen Anfänger ist das fatal, denn er lernt etwas Falsches.

Richtig ist der Vergleich mit der Stellung *vor dem Schlagzug des Gegners*. Jetzt erklärt der Coach: *„Du schlägst
zurück: Der Gegner hat gerade einen Läufer von dir geschlagen, jetzt nimmst du einen Springer. Damit holst du dir das
Material zurück – gewonnen hast du dadurch aber nichts.“* Derselbe Denkfehler steckte in der Tauschbilanz („du gibst die
Dame und bekommst Dame und Läufer – das lohnt sich“) und im Plan-Hinweis („du hast +4 mehr Material“) mitten in einem
Abtausch. Die Lehre: Wer Engine-Varianten in Sätze übersetzt, muss genau wissen, **ab welchem Punkt er zählt**.

#### 3. Cache-Schlüssel brauchen eine Version für die Logik, nicht nur für die Daten

Analysen werden im Browser zwischengespeichert. Der Schlüssel besteht aus Stellung, Zug, Engine-Version, Profil und
Regelversion. Als die Erklärungstexte überarbeitet wurden, blieb die Regelversion auf `1.0.0`. Wer die App vorher schon
genutzt hatte, bekam deshalb weiter die alten, technischen Texte („Zentrumskontrolle: du 0 → 3, Gegner 0“), und zwar
ohne jeden Hinweis. Jetzt steht ausdrücklich in der Konfiguration, dass jede Änderung an Regeln, Texten oder
Analyse-Logik die Version erhöhen muss.

### Weitere Details, die Spaß gemacht haben

- **Mehrere Fenster, mehrere Personen:** Jedes Browserfenster spielt seine eigene Partie in einem eigenen Speicherplatz.
  Offene Fenster stimmen sich per `BroadcastChannel` ab, damit ein neues Fenster nie die Partie eines noch offenen
  Fensters übernimmt. Im Backend gelten Rate-Limits pro Partie, damit sich Personen hinter derselben IP-Adresse nicht
  gegenseitig ausbremsen.
- **Bewertungsleiste:** Ein eigener Endpunkt bewertet die aktuelle Stellung mit fester Suchtiefe, unabhängig von der
  Spielstärke des Computers. Kommt eine Antwort zu spät, weil schon der nächste Zug gespielt wurde, wird sie verworfen.
- **Touch-Bedienung:** Auf dem iPhone ließ sich eine Figur zunächst nicht antippen, nur ziehen. Ein `preventDefault()`
  im `pointerdown` unterdrückt in Safari den folgenden Klick. Aufgefallen ist das erst mit E2E-Tests in WebKit, die
  seitdem fest dazugehören.
- **Tests:** über 200 Backend-Tests (inklusive Integrationstests mit der echten Engine), Vitest-Tests im Frontend und
  Playwright-Tests in Chromium, Firefox, auf Android- und iPhone-Viewports.

---

## Ausprobieren

Voraussetzungen: JDK 21 und Node.js 22+.

```bash
# Backend (prüft die Engine-Prüfsumme und startet zwei Calvin-Prozesse)
cd backend && ./gradlew bootRun

# Frontend (leitet /api an das Backend weiter)
cd frontend && npm ci && npm run dev
```

Danach `http://localhost:5173` öffnen. Alternativ startet `docker compose up --build` alles zusammen hinter nginx.

---

## Fazit

Eine Engine sagt dir, *dass* ein Zug schlecht war. Ein guter Coach sagt dir, *warum* – und muss dabei mindestens so
ehrlich sein wie die Engine. Die Engine anzubinden war beim Bau des Schach-Coaches nicht das Schwierigste. Schwieriger
war, ihre Zahlen so in Sätze zu übersetzen, dass sie **stimmen**: Eine Empfehlung muss tief genug geprüft sein, ein
Zurückschlagen darf nicht als Gewinn gelten, und ein unvermeidbarer Verlust ist kein „Opfer“.

Wenn du Schach spielst und besser werden willst: Spiel ein paar Partien, lies die Erklärungen und nimm dir pro Partie
eine Lernregel mit. Und wenn du Software entwickelst: Spiel deine eigene App, bevor es deine Nutzer tun. Prüfe sie
dabei gegen eine unabhängige, tiefere Quelle. Die Tests waren grün, trotzdem hat erst das Probespielen die wichtigsten
Fehler gefunden.
