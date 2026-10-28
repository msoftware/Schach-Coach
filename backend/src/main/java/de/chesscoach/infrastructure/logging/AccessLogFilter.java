package de.chesscoach.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import de.chesscoach.infrastructure.config.ApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Zugriffsprotokoll für die REST-API (Logger {@code de.chesscoach.access}, eigene Datei chess-coach-access.log).
 * <p>
 * Jede Anfrage bekommt eine kurze Request-ID, die im MDC steht (damit in jeder Logzeile dieser Anfrage), als Header
 * {@code X-Request-Id} zurückgegeben und in Fehlerantworten genannt wird. Ändernde Anfragen und Fehler werden mit INFO
 * bzw. WARN protokolliert, erfolgreiche lesende Abfragen (z. B. das Abfragen des Analysefortschritts) nur mit DEBUG.
 * Client-Adressen werden gekürzt gespeichert; Partiedaten (Züge, Stellungen) werden nie protokolliert.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessLogFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID = "requestId";
    public static final String GAME_ID = "gameId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Logger access = LoggerFactory.getLogger("de.chesscoach.access");
    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);
    private static final Pattern GAME_PATH = Pattern.compile("^/api/games/([0-9a-f]{32})(/.*)?$");

    private final ApiProperties api;

    public AccessLogFilter(ApiProperties api) {
        this.api = api;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        String requestId = newRequestId();
        MDC.put(REQUEST_ID, requestId);
        Matcher game = GAME_PATH.matcher(request.getRequestURI());
        if (game.matches()) {
            MDC.put(GAME_ID, shortGameId(game.group(1)));
        }
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            log(request, response.getStatus(), (System.nanoTime() - started) / 1_000_000);
            explainCorsRejection(request, response.getStatus());
            MDC.remove(REQUEST_ID);
            MDC.remove(GAME_ID);
        }
    }

    private static void log(HttpServletRequest request, int status, long millis) {
        String line = String.format("%s %s %d %dms client=%s id=%s", request.getMethod(), request.getRequestURI(), status, millis,
                anonymize(request.getRemoteAddr()), MDC.get(REQUEST_ID));
        if (status >= 500) {
            access.warn(line);
        } else if (status >= 400 || !"GET".equals(request.getMethod())) {
            access.info(line);
        } else {
            access.debug(line);
        }
    }

    /**
     * Spring lehnt Anfragen eines nicht freigegebenen Ursprungs mit einer nackten 403 ab und protokolliert den Grund nur
     * auf DEBUG. Hinter einem Reverse-Proxy ist das der häufigste Einrichtungsfehler – daher hier mit Grund und Abhilfe.
     */
    private void explainCorsRejection(HttpServletRequest request, int status) {
        String origin = request.getHeader("Origin");
        if (status != 403 || origin == null || api.allowedOrigins().contains(origin)) {
            return;
        }
        String seen = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        log.warn("Anfrage vom Ursprung {} abgelehnt (CORS): nicht in chesscoach.api.allowed-origins {}; das Backend sieht "
                        + "die Anfrage als {}. Abhilfe: Ursprung freigeben und hinter einem Reverse-Proxy "
                        + "server.forward-headers-strategy=native, ProxyPreserveHost und X-Forwarded-Proto setzen.",
                origin, api.allowedOrigins(), seen);
    }

    static String newRequestId() {
        byte[] bytes = new byte[4];
        ThreadLocalRandom.current().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** Die ersten 8 Zeichen der Partie-ID reichen zum Zuordnen und halten die Logzeilen kurz. */
    public static String shortGameId(String gameId) {
        return gameId == null || gameId.length() < 8 ? gameId : gameId.substring(0, 8);
    }

    /**
     * IPv4 auf /24 gekürzt (192.168.1.0), IPv6 auf die ersten 48 Bit (2001:db8:85a3::). IPv6-Adressen werden dafür
     * als Adresse gelesen, damit auch Kurzschreibweisen wie {@code ::1} oder {@code 2001:db8::1} korrekt gekürzt werden.
     */
    static String anonymize(String address) {
        if (address == null || address.isBlank()) {
            return "-";
        }
        if (address.contains(":")) {
            try {
                // Ein IP-Literal wird ohne DNS-Abfrage geparst.
                byte[] bytes = InetAddress.getByName(address).getAddress();
                if (bytes.length == 16) {
                    String prefix = String.format("%x:%x:%x", ((bytes[0] & 0xff) << 8) | (bytes[1] & 0xff),
                            ((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff), ((bytes[4] & 0xff) << 8) | (bytes[5] & 0xff));
                    return "0:0:0".equals(prefix) ? "::" : prefix + "::";
                }
                address = InetAddress.getByAddress(bytes).getHostAddress(); // IPv4-gemappte Adresse
            } catch (UnknownHostException e) {
                return "?";
            }
        }
        int last = address.lastIndexOf('.');
        return last < 0 ? address : address.substring(0, last) + ".0";
    }
}
