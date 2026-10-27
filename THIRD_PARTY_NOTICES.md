# Drittanbieter-Hinweise (THIRD_PARTY_NOTICES)

Dieses Dokument listet alle tatsächlich ausgelieferten Bibliotheken und Assets mit Version, Quelle und Lizenz.
Lizenztexte, die mitgeliefert werden müssen, liegen im Verzeichnis [`licenses/`](licenses/).
Die Bibliothekslisten wurden aus den Lizenzberichten des Builds erzeugt
(`./gradlew checkLicense`, `npm run license-check`); beide Prüfungen laufen in der CI.

## 1. Schach-Engine: Calvin 6.1.1

| Feld | Wert |
|---|---|
| Datei | `backend/engine/calvin-chess-engine-6.1.1.jar` |
| Quelle | https://github.com/kelseyde/calvin-chess-engine/releases/tag/6.1.1 (veröffentlicht 2025-06-04) |
| SHA-256 | `dab9383b478e41718251da2cf738f4f95df2130eb2a42baffc07ede27bb405fe` (wird bei jedem Build geprüft) |
| Autor | Dan Kelsey |
| Lizenz | MIT – [`licenses/calvin-chess-engine-MIT.txt`](licenses/calvin-chess-engine-MIT.txt), zusätzlich `backend/engine/LICENSE-calvin.txt` |

Die Engine wird unverändert als eigenständiger Prozess ausgeführt.

## 2. Schachregeln: Chesslib 1.3.7

| Feld | Wert |
|---|---|
| Artefakt | `com.github.bhlangonijr:chesslib:1.3.7` (JitPack, gebaut aus dem Git-Tag `1.3.7`) |
| Quelle | https://github.com/bhlangonijr/chesslib |
| Lizenz | Apache License 2.0 – [`licenses/chesslib-Apache-2.0.txt`](licenses/chesslib-Apache-2.0.txt). Das veröffentlichte POM enthält keine Lizenzangabe; die Lizenz ergibt sich aus dem Repository und ist in `backend/config/allowed-licenses.json` explizit freigegeben. |

## 3. Schachfiguren (SVG)

Klassischer Figurensatz von **Colin M.L. Burnett („Cburnett“)**, Wikimedia Commons, Kategorie
„SVG chess pieces/Standard transparent“. Die Dateien sind laut Dateibeschreibung mehrfach lizenziert
(`{{self|GFDL|migration=relicense|BSD|GPL}}`); dieses Projekt verwendet sie unter der **BSD-Lizenz
(3-Klausel-Form der Commons-Vorlage)**. Lizenztext: [`licenses/cburnett-chess-pieces-BSD-3-Clause.txt`](licenses/cburnett-chess-pieces-BSD-3-Clause.txt).

Änderungen: XML-Deklaration und DOCTYPE entfernt, feste Breite/Höhe durch `viewBox="0 0 45 45"` ersetzt.
Die Dateien enthalten keine Skripte und keine externen Referenzen. Herkunftsdaten:
`frontend/src/assets/chess-pieces/sources.json`.

