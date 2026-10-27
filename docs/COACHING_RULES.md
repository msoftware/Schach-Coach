# Coaching-Regeln

Dieses Dokument beschreibt jede implementierte Regel der Coaching-Logik (Regelversion **1.1.0**). Die Angaben zu ID, Titel, Zweck, Eingabemerkmalen und Lernprinzip werden direkt aus dem Regelkatalog der laufenden Anwendung (`GET /api/coaching/rules`) übernommen; Erkennungsverfahren, Begründungslogik und Testfälle beschreiben die Implementierung in `backend/src/main/java/de/chesscoach/coaching/`.

## Grundsätze

- **Drei Ebenen:** Ebene 1 (Taktik, `TACT-*`), Ebene 2 (Strategie nach Edward Lasker, `STRAT-*`), Ebene 3 (Erklärung, `ExplanationBuilder`).
- **Eingabe:** `PositionContext` – Stellung vor/nach dem Zug, geprüfte Variante (nur legale Züge, Chesslib), Bewertungen aus Sicht des Benutzers, Variante des besten Zuges, vorherige Züge.
- **Ausgabe:** strukturierte `CoachingFinding`s mit Regel-ID, Kategorie, Quelle (`ENGINE`/`COACHING_RULE`), Gewichtung (`INFO`…`CRITICAL`), Wirkung (positiv/negativ/neutral), abgestufter Bewertung (`score` −4…+4), Felder, Figuren, Beweiskette und Lernprinzip.
- **Gesichert vs. Heuristik:** `VERIFIED` nur, wenn legale Züge oder die Engine-Variante den Befund bestätigen; sonst `HEURISTIC` mit vorsichtiger Formulierung.
- **Vorrang der Taktik:** Liegt ein belegter taktischer Nachteil (≥ MEDIUM) vor oder klassifiziert die Engine den Zug als Fehler/groben Fehler, werden positive strategische Heuristiken als `overriddenByTactics` markiert und nie als Hauptbegründung verwendet.
- **Konfiguration:** einzelne Regeln lassen sich über `chesscoach.coaching.disabled-rules` deaktivieren; neue Regeln werden als Spring-Komponente registriert (`TacticalRule`/`StrategicRule`), ohne die Engine-Integration zu ändern.
- **Strategische Regeln** verwenden keine Engine-spezifischen Klassen, sondern nur Stellungsmerkmale (`domain.position`) und die bereits normalisierten Bewertungen.

## TACT-001 – Materialgewinn

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Erkennt Materialgewinn durch den Zug bzw. verpassten Materialgewinn im Vergleich zum besten Zug.

**Eingabemerkmale:**

- Materialbilanz vor dem Zug
- Materialbilanz am ruhigen Punkt der Engine-Variante
- Variante des besten Zuges
- statische Abtauschbewertung des Zielfelds

**Erkennungsverfahren:** Materialbilanz am „ruhigen Punkt“ der geprüften Variante (spätester Punkt innerhalb von 12 Halbzügen, nach dem nicht unmittelbar weitergeschlagen wird) wird mit der Bilanz vor dem Zug verglichen. Zusätzlich wird die Variante des besten Zuges ausgewertet. Ohne Engine-Variante nur statische Abtauschbewertung (als Heuristik).

**Begründungslogik:** Positiv und belegt, wenn die Variante ≥ 1 Bauerneinheit gewinnt und die Engine-Bewertung nicht um mehr als 100 cp fällt. Negativ („Materialgewinn verpasst“), wenn der beste Zug ≥ 2 Einheiten und mindestens 2 Einheiten mehr als der gespielte Zug gewinnt.

**Lernprinzip:** Prüfe vor jedem Zug, ob der Gegner Figuren ungedeckt gelassen hat – und ob du sie wirklich sicher schlagen kannst.

**Testfälle:** `TacticalRulesTest.Tact001MaterialGain: recognisesVerifiedGain, recognisesMissedGain, doesNotClaimGainWhenLineDoesNotConfirmIt`

## TACT-002 – Materialverlust

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Erkennt vermeidbaren Materialverlust anhand der Engine-Variante des gespielten Zuges.

**Eingabemerkmale:**

- Materialbilanz vor dem Zug
- Materialbilanz am ruhigen Punkt der Variante
- Materialbilanz der Variante des besten Zuges
- Bewertung vor und nach dem Zug

**Erkennungsverfahren:** Materialverlust am ruhigen Punkt der Variante des gespielten Zuges, verglichen mit der Variante des besten Zuges. Ist der gespielte Zug selbst der beste, gilt der Verlust als unvermeidbar bzw. bewusstes Opfer.

**Begründungslogik:** Negativ und belegt mit Variante, gegnerischer Antwort, Schlagfeldern und Materialbilanz. Bleibt die Engine-Bewertung nahezu gleich (Verlust < 80 cp), wird das als „Materialopfer mit Kompensation“ (neutral) erklärt.

