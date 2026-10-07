package me.iofdev.leadhunter.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Answers 401 to every {@code /api/**} call that lacks {@code Authorization: Bearer <token>} (ADR 0037).
 * Only the web server's server functions hold the token, so the browser can never call the API.
 * {@code /api/health} stays open for Docker and Railway. With no token configured nothing gets through.
 */
@Component
class ServiceTokenFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final byte[] expected;
    private final JsonMapper json;

    ServiceTokenFilter(ApiProperties properties, JsonMapper json) {
        this.expected = properties.token().trim().getBytes(StandardCharsets.UTF_8);
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.startsWith("/api/") || path.equals("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (accepts(request.getHeader(HttpHeaders.AUTHORIZATION))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(response.getOutputStream(), new ApiError("missing or wrong API token"));
    }

    private boolean accepts(String header) {
        if (expected.length == 0 || header == null || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return false;
        }
        byte[] given = header.substring(BEARER.length()).trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, given);
    }
}
