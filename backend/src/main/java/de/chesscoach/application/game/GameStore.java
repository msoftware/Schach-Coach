package de.chesscoach.application.game;

import de.chesscoach.application.ErrorCode;
import de.chesscoach.application.GameException;
import de.chesscoach.infrastructure.config.ApiProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Comparator;
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

    /**
     * Nimmt eine neue Partie auf. Ist die Höchstzahl erreicht, wird die am längsten unbenutzte Partie verworfen statt
     * neue Partien abzulehnen; das Frontend stellt eine verworfene Partie bei Bedarf aus dem Browser-Speicher wieder
     * her. So kann niemand – auch nicht viele Clients hinter derselben IP-Adresse – andere am Spielen hindern.
     */
    public synchronized void put(GameSession session) {
        evictIdle();
        while (!sessions.isEmpty() && sessions.size() >= Math.max(1, properties.maxGames())) {
            sessions.values().stream()
                    .min(Comparator.comparingLong(GameSession::accessSequence))
                    .ifPresent(oldest -> sessions.remove(oldest.gameId()));
        }
        // Die neue Partie gilt ab jetzt als benutzt, sonst könnte sie (z. B. nach einer langen Wiederherstellung)
        // als älteste sofort wieder verdrängt werden.
        session.touch();
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

    /**
     * Verwirft inaktive Partien. Partien ohne einen einzigen Zug werden bereits nach der kürzeren
     * {@code emptyGameIdleTimeout} verworfen; sie lassen sich jederzeit neu starten.
     */
    @Scheduled(fixedDelayString = "PT5M")
    public void evictIdle() {
        Instant now = Instant.now();
        Instant limit = now.minus(properties.gameIdleTimeout());
        Instant emptyLimit = now.minus(properties.emptyGameIdleTimeout());
        sessions.values().removeIf(s -> s.lastAccess().isBefore(limit)
                || (s.lastAccess().isBefore(emptyLimit) && hasNoMoves(s)));
    }

    /** Liest die Zughistorie nur unter der Sperre; eine gerade gesperrte Partie ist aktiv und bleibt erhalten. */
    private static boolean hasNoMoves(GameSession session) {
        if (!session.lock().tryLock()) {
            return false;
        }
        try {
            return session.moves().isEmpty();
        } finally {
            session.lock().unlock();
        }
    }
}