**Lernprinzip:** Frage dich vor jedem Zug: Welche Figuren stehen nach meinem Zug angegriffen, und sind sie ausreichend gedeckt?

**Testfälle:** `Tact002MaterialLoss: recognisesAvoidableLoss, gambitWithCompensationIsNotAnError, bestMoveLosingMaterialIsNotReportedAsAvoidable`

## TACT-003 – Ungedeckte Figur

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Erkennt eigene Figuren, die durch den Zug angegriffen und unzureichend gedeckt sind.

**Eingabemerkmale:**

- Angreifer und Verteidiger jedes Feldes
- statische Abtauschbewertung
- legale Schlagzüge des Gegners
- erste gegnerische Antwort der Engine-Variante

**Erkennungsverfahren:** Für jede eigene Figur (außer König) nach dem Zug: statische Abtauschbewertung (SEE mit Röntgenangriffen) ≥ 100 cp für den Gegner, ein legaler Schlagzug des Gegners existiert, und die Figur war vorher nicht en prise bzw. ist die gezogene Figur.

**Begründungslogik:** Belegt, wenn die Engine in der Hauptvariante tatsächlich schlägt; sonst als Heuristik mit dem Hinweis, dass die Engine nicht sofort schlägt.

**Lernprinzip:** Zähle bei jeder angegriffenen Figur Angreifer und Verteidiger – und beachte dabei den Wert der beteiligten Figuren.

**Testfälle:** `Tact003HangingPieces: verifiedWhenEngineCaptures, heuristicWithoutConfirmingLine, quietDevelopingMoveHasNoHangingPiece`

## TACT-004 – Figurenangriff

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Erkennt neu erzeugte Angriffe der gezogenen Figur und übersehene Angriffe des Gegners.

**Eingabemerkmale:**

- Angriffe der gezogenen Figur vor und nach dem Zug
- Werte von Angreifer und Ziel
- statische Abtauschbewertung vor und nach dem Zug
- Antwort der Engine-Variante

**Erkennungsverfahren:** (a) Relevante Ziele der gezogenen Figur (wertvoller als der Angreifer oder ungedeckt), die vorher nicht angegriffen waren. (b) Eigene Figuren, die vor dem Zug en prise waren und es danach mit legalem Schlagzug bleiben, ohne dass der Zug Schach gibt oder gleichwertiges Material schlägt.

**Begründungslogik:** Angriff positiv (belegt, wenn der Folgezug der Variante das Ziel schlägt). „Angriff ignoriert“ negativ (belegt, wenn der Gegner in der Variante schlägt).

**Lernprinzip:** Reagiere auf jeden gegnerischen Angriff: Ziehe die Figur weg, decke sie oder schaffe eine stärkere Gegendrohung.

**Testfälle:** `Tact004PieceAttack: newAttackOnUndefendedPiece, ignoredAttack, movingTheAttackedPieceAwayIsNotIgnoring`

## TACT-005 – Doppelangriff

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Erkennt Doppelangriffe langschrittiger Figuren und kombinierte Drohungen mehrerer Figuren.

**Eingabemerkmale:**

- relevante Ziele jeder Figur vor und nach dem Zug
- Wert und Deckung der Ziele
- Folgezüge der Engine-Variante

**Erkennungsverfahren:** Dame, Turm, Läufer oder König greifen nach dem Zug ≥ 2 relevante Ziele an (mindestens eines neu), oder gezogene Figur und abgedeckte Linie erzeugen zusammen zwei Drohungen. Dieselbe Prüfung wird für die gegnerische Antwort der Variante durchgeführt.

**Begründungslogik:** Belegt, wenn die Variante eines der Ziele schlägt; gegnerische Doppelangriffe stammen aus der Engine-Hauptvariante und sind als solche gekennzeichnet.

**Lernprinzip:** Doppelangriffe entstehen oft mit der Dame: Prüfe, ob sie nach einem Zug zwei ungedeckte Figuren oder König und Figur zugleich angreift.

**Testfälle:** `Tact005DoubleAttack: queenAttacksKingAndRook, opponentDoubleAttackAfterQuietMove, knightForkIsNotCountedAsDoubleAttack`

## TACT-006 – Fesselung

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Erkennt neu entstandene Fesselungen für und gegen den Benutzer.

**Eingabemerkmale:**

- Linien langschrittiger Figuren
- Werte der vorderen und hinteren Figur
- legale Züge der gefesselten Figur

**Erkennungsverfahren:** Linienanalyse aller langschrittigen Figuren: vordere gegnerische Figur, dahinter König (absolute Fesselung) oder wertvollere Figur (relative Fesselung); nur neu entstandene Fesselungen. Absolute Fesselungen werden geprüft, indem alle legalen Züge der gefesselten Figur auf der Linie bleiben müssen.