| Datei | Commons-Datei | SHA-1 der Originaldatei (laut Commons-API) |
|---|---|---|
| `bB.svg` | [File:Chess bdt45.svg](https://commons.wikimedia.org/wiki/File:Chess_bdt45.svg) | `da6dd1b5ef629bacebbd2ce26c7d81ba8a205587` |
| `bK.svg` | [File:Chess kdt45.svg](https://commons.wikimedia.org/wiki/File:Chess_kdt45.svg) | `b1165ef85a3df6f1af2549ab0af78ab21b540e8a` |
| `bN.svg` | [File:Chess ndt45.svg](https://commons.wikimedia.org/wiki/File:Chess_ndt45.svg) | `3c79cbbda76bcf1d4e4062cef6dd3b58a250a562` |
| `bP.svg` | [File:Chess pdt45.svg](https://commons.wikimedia.org/wiki/File:Chess_pdt45.svg) | `0a6d2f3dc6327a02ca591bc489d701fbff228138` |
| `bQ.svg` | [File:Chess qdt45.svg](https://commons.wikimedia.org/wiki/File:Chess_qdt45.svg) | `ed72e75b7bdbf880a3c9bee053c8786cfab8bdb9` |
| `bR.svg` | [File:Chess rdt45.svg](https://commons.wikimedia.org/wiki/File:Chess_rdt45.svg) | `bd0e866f1e6da8e3d6f9c0b356b60fc58391aff6` |
| `wB.svg` | [File:Chess blt45.svg](https://commons.wikimedia.org/wiki/File:Chess_blt45.svg) | `35dd477dc22636bfcc559a01f75b00369205ffda` |
| `wK.svg` | [File:Chess klt45.svg](https://commons.wikimedia.org/wiki/File:Chess_klt45.svg) | `2c7569b837971207e40f7148e2b2086aaf8e4bbd` |
| `wN.svg` | [File:Chess nlt45.svg](https://commons.wikimedia.org/wiki/File:Chess_nlt45.svg) | `3a2253429c0e39863b3f5ecf447209dccecdc337` |
| `wP.svg` | [File:Chess plt45.svg](https://commons.wikimedia.org/wiki/File:Chess_plt45.svg) | `09d59e2770fcee23722ac53b26e875f76d2c1eb1` |
| `wQ.svg` | [File:Chess qlt45.svg](https://commons.wikimedia.org/wiki/File:Chess_qlt45.svg) | `e638eb28ec25007b9f8fac8476bdf6ae1fc5a0ee` |
| `wR.svg` | [File:Chess rlt45.svg](https://commons.wikimedia.org/wiki/File:Chess_rlt45.svg) | `126b7779885b87e87acc713474587732711bc8d3` |

## 4. Backend-Bibliotheken (Laufzeit, Maven Central)

Lizenzen mit „/“ sind Mehrfachlizenzen des jeweiligen Projekts. Die Lizenztexte sind in den JAR-Dateien der Bibliotheken (`META-INF`) enthalten, die unverändert im ausführbaren Backend-JAR mitgeliefert werden.

**Apache License, Version 2.0** (61): `com.fasterxml.jackson.core:jackson-annotations:2.21`, `com.fasterxml.jackson.core:jackson-core:2.21.7`, `com.fasterxml.jackson.core:jackson-databind:2.21.7`, `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.21.7`, `com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.7`, `com.fasterxml.jackson:jackson-bom:2.21.7`, `com.fasterxml:classmate:1.7.3`, `commons-logging:commons-logging:1.3.6`, `io.micrometer:micrometer-commons:1.17.2`, `io.micrometer:micrometer-core:1.17.2`, `io.micrometer:micrometer-jakarta9:1.17.2`, `io.micrometer:micrometer-observation:1.17.2`, `io.swagger.core.v3:swagger-annotations-jakarta:2.2.55`, `io.swagger.core.v3:swagger-core-jakarta:2.2.55`, `io.swagger.core.v3:swagger-models-jakarta:2.2.55`, `org.apache.commons:commons-lang3:3.20.0`, `org.apache.logging.log4j:log4j-api:2.25.5`, `org.apache.logging.log4j:log4j-to-slf4j:2.25.5`, `org.apache.tomcat.embed:tomcat-embed-el:11.0.27`, `org.apache.tomcat.embed:tomcat-embed-websocket:11.0.27`, `org.hibernate.validator:hibernate-validator:9.1.5.Final`, `org.jboss.logging:jboss-logging:3.6.3.Final`, `org.jspecify:jspecify:1.0.1`, `org.springdoc:springdoc-openapi-starter-common:3.2.0`, `org.springdoc:springdoc-openapi-starter-webmvc-api:3.2.0`, `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0`, `org.springframework.boot:spring-boot-actuator-autoconfigure:4.1.2`, `org.springframework.boot:spring-boot-actuator:4.1.2`, `org.springframework.boot:spring-boot-autoconfigure:4.1.2`, `org.springframework.boot:spring-boot-health:4.1.2`, `org.springframework.boot:spring-boot-http-converter:4.1.2`, `org.springframework.boot:spring-boot-jackson:4.1.2`, `org.springframework.boot:spring-boot-micrometer-metrics:4.1.2`, `org.springframework.boot:spring-boot-micrometer-observation:4.1.2`, `org.springframework.boot:spring-boot-servlet:4.1.2`, `org.springframework.boot:spring-boot-starter-actuator:4.1.2`, `org.springframework.boot:spring-boot-starter-jackson:4.1.2`, `org.springframework.boot:spring-boot-starter-logging:4.1.2`, `org.springframework.boot:spring-boot-starter-micrometer-metrics:4.1.2`, `org.springframework.boot:spring-boot-starter-tomcat-runtime:4.1.2`, `org.springframework.boot:spring-boot-starter-tomcat:4.1.2`, `org.springframework.boot:spring-boot-starter-validation:4.1.2`, `org.springframework.boot:spring-boot-starter-webmvc:4.1.2`, `org.springframework.boot:spring-boot-starter:4.1.2`, `org.springframework.boot:spring-boot-tomcat:4.1.2`, `org.springframework.boot:spring-boot-validation:4.1.2`, `org.springframework.boot:spring-boot-web-server:4.1.2`, `org.springframework.boot:spring-boot-webmvc:4.1.2`, `org.springframework.boot:spring-boot:4.1.2`, `org.springframework:spring-aop:7.0.10`, `org.springframework:spring-beans:7.0.10`, `org.springframework:spring-context:7.0.10`, `org.springframework:spring-core:7.0.10`, `org.springframework:spring-expression:7.0.10`, `org.springframework:spring-web:7.0.10`, `org.springframework:spring-webmvc:7.0.10`, `org.webjars:swagger-ui:5.34.0`, `org.yaml:snakeyaml:2.6`, `tools.jackson.core:jackson-core:3.1.7`, `tools.jackson.core:jackson-databind:3.1.7`, `tools.jackson:jackson-bom:3.1.7`

**Apache License, Version 2.0 (laut Repository; POM ohne Angabe)** (1): `com.github.bhlangonijr:chesslib:1.3.7`

**Apache License, Version 2.0 / Eclipse Public License - v 2.0** (1): `org.apache.tomcat.embed:tomcat-embed-core:11.0.27`

**Apache License, Version 2.0 / Eclipse Public License - v 2.0 / GNU GENERAL PUBLIC LICENSE, Version 2 / GNU GENERAL PUBLIC LICENSE, Version 2 + Classpath Exception** (1): `jakarta.validation:jakarta.validation-api:3.1.1`

**Apache License, Version 2.0 / Eclipse Public License - v 2.0 / GNU GENERAL PUBLIC LICENSE, Version 2 / GNU GENERAL PUBLIC LICENSE, Version 2 + Classpath Exception / PUBLIC DOMAIN / The 3-Clause BSD License** (1): `jakarta.activation:jakarta.activation-api:2.1.4`

**Creative Commons Legal Code / PUBLIC DOMAIN / The 2-Clause BSD License** (1): `org.hdrhistogram:HdrHistogram:2.2.2`

**Eclipse Public License - v 2.0 / GNU GENERAL PUBLIC LICENSE, Version 2 / GNU GENERAL PUBLIC LICENSE, Version 2 + Classpath Exception** (1): `jakarta.annotation:jakarta.annotation-api:3.0.0`

**Eclipse Public License - v 2.0 / GNU GENERAL PUBLIC LICENSE, Version 2 / GNU GENERAL PUBLIC LICENSE, Version 2 + Classpath Exception / The 3-Clause BSD License** (1): `jakarta.xml.bind:jakarta.xml.bind-api:4.0.5`

**Eclipse Public License - v 2.0 / GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1** (2): `ch.qos.logback:logback-classic:1.5.38`, `ch.qos.logback:logback-core:1.5.38`

**MIT License** (3): `org.slf4j:jul-to-slf4j:2.0.20`, `org.slf4j:slf4j-api:2.0.20`, `org.webjars:webjars-locator-lite:1.1.5`


## 5. Frontend-Bibliotheken (Produktionsabhängigkeiten, npm)

- `@babel/helper-string-parser@7.29.11` – MIT
- `@babel/helper-validator-identifier@7.29.7` – MIT
- `@babel/parser@7.29.11` – MIT
- `@babel/types@7.29.11` – MIT
- `@jridgewell/sourcemap-codec@1.6.0` – MIT
- `@quasar/extras@2.1.0` – MIT
- `@vue/compiler-core@3.5.44` – MIT
- `@vue/compiler-dom@3.5.44` – MIT
- `@vue/compiler-sfc@3.5.44` – MIT
- `@vue/compiler-ssr@3.5.44` – MIT
- `@vue/devtools-api@6.6.4` – MIT
- `@vue/devtools-api@7.7.10` – MIT
- `@vue/devtools-kit@7.7.10` – MIT
- `@vue/devtools-shared@7.7.10` – MIT
- `@vue/reactivity@3.5.44` – MIT
- `@vue/runtime-core@3.5.44` – MIT
- `@vue/runtime-dom@3.5.44` – MIT
- `@vue/server-renderer@3.5.44` – MIT
- `@vue/shared@3.5.44` – MIT
- `birpc@2.9.0` – MIT
- `copy-anything@4.1.5` – MIT
- `csstype@3.2.3` – MIT
- `entities@7.0.1` – BSD-2-Clause
- `estree-walker@2.0.2` – MIT
- `hookable@5.5.3` – MIT
- `idb@8.0.4` – ISC
- `magic-string@0.30.21` – MIT
- `mitt@3.0.1` – MIT
- `nanoid@3.3.20` – MIT
- `perfect-debounce@1.0.0` – MIT
- `picocolors@1.1.1` – ISC
- `pinia@3.0.4` – MIT
- `postcss@8.5.31` – MIT
- `quasar@2.35.1` – MIT
- `rfdc@1.4.1` – MIT
- `source-map-js@1.2.2` – BSD-3-Clause
- `speakingurl@14.0.1` – BSD-3-Clause
- `superjson@2.2.6` – MIT
- `typescript@5.9.3` – Apache-2.0
- `vue-router@4.6.4` – MIT
- `vue@3.5.44` – MIT

Die Material-Icons-Schrift ist Teil von `@quasar/extras` und wird lokal ausgeliefert (keine Laufzeitabhängigkeit von Google Fonts).

## 6. Werkzeuge (nicht ausgeliefert)

Gradle 9.8.0, JUnit 5, AssertJ, Mockito, JaCoCo 0.8.14, Vite 8.3.4, Vitest 4.1.12, Vue Test Utils 2.4.6,
Playwright 1.64.0, TypeScript 5.9.3, vue-tsc 3.3.13, jsdom 27.4.0, fake-indexeddb 6.2.6,
license-checker-rseidelsohn 5.0.1 – jeweils unter ihren eigenen Open-Source-Lizenzen.

## 7. Fachliche Grundlage

Die strategischen Coaching-Regeln wurden in eigener Formulierung aus den Prinzipien von Edward Lasker,
*Chess Strategy* (Project Gutenberg, eBook Nr. 5614), abgeleitet. Der Buchtext wird weder mitgeliefert
noch zur Laufzeit geladen.
