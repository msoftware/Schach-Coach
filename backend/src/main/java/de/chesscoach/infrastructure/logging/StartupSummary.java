package de.chesscoach.infrastructure.logging;

import de.chesscoach.coaching.CoachingProperties;
import de.chesscoach.infrastructure.config.AnalysisProperties;
import de.chesscoach.infrastructure.config.ApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Fasst beim Start die für den Betrieb wichtigen Einstellungen zusammen – vor allem jene, die hinter einem
 * Reverse-Proxy (Apache, nginx) über Erfolg oder Fehler entscheiden: freigegebene Ursprünge und Forward-Header.
 */
@Component
public class StartupSummary {

    private static final Logger log = LoggerFactory.getLogger(StartupSummary.class);

    private final Environment env;
    private final ApiProperties api;
    private final AnalysisProperties analysis;
    private final CoachingProperties coaching;

    public StartupSummary(Environment env, ApiProperties api, AnalysisProperties analysis, CoachingProperties coaching) {
        this.env = env;
        this.api = api;
        this.analysis = analysis;
        this.coaching = coaching;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logSummary() {
        String forwardHeaders = env.getProperty("server.forward-headers-strategy", "none");
        log.info("Schach-Coach bereit auf Port {} (Profile: {})", env.getProperty("local.server.port", "?"),
                String.join(", ", env.getActiveProfiles().length == 0 ? new String[]{"default"} : env.getActiveProfiles()));
        log.info("Erlaubte Ursprünge (CORS): {} · Forward-Header: {}", api.allowedOrigins(), forwardHeaders);
        if ("none".equalsIgnoreCase(forwardHeaders)) {
            log.info("Hinweis: Hinter einem Reverse-Proxy server.forward-headers-strategy=native setzen und die öffentliche "
                    + "Adresse (z. B. https://<domain>) in chesscoach.api.allowed-origins eintragen, sonst lehnt das Backend "
                    + "Anfragen des Browsers als fremden Ursprung ab.");
        }
        log.info("Partien: max. {} im Speicher, Leerlauf {} · Rate-Limit {} je Partie / {} je Adresse und Minute",
                api.maxGames(), api.gameIdleTimeout(), api.rateLimitPerMinute(), api.rateLimitPerMinutePerClient());
        log.info("Analyse: {} / {} Knoten (Stellung/Kandidaten), Nachprüfung ±{} cp ×{}, Warteschlange {} · Regelversion {}",
                analysis.rootNodes(), analysis.candidateNodes(), analysis.verifyMargin(), analysis.verifyFactor(),
                analysis.maxQueuedAnalyses(), coaching.rulesVersion());
        log.info("Logdateien in {}", Path.of(env.getProperty("logging.file.path", "logs")).toAbsolutePath());
    }
}