**Begründungslogik:** Positiv für eigene, negativ für gegen den Benutzer gerichtete Fesselungen; nennt Angreifer, gefesselte Figur und Figur dahinter.

**Lernprinzip:** Eine gefesselte Figur ist ein Angriffsziel: Greife sie mit weiteren Figuren, besonders mit Bauern, an.

**Testfälle:** `Tact006Pin: absolutePinIsVerifiedByLegalMoves, walkingIntoPin, noPinWithoutLine`

## TACT-007 – Spieß

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Erkennt Spieße durch den eigenen Zug und durch die gegnerische Antwort.

**Eingabemerkmale:**

- Linien langschrittiger Figuren
- Werte von vorderer und hinterer Figur
- Deckung der hinteren Figur
- Folgezüge der Engine-Variante

**Erkennungsverfahren:** Linienanalyse: vordere Figur wertvoller (oder König) als die hintere, Angreifer weniger wert als die vordere Figur, hintere Figur ungedeckt oder mindestens so wertvoll wie der Angreifer; ausgelöst von der gezogenen Figur bzw. der gegnerischen Antwort.

**Begründungslogik:** Belegt, wenn der übernächste Halbzug der Variante die hintere Figur schlägt.

**Lernprinzip:** Stelle König und Dame oder Dame und Turm nicht ungeschützt auf dieselbe Linie oder Diagonale.

**Testfälle:** `Tact007Skewer: rookSkewersKingAndQueen, noSkewerWhenNothingBehind`

## TACT-008 – Abzugsangriff

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Erkennt Abzugsangriffe und Abzugsschach durch den eigenen Zug und durch die gegnerische Antwort.

**Eingabemerkmale:**

- Linien eigener langschrittiger Figuren vor und nach dem Zug
- Startfeld der gezogenen Figur
- relevante Ziele der geöffneten Linie

**Erkennungsverfahren:** Langschrittige eigene Figuren (außer der gezogenen), deren Linie über das Startfeld führt und die danach ein relevantes Ziel angreifen, das sie vorher nicht angriffen.

**Begründungslogik:** Positiv; Abzugsschach wird gesondert benannt. Gegnerische Abzugsangriffe aus der Hauptvariante negativ.

**Lernprinzip:** Steht eine eigene Figur zwischen deinem Läufer, Turm oder deiner Dame und einem gegnerischen Ziel, kann ihr Wegzug einen doppelten Angriff erzeugen.

**Testfälle:** `Tact008DiscoveredAttack: discoveredCheck, noDiscoveryWhenLineStaysClosed`

## TACT-009 – Zwischenzug

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Erkennt gespielte und verpasste Zwischenzüge nach einem gegnerischen Schlagzug.

**Eingabemerkmale:**

- vorheriger Zug des Gegners (Schlagzug)
- verfügbare Rückschläge
- Schach und Drohungen des Zuges
- Engine-Bewertung des gespielten und des besten Zuges

**Erkennungsverfahren:** Nur nach einem gegnerischen Schlagzug mit verfügbarem Rückschlag. Zwischenzug = kein Rückschlag, sondern Schach oder neuer Angriff auf eine Figur ≥ Turm.

**Begründungslogik:** Positiv, wenn der Zwischenzug laut Engine höchstens 30 cp schlechter als der beste Zug ist. „Zwischenzug verpasst“, wenn direkt zurückgeschlagen wurde, der beste Zug ein Zwischenzug ist und der Verlust ≥ 80 cp beträgt.

**Lernprinzip:** Schlage nicht automatisch zurück: Prüfe zuerst, ob ein Schach oder eine stärkere Drohung dazwischen möglich ist.

**Testfälle:** `Tact009Intermezzo: checkInsteadOfRecapture, missedIntermezzo, notApplicableWithoutPreviousCapture`

## TACT-010 – Mattdrohung

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Erkennt Matt, Mattdrohungen und erzwungene Mattfolgen sowie verpasste Matts.

**Eingabemerkmale:**

- legale Züge nach dem Zug
- Nullzug-Prüfung auf Matt in einem Zug
- Mattbewertungen der Engine für gespielten und besten Zug

**Erkennungsverfahren:** Schachmatt durch den Zug (Regelprüfung), Mattbewertung der Engine nach dem Zug, Nullzug-Prüfung „hätte ich noch einmal den Zug, gäbe es Matt in 1?“ über die legalen Züge, sowie Mattbewertung des besten Zuges.

**Begründungslogik:** Matt/erzwungenes Matt/Mattdrohung positiv und belegt; „Matt verpasst“ kritisch negativ mit Variante des besten Zuges.

**Lernprinzip:** Suche bei jedem Zug zuerst nach Schachgeboten: Sie sind die forcierendsten Züge und führen oft zum Matt.

