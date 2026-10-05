package me.iofdev.leadhunter.apify;

import java.math.BigDecimal;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param token               Apify API token, from {@code APIFY_TOKEN}
 * @param actorId             the Google Maps scraper actor, see ADR 0005
 * @param pollWait            how long each status request waits for the run to finish
 * @param maxRunTime          give up on a run after this long
 * @param costSettleDelay     Apify's first answer after a run ends can hold a preliminary cost, so we wait this long and read it again
 * @param estimatedUsdPerPlace used only for {@code --dry-run} estimates; set it to the actor's current price
 * @param maxPlacesPerRun     budget guard: refuse a campaign run that could return more places than this
 */
@ConfigurationProperties("leadhunter.apify")
public record ApifyProperties(
        String token,
        @DefaultValue("https://api.apify.com") String baseUrl,
        @DefaultValue("compass~crawler-google-places") String actorId,
        @DefaultValue("60s") Duration pollWait,
        @DefaultValue("20m") Duration maxRunTime,
        @DefaultValue("10s") Duration costSettleDelay,
        @DefaultValue("0.004") BigDecimal estimatedUsdPerPlace,
        @DefaultValue("600") int maxPlacesPerRun) {

    public boolean hasToken() {
        return token != null && !token.isBlank();
    }
}
