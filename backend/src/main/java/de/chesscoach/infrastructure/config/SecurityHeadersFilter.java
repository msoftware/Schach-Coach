package de.chesscoach.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Setzt dieselben Sicherheits-Header wie {@code frontend/nginx.conf}, damit auch der Betrieb als einzelnes JAR
 * (Frontend unter {@code static/}) geschützt ist. Die Content-Security-Policy gilt nur für das Frontend; die
 * Swagger-UI benötigt Inline-Skripte und die REST-API liefert nur JSON.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String CONTENT_SECURITY_POLICY = "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
            + "font-src 'self' data:; connect-src 'self'";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") && !path.startsWith("/swagger-ui") && !path.startsWith("/v3/api-docs")) {
            response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        }
        chain.doFilter(request, response);
    }
}