**Testfälle:** `Tact010MateThreat: checkmate, quietMoveCreatesMateThreat, missedMate, noThreatInQuietPosition`

## TACT-011 – Mattgefahr

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Erkennt übersehene Mattgefahr und erfolgreich abgewehrte Mattdrohungen.

**Eingabemerkmale:**

- legale Antworten des Gegners nach dem Zug
- Nullzug-Prüfung vor dem Zug
- Mattbewertungen der Engine vor und nach dem Zug

**Erkennungsverfahren:** Legale Antworten des Gegners nach dem Zug werden auf Matt in 1 geprüft; Mattbewertung der Engine gegen den Benutzer, während der beste Zug kein Matt zulässt; Nullzug-Prüfung vor dem Zug für bestehende Mattdrohungen.

**Begründungslogik:** „Matt in einem Zug übersehen“ bzw. „Erzwungenes Matt zugelassen“ kritisch negativ; „Mattdrohung abgewehrt“ positiv.

**Lernprinzip:** Bevor du angreifst, prüfe: Welche Schachgebote hat mein Gegner, und droht er Matt?

**Testfälle:** `Tact011MateDefense: allowsMateInOne, parriesBackRankMateThreat, noFindingWithoutMateDanger`

## TACT-012 – Geöffnete Linien zum König

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Erkennt Züge, die gegnerischen Langschrittlern neuen Zugriff auf die eigene Königszone geben.

**Eingabemerkmale:**

- Königszone (König und Nachbarfelder)
- Angriffe gegnerischer Läufer, Türme und Damen vor und nach dem Zug
- Schachgebote in der Engine-Variante

**Erkennungsverfahren:** Felder der eigenen Königszone, die gegnerische Läufer/Türme/Damen nach dem Zug neu erreichen, weil die Linie über das Startfeld der gezogenen Figur (bzw. das geräumte Feld) frei wurde. Relevant ab zwei Feldern oder einem Feld, das eine Schwerfigur erreicht.

**Begründungslogik:** Negativ; belegt, wenn der Gegner in den nächsten Halbzügen der Variante Schach gibt, sonst Heuristik. Nennt Angreifer und Felder.

**Lernprinzip:** Ziehe Bauern vor dem eigenen König nur mit gutem Grund: Jede geöffnete Linie kann zum Angriffsweg werden.

**Testfälle:** `Tact012KingSafety: pawnCaptureOpensFileTowardsKing, quietMoveAwayFromKingIsHarmless`

## TACT-013 – Verteidiger

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Erkennt entfernte und überlastete Verteidiger.

**Eingabemerkmale:**

- Deckungsbeziehungen vor und nach dem Zug
- statische Abtauschbewertung
- geschlagene Figur
- Antwort der Engine-Variante

**Erkennungsverfahren:** (a) Die gezogene Figur deckte eine eigene Figur, die danach mit Gewinn geschlagen werden kann. (b) Mit einem Schlagzug wird ein gegnerischer Verteidiger beseitigt. (c) Ein gegnerischer Verteidiger ist allein für zwei angegriffene Figuren zuständig (Überlastung).

**Begründungslogik:** (a) negativ, belegt bei Schlag in der Variante; (b) und (c) positiv als Heuristik.

**Lernprinzip:** Bevor eine Figur zieht, prüfe, welche Aufgaben sie gerade erfüllt – vielleicht deckt sie etwas Wichtiges.

**Testfälle:** `Tact013Defense: defenderLeaves, moveCreatesOverloadedDefender, unrelatedMoveKeepsDefence`

## TACT-014 – Freibauer läuft durch

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Erkennt Freibauern, die laut Variante oder Quadratregel umwandeln können.

**Eingabemerkmale:**

- Umwandlungen in der Engine-Variante
- Freibauern beider Seiten
- Quadratregel im reinen Bauernendspiel
- Seite am Zug

**Erkennungsverfahren:** Umwandlung einer Seite innerhalb von 12 Halbzügen der Engine-Variante; im reinen Bauernendspiel zusätzlich Quadratregel (freier Weg, Seite am Zug berücksichtigt).

**Begründungslogik:** Eigene Umwandlung positiv, gegnerische negativ (belegt durch Variante); Quadratregel als Heuristik formuliert („sofern der Weg frei bleibt“).

**Lernprinzip:** Ein Freibauer muss laufen: Je weiter er vorrückt, desto mehr gegnerische Kräfte bindet er.

**Testfälle:** `Tact014PassedPawn: promotionInLine, ruleOfTheSquare, kingInsideSquareCatchesPawn`

## TACT-015 – Gabel

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Erkennt Springer- und Bauerngabeln durch den eigenen Zug oder die gegnerische Antwort.

**Eingabemerkmale:**

- Angriffe der gezogenen Figur
- Wert und Deckung der Ziele
- Folgezug der Engine-Variante

