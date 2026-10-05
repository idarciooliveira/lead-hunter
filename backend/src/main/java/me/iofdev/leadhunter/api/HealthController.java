package me.iofdev.leadhunter.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liveness probe for Docker, Railway and the web client. Always open, never any auth. */
@RestController
@RequestMapping("/api")
class HealthController {

    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
