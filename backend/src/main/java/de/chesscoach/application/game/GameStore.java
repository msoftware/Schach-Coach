package de.chesscoach.application.game;

import de.chesscoach.application.ErrorCode;
import de.chesscoach.application.GameException;
import de.chesscoach.infrastructure.config.ApiProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hält laufende Partien ausschließlich im Arbeitsspeicher (keine dauerhafte serverseitige Speicherung).
 * Inaktive Partien werden nach einer konfigurierbaren Zeit verworfen.
 */
@Component
public class GameStore {

    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final ApiProperties properties;

    public GameStore(ApiProperties properties) {
        this.properties = properties;
    }

    /** Zufällige Sitzungskennung ohne Bezug zu Personen. */
    public String newGameId() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public void put(GameSession session) {
        evictIdle();
        if (sessions.size() >= properties.maxGames()) {
            throw new GameException(ErrorCode.TOO_MANY_GAMES,
                    "Der Server hält bereits die maximale Anzahl aktiver Partien. Bitte versuche es später erneut.");
        }
        sessions.put(session.gameId(), session);
    }

    public GameSession get(String gameId) {
        if (gameId == null || !gameId.matches("[0-9a-f]{32}")) {
            throw new GameException(ErrorCode.GAME_NOT_FOUND, "Die Partie existiert nicht (mehr).");
        }
        GameSession session = sessions.get(gameId);
        if (session == null) {
            throw new GameException(ErrorCode.GAME_NOT_FOUND,
                    "Die Partie existiert auf dem Server nicht (mehr). Sie kann aus dem Browser-Speicher wiederhergestellt werden.");
        }
        session.touch();
        return session;
    }

    public int size() {
        return sessions.size();
    }

    @Scheduled(fixedDelayString = "PT5M")
    public void evictIdle() {
        Instant limit = Instant.now().minus(properties.gameIdleTimeout());
        sessions.values().removeIf(s -> s.lastAccess().isBefore(limit));
    }
}