**Erkennungsverfahren:** Springer- oder Bauernzug, nach dem die Figur ≥ 2 relevante Ziele angreift (mindestens eines neu); dieselbe Prüfung für die gegnerische Antwort.

**Begründungslogik:** Belegt, wenn ein Ziel im Folgezug der Variante geschlagen wird.

**Lernprinzip:** Achte auf Felder, von denen ein gegnerischer Springer zwei deiner Figuren gleichzeitig angreifen kann.

**Testfälle:** `Tact015Fork: knightForksKingAndRook, opponentForkAfterQuietMove, singleTargetIsNoFork`

## TACT-016 – Abwicklung

**Kategorie:** Taktik (Ebene 1) · **Version:** 1.0

**Zweck:** Fasst einen durch den Zug eingeleiteten Schlagabtausch zusammen und bewertet seine Folgen.

**Eingabemerkmale:**

- ununterbrochene Schlagfolge ab dem Zug
- Materialbilanz vor und nach der Abwicklung
- Spielphase vor und nach der Abwicklung

**Erkennungsverfahren:** Ununterbrochene Schlagfolge ab dem eigenen Schlagzug (≥ 2 Halbzüge) in der Variante; Materialbilanz vorher/nachher und Übergang ins Endspiel.

**Begründungslogik:** Positiv/neutral/negativ je nach Nettomaterial; beschreibt getauschte Figuren und Endspielübergang.

**Lernprinzip:** Wickle ab, wenn du Material oder eine bessere Struktur mitnimmst – besonders mit Materialvorteil ins Endspiel.

**Testfälle:** `Tact016Simplification: losingExchange, equalTradeIsNeutral, notApplicableForQuietMove`

## STRAT-001 – Entwicklung der Figuren

**Kategorie:** Strategie (Ebene 2) · **Version:** 1.0

**Zweck:** Bewertet, ob ein Eröffnungszug die Entwicklung fördert oder Tempo verliert.

**Eingabemerkmale:**

- Leichtfiguren auf Startfeldern (beide Seiten)
- gezogene Figur und ihr Startfeld
- Zugnummer und Material (Eröffnungsphase)
- Mobilität unentwickelter Läufer

**Erkennungsverfahren:** Nur in der Eröffnung (Zug ≤ 15, fast volles Material). Leichtfiguren auf Ausgangsfeldern, Ausgangsfelder aller Figuren, Läufermobilität nach Bauernzügen, Entwicklungsvergleich.

**Begründungslogik:** Entwicklung positiv; erneutes Ziehen einer bereits entwickelten Figur bei ≥ 2 unentwickelten Leichtfiguren (ohne Schlag/Schach/Bedrohung) negativ; frühe Damenzüge negativ; Bauernzüge, die Läufer befreien, positiv.

**Lernprinzip:** Entwickle in der Eröffnung zuerst Springer und Läufer, rochiere früh und ziehe keine Figur zweimal ohne Grund.

**Testfälle:** `Strat001Development: developingKnight, movingDevelopedPieceAgain, earlyQueenMoveIsNotCountedAsSecondMove, notApplicableAfterOpening`

## STRAT-002 – Zentrumskontrolle

**Kategorie:** Strategie (Ebene 2) · **Version:** 1.0

**Zweck:** Bewertet die Veränderung von Kontrolle und Besetzung der Felder d4, e4, d5 und e5 sowie zentrale Bauernhebel.

**Eingabemerkmale:**

- Angriffe beider Seiten auf d4/e4/d5/e5 vor und nach dem Zug
- Besetzung der Zentrumsfelder
- Angriffe des gezogenen Bauern auf gegnerische Zentralbauern

**Erkennungsverfahren:** Kontrolle (Angriffe) und Besetzung von d4/e4/d5/e5 vor und nach dem Zug für beide Seiten; Bauernhebel gegen gegnerische Zentralbauern.

**Begründungslogik:** Nennt konkrete Felder und die Kontrollzahlen; positiv ab +2 (oder Besetzung), negativ ab −2.

**Lernprinzip:** Kämpfe um das Zentrum: Bauern auf d4/e4 (bzw. d5/e5) und Figuren, die diese Felder kontrollieren, geben deinen Figuren Raum.

**Testfälle:** `Strat002Center: pawnOccupiesCenter, retreatLosesCenterControl, pawnLever`

## STRAT-003 – Königssicherheit

**Kategorie:** Strategie (Ebene 2) · **Version:** 1.0

**Zweck:** Bewertet Rochade, Verlust des Rochaderechts, Bauernschutz und offene Linien vor dem eigenen König.

**Eingabemerkmale:**

- Rochaderechte vor und nach dem Zug
- Bauernschutz vor dem König
- offene/halboffene Linien neben dem König
- gegnerische Schwerfiguren und Läufer auf diesen Linien
- Damen auf dem Brett

