package me.iofdev.leadhunter.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets the TanStack Start client call the API from another origin (ADR 0031).
 * Reads run in server loaders, but mutations run in the browser, so the browser
 * enforces CORS on them. Temporary shape: ADR 0037 moves API calls into server functions
 * behind the shared login, and then the browser never touches the API directly.
 */
@Configuration
class ApiCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "PATCH", "POST", "PUT", "OPTIONS")
                .allowedHeaders("*");
    }
}
