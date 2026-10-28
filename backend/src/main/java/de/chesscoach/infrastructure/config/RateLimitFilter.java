package de.chesscoach.infrastructure.config;

import de.chesscoach.application.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Begrenzt rechenintensive Anfragen (Züge, Computerzüge, Analysen, Tipps, Partiestart) mit einfachen Token-Buckets.
 * Schützt die lokale Engine vor übermäßigen Analyseanforderungen (Requirements 9.1/11.4).
 * <p>
 * Das eigentliche Limit gilt je Partie, damit sich mehrere Personen hinter derselben IP-Adresse (gleicher Rechner,
 * Schul- oder Firmennetz) nicht gegenseitig ausbremsen. Ein zusätzliches, großzügigeres Limit je IP-Adresse
 * verhindert, dass ein einzelner Client es durch viele neue Partien umgeht.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    static final int MAX_BUCKETS = 10_000;
    static final String OVERFLOW_CLIENT = "overflow";
    private static final Pattern GAME_PATH = Pattern.compile("^/api/games/([0-9a-f]{32})(/.*)?$");

    private final int perGame;
    private final int perClient;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(ApiProperties properties) {
        this.perGame = properties.rateLimitPerMinute();
        this.perClient = properties.rateLimitPerMinutePerClient();
    }

    private static final class Bucket {
        private final int perMinute;
        private double tokens;
        private long lastRefill;

        Bucket(int perMinute) {
            this.perMinute = perMinute;
            this.tokens = perMinute;
            this.lastRefill = System.nanoTime();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !(path.startsWith("/api/games"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Matcher game = GAME_PATH.matcher(request.getRequestURI());
        boolean gameAllowed = perGame <= 0 || !game.matches() || tryAcquire("game:" + game.group(1), perGame);
        boolean clientAllowed = gameAllowed && (perClient <= 0 || tryAcquire("ip:" + request.getRemoteAddr(), perClient));
        if (!clientAllowed) {
            log.warn("Rate-Limit erreicht: {} ({} je Minute) für {}", gameAllowed ? "je Adresse" : "je Partie",
                    gameAllowed ? perClient : perGame, request.getRequestURI());
            response.setStatus(ErrorCode.RATE_LIMITED.status());
            response.setContentType("application/problem+json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", "10");
            response.getWriter().write("{\"type\":\"" + ErrorCode.RATE_LIMITED.type() + "\",\"title\":\""
                    + ErrorCode.RATE_LIMITED.title() + "\",\"status\":429,\"detail\":\"Zu viele Anfragen in kurzer Zeit. "
                    + "Bitte warte einen Moment.\",\"code\":\"RATE_LIMITED\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    boolean tryAcquire(String key, int perMinute) {
        if (buckets.size() > MAX_BUCKETS && !buckets.containsKey(key)) {
            evictRefilledBuckets();
            if (buckets.size() > MAX_BUCKETS) {
                // Sehr viele gleichzeitig aktive Schlüssel: neue teilen sich einen Bucket, statt alle Limits zurückzusetzen.
                key = OVERFLOW_CLIENT;
            }
        }
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(perMinute));
        synchronized (bucket) {
            long now = System.nanoTime();
            double refill = (now - bucket.lastRefill) / 60_000_000_000.0 * bucket.perMinute;
            bucket.tokens = Math.min(bucket.perMinute, bucket.tokens + refill);
            bucket.lastRefill = now;
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return true;
            }
            return false;
        }
    }

    /** Entfernt Buckets, die seit mindestens einer Minute unbenutzt und damit ohnehin wieder voll sind. */
    private void evictRefilledBuckets() {
        long threshold = System.nanoTime() - 60_000_000_000L;
        buckets.values().removeIf(bucket -> {
            synchronized (bucket) {
                return bucket.lastRefill - threshold <= 0;
            }
        });
    }
}