**Erkennungsverfahren:** Rochade, Verlust des Rochaderechts ohne Rochade (nur mit gegnerischer Dame), Bauernschild vor dem rochierten König, offene/halboffene Linien neben dem König mit gegnerischen Schwerfiguren, Diagonalen von Läufer/Dame zur Königszone.

**Begründungslogik:** Ein nicht rochierter König wird nicht pauschal kritisiert; Kritik nennt konkrete Linien und Angreifer.

**Lernprinzip:** Bringe deinen König rechtzeitig in Sicherheit und schwäche seine Bauerndeckung nur, wenn es einen konkreten Grund gibt.

**Testfälle:** `Strat003KingSafety: castling, losingCastlingRights, weakeningPawnShield, uncastledKingIsNotPenalisedPerSe`

## STRAT-004 – Figurenaktivität

**Kategorie:** Strategie (Ebene 2) · **Version:** 1.0

**Zweck:** Bewertet die Mobilitätsänderung der gezogenen Figur, Randspringer und durch eigene Bauern blockierte Läufer.

**Eingabemerkmale:**

- Pseudomobilität der gezogenen Figur vor und nach dem Zug
- Randlage von Springern
- Mobilität eigener Läufer vor und nach einem Bauernzug
- Gesamtmobilität beider Seiten

**Erkennungsverfahren:** Pseudomobilität der gezogenen Figur vor/nach dem Zug (Schwelle ±3 Felder), Springer am Rand, Läufer, die durch eigene Bauernzüge blockiert werden, Gesamtmobilität.

**Begründungslogik:** Nennt Feldanzahl vorher/nachher und das konkrete Feld.

**Lernprinzip:** Stelle deine Figuren dorthin, wo sie viele wichtige Felder kontrollieren; eine passive Figur ist eine halbe Figur.

**Testfälle:** `Strat004Activity: knightOnTheRim, rookActivated, pawnBlocksOwnBishop`

## STRAT-005 – Bauernstruktur

**Kategorie:** Strategie (Ebene 2) · **Version:** 1.0

**Zweck:** Erkennt neu entstandene Bauernschwächen und Freibauern beider Seiten.

**Eingabemerkmale:**

- isolierte, doppelte, rückständige und hängende Bauern vor und nach dem Zug
- Freibauern
- halboffene Linien als Angriffsrichtung

**Erkennungsverfahren:** Isolierte, doppelte, rückständige und hängende Bauern sowie Freibauern beider Seiten vor und nach dem Zug; nur neu entstandene Merkmale.

**Begründungslogik:** Schwächen eigener Bauern negativ, beim Gegner positiv; Freibauern umgekehrt. Angriffsrichtung über halboffene Linien wird genannt.

**Lernprinzip:** Bauernzüge sind endgültig: Prüfe vor jedem Bauernzug und jedem Schlagen mit einem Bauern, welche dauerhaften Schwächen entstehen.

**Testfälle:** `Strat005PawnStructure: capturingTowardsDoubledIsolatedPawns, creatingPassedPawn, quietPieceMoveChangesNothing`

## STRAT-006 – Vorposten und schwache Felder

**Kategorie:** Strategie (Ebene 2) · **Version:** 1.0

**Zweck:** Erkennt Springer- und Läuferzüge auf Vorposten sowie durch Bauernzüge neu entstandene schwache Felder im eigenen Lager.

**Eingabemerkmale:**

- Felder, die gegnerische Bauern nie mehr angreifen können
- Deckung durch eigene Bauern
- schwache Felder im eigenen Lager vor und nach einem Bauernzug
- gegnerische Leichtfiguren, die diese Felder nutzen können

**Erkennungsverfahren:** Vorposten: Feld auf der 4.–6. Reihe, von eigenem Bauern gedeckt, von keinem gegnerischen Bauern mehr angreifbar. Schwache Felder: Felder der 3./4. eigenen Reihe (Zentrum oder vor dem König), die nach einem Bauernzug kein eigener Bauer mehr kontrollieren kann.

**Begründungslogik:** Springer/Läufer auf Vorposten positiv; neue Löcher negativ mit Nennung gegnerischer Figuren, die sie nutzen könnten.

**Lernprinzip:** Suche stabile Vorposten für deine Springer: Felder in der gegnerischen Hälfte, die kein gegnerischer Bauer mehr angreifen kann.

**Testfälle:** `Strat006Outpost: knightOnOutpost, attackablePostIsNoOutpost, pawnMoveCreatesHoles`

## STRAT-007 – Offene Linien und Diagonalen

**Kategorie:** Strategie (Ebene 2) · **Version:** 1.0

**Zweck:** Bewertet Turm-, Damen- und Läuferzüge in Bezug auf offene Linien, Diagonalen und die siebte Reihe.

**Eingabemerkmale:**

