package de.chesscoach.engine;

import de.chesscoach.infrastructure.config.EngineProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hält die beiden Engine-Instanzen: eine für Computerzüge ("play") und eine für das Coaching ("analysis").
 * Dadurch haben Computerzüge Vorrang und gleichzeitige Analysen verfälschen keine Spielzüge (Requirements 6.5).
 */
@Component
public class EngineRegistry {

    private static final Logger log = LoggerFactory.getLogger(EngineRegistry.class);

    private final ChessEngine playEngine;
    private final ChessEngine analysisEngine;
    private final EngineProperties properties;

    @org.springframework.beans.factory.annotation.Autowired
    public EngineRegistry(ChessEngineFactory factory, EngineProperties properties) {
        this.playEngine = factory.create("play");
        this.analysisEngine = factory.create("analysis");
        this.properties = properties;
    }

    /** Konstruktor für Tests mit vorgegebenen Engines. */
    public EngineRegistry(ChessEngine playEngine, ChessEngine analysisEngine, EngineProperties properties) {
        this.playEngine = playEngine;
        this.analysisEngine = analysisEngine;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startEngines() {
        if (!properties.startOnBoot()) {
            return;
        }
        for (ChessEngine engine : List.of(playEngine, analysisEngine)) {
            try {
                engine.start();
            } catch (EngineException e) {
                log.error("Engine '{}' ist nicht verfügbar: {}", engine.getEngineInfo().role(), e.getMessage());
            }
        }
    }

    public ChessEngine play() {
        return playEngine;
    }

    public ChessEngine analysis() {
        return analysisEngine;
    }

    public List<EngineInfo> status() {
        return List.of(playEngine.getEngineInfo(), analysisEngine.getEngineInfo());
    }

    /** Prüft die Spiel-Engine und startet sie bei Bedarf (z. B. nach einem Ausfall) erneut. */
    public boolean ensurePlayEngineAvailable() {
        if (playEngine.getEngineInfo().available()) {
            return true;
        }
        try {
            playEngine.start();
            return true;
        } catch (EngineException e) {
            return false;
        }
    }

    @PreDestroy
    public void shutdown() {
        playEngine.shutdown();
        analysisEngine.shutdown();
    }
}
