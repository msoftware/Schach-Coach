package de.chesscoach.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/**
 * Konfiguration der Engine-Integration (Präfix {@code chesscoach.engine}).
 *
 * @param type            CALVIN (mitgeliefert) oder UCI (beliebige UCI-Engine, z. B. Stockfish)
 * @param calvin          Calvin-spezifische Einstellungen
 * @param uci             Einstellungen für eine generische UCI-Engine
 * @param hashMb          Größe der Hashtabelle je Instanz
 * @param threads         Such-Threads je Instanz
 * @param startTimeout    maximale Wartezeit auf uciok/readyok
 * @param searchGrace     Zuschlag auf die Suchzeit, bevor "stop" gesendet wird
 * @param maxSearchTime   maximale Dauer einer knoten- oder tiefenbegrenzten Suche
 * @param maxLineLength   maximale Länge einer verarbeiteten Ausgabezeile
 * @param startOnBoot     Engines beim Start des Backends initialisieren
 */
@ConfigurationProperties("chesscoach.engine")
public record EngineProperties(@DefaultValue("CALVIN") EngineType type,
                               @DefaultValue Calvin calvin,
                               @DefaultValue Uci uci,
                               @DefaultValue("32") int hashMb,
                               @DefaultValue("1") int threads,
                               @DefaultValue("30s") Duration startTimeout,
                               @DefaultValue("3s") Duration searchGrace,
                               @DefaultValue("20s") Duration maxSearchTime,
                               @DefaultValue("8192") int maxLineLength,
                               @DefaultValue("true") boolean startOnBoot) {

    public enum EngineType {
        CALVIN, UCI
    }

    /**
     * @param jarPath        Pfad zum Calvin-JAR (fest konfiguriert, nie aus Benutzereingaben)
     * @param version        Version der ausgelieferten Datei
     * @param javaExecutable optionaler Pfad zur Java-Laufzeit (Standard: die des Backends)
     * @param maxHeapMb      Heap des Engine-Prozesses
     */
    public record Calvin(@DefaultValue("engine/calvin-chess-engine-6.1.1.jar") String jarPath,
                         @DefaultValue("6.1.1") String version,
                         @DefaultValue("") String javaExecutable,
                         @DefaultValue("512") int maxHeapMb) {
    }

    /**
     * @param command fest konfiguriertes Startkommando mit Argumenten
     * @param version dokumentierte Version
     */
    public record Uci(@DefaultValue List<String> command, @DefaultValue("unbekannt") String version) {
    }
}