- offene und halboffene Linien
- Ziel- und Startlinie der Schwerfigur
- Eindringen auf die siebte Reihe
- Länge der kontrollierten Diagonale

**Erkennungsverfahren:** Turm-/Damenzüge auf offene oder halboffene Linien, Verlassen offener Linien, Eindringen auf die 7. Reihe mit Zielen, Läufer auf langen offenen Diagonalen.

**Begründungslogik:** Erklärt den Nutzen in Bezug auf die konkrete Linie bzw. Reihe.

**Lernprinzip:** Bringe deine Türme auf offene Linien und nutze sie, um auf die siebte Reihe einzudringen.

**Testfälle:** `Strat007OpenLines: rookToOpenFile, rookToSeventhRank, knightMovesAreIgnored`

## STRAT-008 – Material und Kompensation

**Kategorie:** Strategie (Ebene 2) · **Version:** 1.0

**Zweck:** Erklärt Materialungleichgewichte und benennt konkrete Kompensationsfaktoren.

**Eingabemerkmale:**

- Materialbilanz am ruhigen Punkt der Variante
- Engine-Bewertung nach dem Zug
- Entwicklungsvorsprung
- Königssicherheit beider Seiten
- Freibauern
- Schachgebote/Drohungen in der Variante

**Erkennungsverfahren:** Materialbilanz am ruhigen Punkt der Variante gegenüber vorher, Engine-Bewertung nach dem Zug, Kompensationsfaktoren (Entwicklungsvorsprung, König im Zentrum, Freibauern, Schachgebote in der Variante, Mobilität).

**Begründungslogik:** Materialnachteil mit Bewertung ≥ −0,50 wird ausdrücklich nicht als Fehler, sondern als Kompensation erklärt (belegt durch die Engine-Bewertung).

**Lernprinzip:** Zähle nicht nur Material: Entwicklungsvorsprung, ein unsicherer gegnerischer König oder ein starker Freibauer können mehr wert sein als ein Bauer.

**Testfälle:** `Strat008Compensation: gambitWithCompensation, materialDeficitWithBadScoreIsNotCompensation, noEngineScoreNoClaim`

## STRAT-009 – Figurenabtausch

**Kategorie:** Strategie (Ebene 2) · **Version:** 1.0

**Zweck:** Bewertet Figurentausche nach Materiallage, Läuferpaar, gutem/schlechtem Läufer und Aktivität der getauschten Figuren.

**Eingabemerkmale:**

- getauschte Figuren (Zug und direkte Antwort)
- Materialbilanz
- Läuferpaar vor und nach dem Tausch
- Anteil eigener Bauern auf der Feldfarbe des verbleibenden Läufers
- Mobilität der getauschten Figuren

**Erkennungsverfahren:** Figurentausch (Zug schlägt Figur, Gegner schlägt zurück): Läuferpaar, Materiallage, guter/schlechter Läufer (Anteil eigener Bauern auf Läuferfarbe ≥ 60 %), Mobilität der getauschten Figuren.

**Begründungslogik:** Erklärt, welche Seite vom Tausch profitiert und warum.

**Lernprinzip:** Tausche Figuren, wenn du materiell vorne liegst; behalte sie, wenn du angreifen willst oder zurückliegst.

**Testfälle:** `Strat009Exchange: givingUpBishopPair, simplifyingWhenAhead, noExchangeWithoutRecapture`

## STRAT-010 – Initiative und Tempo

**Kategorie:** Strategie (Ebene 2) · **Version:** 1.0

**Zweck:** Erkennt konkrete Drohungen, die den Gegner zur Reaktion zwingen, sowie Tempoverluste durch Hin- und Herziehen.

**Eingabemerkmale:**

- Schachgebote
- neu angegriffene relevante Ziele
- Mattdrohungen
- vorheriger eigener Zug derselben Figur

**Erkennungsverfahren:** Konkrete Drohungen: Schach, neu angegriffene relevante Ziele, Mattdrohung; Tempoverlust durch Rückkehr der zuletzt gezogenen Figur auf ihr Ausgangsfeld.

**Begründungslogik:** Initiative nur bei konkreter, benannter Drohung (nicht nach Angriffsanzahl); belegt, wenn die Engine den Zug als gut bewertet.

**Lernprinzip:** Spiele mit Tempo: Züge, die gleichzeitig entwickeln und drohen, zwingen den Gegner, nach deiner Pfeife zu tanzen.

**Testfälle:** `Strat010Initiative: tempoLossByMovingBack, concreteThreat, quietMoveHasNoInitiative`

## STRAT-011 – Langfristige Pläne

**Kategorie:** Strategie (Ebene 2) · **Version:** 1.0

**Zweck:** Leitet stellungsbezogene Pläne für beide Seiten ab (schwache Bauern, offene Linien, passive Figuren, Freibauern, Bauernmehrheiten, Königsaktivität, Abtausch).

