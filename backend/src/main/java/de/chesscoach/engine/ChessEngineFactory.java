package de.chesscoach.engine;

import de.chesscoach.domain.position.MoveOracle;
import de.chesscoach.infrastructure.config.EngineProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Erzeugt die konfigurierte Engine-Implementierung. Die übrige Anwendung kennt nur {@link ChessEngine}. */
@Component
public class ChessEngineFactory {

    private final EngineProperties properties;
    private final MoveOracle moveOracle;

    public ChessEngineFactory(EngineProperties properties, MoveOracle moveOracle) {
        this.properties = properties;
        this.moveOracle = moveOracle;
    }

    public ChessEngine create(String role) {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("Hash", String.valueOf(properties.hashMb()));
        options.put("Threads", String.valueOf(properties.threads()));
        UciChessEngine.Settings settings;
        return switch (properties.type()) {
            case CALVIN -> {
                EngineProperties.Calvin calvin = properties.calvin();
                Path java = calvin.javaExecutable() == null || calvin.javaExecutable().isBlank()
                        ? CalvinChessEngine.currentJava() : Path.of(calvin.javaExecutable());
                Path jar = resolveJar(calvin.jarPath());
                settings = settings(role, calvin.version(), options);
                List<String> command;
                try {
                    command = CalvinChessEngine.command(java, jar, calvin.maxHeapMb());
                } catch (EngineUnavailableException e) {
                    yield new UnavailableEngine(role, calvin.version(), e.getMessage());
                }
                yield new CalvinChessEngine(new UciProcessManager("calvin-" + role, command, null,
                        properties.maxLineLength()), settings, moveOracle);
            }
            case UCI -> {
                settings = settings(role, properties.uci().version(), options);
                if (properties.uci().command().isEmpty()) {
                    yield new UnavailableEngine(role, properties.uci().version(), "Kein Engine-Kommando konfiguriert.");
                }
                yield new UciChessEngine(new UciProcessManager("uci-" + role, properties.uci().command(), null,
                        properties.maxLineLength()), settings, moveOracle);
            }
        };
    }

    private UciChessEngine.Settings settings(String role, String version, Map<String, String> options) {
        return new UciChessEngine.Settings(role, version, options, properties.startTimeout(), properties.searchGrace(),
                properties.maxSearchTime());
    }

    /** Relativer Pfad: zuerst relativ zum Arbeitsverzeichnis, dann relativ zu ./backend (Start aus dem Projektwurzelverzeichnis). */
    static Path resolveJar(String configured) {
        Path path = Path.of(configured);
        if (path.isAbsolute() || path.toFile().isFile()) {
            return path;
        }
        Path alternative = Path.of("backend").resolve(configured);
        return alternative.toFile().isFile() ? alternative : path;
    }
}
