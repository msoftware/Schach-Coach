# Betrieb und Deployment

## Systemanforderungen

| Komponente | Anforderung |
|---|---|
| Java | JDK 21 (getestet: Oracle JDK 21.0.10; in Docker Eclipse Temurin 21.0.12). Ein JDK wird benötigt, weil Calvin das Modul `jdk.incubator.vector` nutzt. |
| Node.js | 22 oder neuer (nur zum Bauen des Frontends; getestet mit 24.14) |
| Speicher | ca. 1,5–2 GB RAM (Backend + zwei Engine-Prozesse mit je max. 512 MB Heap) |
| CPU | 2 Kerne empfohlen (je ein Thread für Spiel- und Analyse-Engine) |
| Betriebssysteme | Windows 10/11, Linux (x86-64), macOS – überall, wo ein JDK 21 läuft; getestet: Windows 11 |
| Browser | aktuelle Chromium-basierte Browser und Firefox (getestet), Safari (nicht getestet) |

Es ist keine separate Installation einer Schach-Engine nötig; Calvin liegt unter `backend/engine/`.

## Variante A: lokal (Entwicklung)

```bash
cd backend && ./gradlew bootRun
cd frontend && npm ci && npm run dev     # http://localhost:5173
```

## Variante B: ein ausführbares JAR

```bash
cd frontend && npm ci && npx vite build
cd ../backend && ./gradlew bootJar -PwithFrontend
java -jar build/libs/chess-coach-backend.jar --chesscoach.engine.calvin.jar-path=engine/calvin-chess-engine-6.1.1.jar
# http://localhost:8080
```

Das JAR sucht die Engine relativ zum Arbeitsverzeichnis (`engine/…`) und ersatzweise unter `backend/engine/…`.

## Variante C: Docker Compose

```bash
docker compose up --build      # http://localhost:8081
```

- `backend`: Gradle-Build im Container, Laufzeit Eclipse Temurin 21 (JDK), Engine unter `/app/engine`, Profil `prod`
  (Swagger-UI aus), läuft als unprivilegierter Benutzer.
- `frontend`: Vite-Build, ausgeliefert von nginx; `/api` wird an `backend:8080` weitergeleitet (gleicher Ursprung).
- Hinweis: Die Docker-Dateien wurden erstellt, ein Image-Build konnte in der Entwicklungsumgebung mangels laufendem
  Docker-Daemon nicht ausgeführt werden. Die verwendeten Basis-Image-Tags wurden auf Existenz geprüft.

## Hinter einem HTTPS-Reverse-Proxy (z. B. Apache)

Der Browser schickt bei `POST`-Anfragen den Header `Origin: https://<domain>`. Sieht das Backend intern einen anderen
Host oder `http`, behandelt Spring die Anfrage als fremden Ursprung und lehnt sie ab (`403 Invalid CORS request`), wenn
die Domain nicht in `chesscoach.api.allowed-origins` steht. Deshalb:

1. **Ursprung freigeben:** `CHESSCOACH_API_ALLOWED_ORIGINS=https://<domain>` (Umgebungsvariable des Backends bzw. in
   `docker-compose.yml` statt `http://localhost:8081`).
2. **Host und Schema weitergeben** und die echte Client-Adresse für das Rate-Limit auswerten
   (`SERVER_FORWARD_HEADERS_STRATEGY=native`):

```apache
<VirtualHost *:443>
    ServerName <domain>
    ProxyPreserveHost On
    RequestHeader set X-Forwarded-Proto "https"
    RequestHeader set X-Forwarded-Port "443"
    ProxyPass        / http://127.0.0.1:<port>/ retry=1 timeout=120
    ProxyPassReverse / http://127.0.0.1:<port>/
    # SSL-Zertifikat wie gehabt (z. B. Let's Encrypt)
</VirtualHost>
```

Benötigte Module: `a2enmod proxy proxy_http headers`. WebSocket-Weiterleitungen braucht die Anwendung nicht.

3. **Weiterleitung http → https mit 308**, nicht mit 301/302: Browser machen bei 301/302 aus einem `POST` ein `GET`
   (Antwort dann `405 METHOD_NOT_ALLOWED`). Beispiel im `:80`-VirtualHost:
   `RewriteRule ^ https://%{SERVER_NAME}%{REQUEST_URI} [END,NE,R=308]`.
4. **Produktiv nicht den Vite-Entwicklungsserver** hinter den Proxy stellen, sondern Variante B (ein JAR mit
   `-PwithFrontend`) oder C (Docker Compose).

## Konfiguration (`backend/src/main/resources/application.yml`)

Alle Werte lassen sich per Umgebungsvariable überschreiben (z. B. `CHESSCOACH_ANALYSIS_CANDIDATE_NODES=120000`).