**Eingabemerkmale:**

- Bauernschwächen beider Seiten
- offene Linien ohne Turm
- Mobilität der Figuren
- Freibauern und Bauernmehrheiten
- Spielphase und Königsstellung
- Materialbilanz
- nächster eigener Zug der Engine-Hauptvariante

**Erkennungsverfahren:** Für beide Seiten: schwache gegnerische Bauern auf halboffenen Linien, unbesetzte offene Linien, passivste Figur (außer in der Eröffnung), Freibauern, Bauernmehrheiten, Königsaktivierung im Endspiel, Abtausch bei Materialvorteil, nächster eigener Zug der Engine-Hauptvariante.

**Begründungslogik:** Jeder Plan nennt sein Merkmal („basis“); ohne Merkmal kein Plan. Engine-Fortsetzungen werden als Hauptvariante, nicht als Zwang gekennzeichnet.

**Lernprinzip:** Spiele nach einem Plan, der sich aus der Stellung ergibt: Schwächen angreifen, Linien besetzen, die schlechteste Figur verbessern.

**Testfälle:** `Strat011Plans: attackIsolatedPawn, kingActivationInEndgame, engineContinuationIsLabelledAsSearchLine`

## STRAT-012 – Endspielprinzipien

**Kategorie:** Strategie (Ebene 2) · **Version:** 1.0

**Zweck:** Bewertet Königsaktivität, Unterstützung von Freibauern, Turm hinter dem Freibauern, Opposition und Umwandlungsrennen.

**Eingabemerkmale:**

- Spielphase
- Zentrumsabstand des Königs vor und nach dem Zug
- Freibauern beider Seiten
- Turmstellung relativ zu Freibauern
- Königsstellung im reinen Bauernendspiel
- Umwandlungsdistanzen

**Erkennungsverfahren:** Nur im Endspiel: Zentrumsabstand des Königs, Unterstützung eigener Freibauern, Turm hinter Freibauern (eigenen oder gegnerischen), direkte Opposition im reinen Bauernendspiel, Umwandlungsrennen.

**Begründungslogik:** Erklärt unterstützende Merkmale; für das exakte Ergebnis wird ausdrücklich auf die Engine verwiesen.

**Lernprinzip:** Im Endspiel gilt: König aktivieren, Freibauern schaffen und unterstützen, Türme hinter die Freibauern stellen.

**Testfälle:** `Strat012Endgame: kingTowardsCenter, opposition, rookBehindPassedPawn, notApplicableInMiddlegame`

## Ebene 3 – Klassifizierung und Erklärung

**MistakeClassifier** (Schwellen konfigurierbar unter `chesscoach.coaching.classification`): Bestzug ≤ 10 cp, Sehr gut ≤ 25, Gut ≤ 60, Ungenau ≤ 120, Fehler ≤ 250, darüber grober Fehler. Zusätzlich:
- Mattbewertungen werden gesondert behandelt (Matt zugelassen → grober Fehler; Matt verpasst → je nach verbleibendem Vorteil; langsameres Matt → Sehr gut/Gut). Der Verlust wird dann nicht als Centipawn-Wert angegeben.
- Wechsel von Vorteil (≥ +1,50) zu Nachteil (≤ −1,50) ist mindestens ein Fehler.
- In entschiedenen Stellungen werden Verluste über die Gewinnerwartung gedämpft (< 2 Prozentpunkte → höchstens „Gut“, < 6 → höchstens „Ungenau“).
- „Erzwungen“: einziger legaler Zug oder bester Zug in nicht gewonnener Stellung, bei dem alle anderen geprüften Kandidaten ≥ 200 cp schlechter sind.
- Geringe Analysezuverlässigkeit → Einordnung „vorläufig“; fehlende Bewertung → „Unsicher“.

**ExplanationBuilder:** wählt den Erklärungstyp (`TACTICAL_BLUNDER`, `MATERIAL_LOSS`, `MISSED_WIN`, `MISSED_DEFENSE`, `STRATEGIC_INACCURACY`, `GOOD_MOVE`, `BEST_MOVE`, `FORCED_MOVE`, `DRAWING_RESOURCE`, `ENDGAME_TECHNIQUE`, `UNCERTAIN_ANALYSIS`) und beantwortet die sieben Leitfragen. Jede Aussage trägt ihre Quellen (Regel-ID oder `ENGINE`). Varianten werden stets als „Hauptvariante, keine erzwungene Zugfolge“ bezeichnet.

Tests: `MistakeClassifierTest`, `ExplanationQualityTest` (Katalog kuratierter Stellungen mit erwarteten Regel-IDs, Erklärungstypen, zulässigen und verbotenen Aussagen sowie Invarianten: keine nicht existierenden Figuren, nur legale Beweisketten, keine übergangene taktische Widerlegung, keine als zwingend formulierte Heuristik).
