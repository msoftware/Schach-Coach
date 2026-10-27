package de.chesscoach.infrastructure.config;

import de.chesscoach.application.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Begrenzt rechenintensive Anfragen (Züge, Computerzüge, Analysen, Tipps, Partiestart) je Client mit einem
 * einfachen Token-Bucket. Schützt die lokale Engine vor übermäßigen Analyseanforderungen (Requirements 9.1/11.4).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int perMinute;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(ApiProperties properties) {
        this.perMinute = properties.rateLimitPerMinute();
    }

    private static final class Bucket {
        private double tokens;
        private long lastRefill;

        Bucket(double tokens) {
            this.tokens = tokens;
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
        if (perMinute > 0 && !tryAcquire(request.getRemoteAddr())) {
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

    boolean tryAcquire(String client) {
        if (buckets.size() > 10_000) {
            buckets.clear();
        }
        Bucket bucket = buckets.computeIfAbsent(client, k -> new Bucket(perMinute));
        synchronized (bucket) {
            long now = System.nanoTime();
            double refill = (now - bucket.lastRefill) / 60_000_000_000.0 * perMinute;
            bucket.tokens = Math.min(perMinute, bucket.tokens + refill);
            bucket.lastRefill = now;
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return true;
            }
            return false;
        }
    }
}