| Schlüssel | Bedeutung | Standard |
|---|---|---|
| `chesscoach.engine.type` | `CALVIN` oder `UCI` | `CALVIN` |
| `chesscoach.engine.calvin.jar-path` | Pfad zum Engine-JAR | `engine/calvin-chess-engine-6.1.1.jar` |
| `chesscoach.engine.calvin.max-heap-mb` | Heap je Engine-Prozess | 512 |
| `chesscoach.engine.hash-mb` / `threads` | UCI-Optionen | 32 / 1 |
| `chesscoach.engine.start-timeout` / `search-grace` / `max-search-time` | Zeitgrenzen | 30 s / 3 s / 20 s |
| `chesscoach.analysis.*` | Suchgrenzen der Analyse | siehe ENGINE_INTEGRATION.md |
| `chesscoach.difficulty.levels.*` | Engine-Parameter je Spielstärke | siehe ENGINE_INTEGRATION.md |
| `chesscoach.coaching.classification.*` | Schwellen der Qualitätsklassen | 10/25/60/120/250 cp |
| `chesscoach.coaching.disabled-rules` | deaktivierte Regel-IDs | leer |
| `chesscoach.api.allowed-origins` | CORS-Ursprünge | `http://localhost:5173`, `http://127.0.0.1:5173`, `http://localhost:8080` |
| `chesscoach.api.rate-limit-per-minute` | rechenintensive Anfragen je Partie | 120 |
| `chesscoach.api.rate-limit-per-minute-per-client` | rechenintensive Anfragen je IP-Adresse (mehrere Personen können sich eine Adresse teilen) | 600 |
| `chesscoach.api.max-games` / `game-idle-timeout` | Partien im Arbeitsspeicher; bei Erreichen der Höchstzahl wird die am längsten unbenutzte Partie verworfen (das Frontend stellt sie bei Bedarf wieder her) | 500 / 6 h |
| `chesscoach.api.empty-game-idle-timeout` | Partien ohne Zug werden früher verworfen | 30 min |
| `server.forward-headers-strategy` | `native` hinter einem Reverse-Proxy (in `docker-compose.yml` gesetzt), sonst leer | – |

## Betriebshinweise

- Hinter einem eigenen Reverse-Proxy `server.forward-headers-strategy=native` setzen und den Proxy
  `X-Forwarded-For` mit der echten Client-Adresse **überschreiben** lassen (nicht anhängen), sonst teilen sich alle
  Nutzer ein Rate-Limit bzw. Clients können ihre Adresse fälschen. Den Backend-Port nicht direkt veröffentlichen.
- Mehrere Personen und mehrere Browserfenster können gleichzeitig spielen; jedes Fenster hat eine eigene Partie
  (eigener Slot im Browser-Speicher). Alle Partien teilen sich die beiden Engine-Prozesse: Computerzüge und Analysen
  werden nacheinander berechnet, bei vielen gleichzeitigen Spielern verlängern sich daher die Wartezeiten.
- Partien liegen nur im Arbeitsspeicher; nach einem Neustart des Backends stellt das Frontend die Partie aus dem
  Browser-Speicher wieder her.
- Gesundheitsprüfung: `GET /api/health` (`UP`, `DEGRADED` bei nicht verfügbarer Engine).

## Logging

Neben der Konsole schreibt das Backend drei rollierende Dateien (Konfiguration: `backend/src/main/resources/logback-spring.xml`):

| Datei | Inhalt |
|---|---|
| `chess-coach.log` | alle Meldungen ab INFO: Startzusammenfassung, Partien gestartet/beendet, Analysen (Dauer, Tiefe, Knoten, Klasse), Engine-Starts und -Neustarts, verworfene Partien |
| `chess-coach-error.log` | nur WARN und ERROR – Rate-Limit-Treffer, volle Analyse-Warteschlange, Engine-Probleme, ungültige Anfragen (405/415), unerwartete Fehler mit Stacktrace |
| `chess-coach-access.log` | eine Zeile je API-Anfrage: Methode, Pfad, Status, Dauer, gekürzte Client-Adresse, Request-ID |

Rotation: täglich und zusätzlich ab `max-file-size`; ältere Dateien werden als `.gz` abgelegt und nach `max-history`
Tagen bzw. bei Überschreiten von `total-size-cap` gelöscht.

| Einstellung | Bedeutung | Standard |
|---|---|---|
| `logging.file.path` (`LOGGING_FILE_PATH`) | Verzeichnis der Logdateien | `logs` (Docker: `/app/logs`, Volume `backend-logs`) |
| `chesscoach.logging.max-file-size` | Größe, ab der innerhalb eines Tages rotiert wird | 10MB |
| `chesscoach.logging.max-history` / `access-max-history` | Aufbewahrung in Tagen (Anwendung/Fehler bzw. Zugriffe) | 30 / 14 |
| `chesscoach.logging.total-size-cap` | Obergrenze je Logart | 1GB |
| `logging.level.de.chesscoach` | Detailgrad der Anwendung (`DEBUG` zeigt u. a. Nachprüfungen und eingereihte Analysen) | INFO |
| `logging.level.de.chesscoach.access` | `DEBUG` protokolliert zusätzlich jede erfolgreiche lesende Abfrage | INFO |

**Fehler zuordnen:** Jede API-Anfrage bekommt eine Request-ID. Sie steht im Antwort-Header `X-Request-Id`, in jeder
Fehlerantwort (`"requestId"`) und in jeder Logzeile dieser Anfrage, z. B. `[3f9a01c2 7b2e4d10]` (Request-ID,
Partie-ID). Meldet der Browser einen Fehler, findet `grep 3f9a01c2 logs/*.log` alle zugehörigen Einträge – auch die
der Analyse, die die Anfrage ausgelöst hat. Bei Docker: `docker compose exec backend sh -c 'grep 3f9a01c2 /app/logs/*.log'`
oder `docker compose logs backend`.

**Datenschutz:** Die Logs enthalten keine Partien, Züge oder Stellungen. Client-Adressen werden gekürzt gespeichert
(IPv4 auf das /24-Netz, z. B. `203.0.113.0`, IPv6 auf die ersten 48 Bit). Partien sind nur über eine zufällige,
gekürzte Kennung erkennbar.
