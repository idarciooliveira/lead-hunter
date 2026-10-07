package me.iofdev.leadhunter.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.iofdev.leadhunter.auth.OrgId;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs after the service token check (ADR 0037). The web server says who is signed in and which organization they
 * work in with {@code X-LeadHunter-User} and {@code X-LeadHunter-Org}. This filter answers 403 unless that user is a
 * member of that organization, then hands the organization to controllers as an {@link OrgId} argument (ADR 0043).
 * {@code /api/health} stays open.
 */
@Component
@Order(2)
class OrgMembershipFilter extends OncePerRequestFilter {

    static final String ATTRIBUTE = OrgMembershipFilter.class.getName() + ".org";

    private final JdbcClient jdbc;
    private final JsonMapper json;

    OrgMembershipFilter(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
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
        String user = request.getHeader("X-LeadHunter-User");
        String org = request.getHeader("X-LeadHunter-Org");
        if (user == null || user.isBlank() || org == null || org.isBlank() || !isMember(org.trim(), user.trim())) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            json.writeValue(response.getOutputStream(), new ApiError("not a member of this organization"));
            return;
        }
        request.setAttribute(ATTRIBUTE, new OrgId(org.trim()));
        chain.doFilter(request, response);
    }

    private boolean isMember(String org, String user) {
        return jdbc.sql("select exists (select 1 from member where organization_id = :org and user_id = :user)")
                .param("org", org)
                .param("user", user)
                .query(Boolean.class)
                .single();
    }
}
