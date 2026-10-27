package de.chesscoach.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/**
 * Konfiguration der REST-API (Präfix {@code chesscoach.api}).
 *
 * @param allowedOrigins        erlaubte CORS-Ursprünge des Frontends
 * @param maxGames              maximale Anzahl gleichzeitig gehaltener Partien im Arbeitsspeicher
 * @param gameIdleTimeout       Partien ohne Zugriff werden nach dieser Zeit verworfen
 * @param rateLimitPerMinute    erlaubte rechenintensive Anfragen (Züge, Analysen, Computerzüge) je Client und Minute
 * @param maxRestoreMoves       maximale Zuganzahl bei der Wiederherstellung einer Partie
 */
@ConfigurationProperties("chesscoach.api")
public record ApiProperties(@DefaultValue({"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:8080"})
                            List<String> allowedOrigins,
                            @DefaultValue("500") int maxGames,
                            @DefaultValue("6h") Duration gameIdleTimeout,
                            @DefaultValue("120") int rateLimitPerMinute,
                            @DefaultValue("600") int maxRestoreMoves) {
}
