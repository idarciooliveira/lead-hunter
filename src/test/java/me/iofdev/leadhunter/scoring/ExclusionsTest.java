package me.iofdev.leadhunter.scoring;

import static me.iofdev.leadhunter.scoring.TestPlaces.place;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import me.iofdev.leadhunter.campaign.CampaignFile;
import me.iofdev.leadhunter.maps.ScrapedPlace;
import me.iofdev.leadhunter.place.PhoneNumber;
import org.junit.jupiter.api.Test;

class ExclusionsTest {

    private static final CampaignFile.Search SEARCH = new CampaignFile.Search(
            List.of("clínica"), List.of("Luanda"), null, null, null, List.of("hospital"), List.of("Kintexia"), null);

    private static Optional<String> check(ScrapedPlace place) {
        return Exclusions.check(place, PhoneNumber.parse(place.phone()), SEARCH);
    }

    @Test
    void keepsAnOrdinaryClinic() {
        assertThat(check(place("Clínica Sorriso", "Clínica dentária", "923456789", null, 50))).isEmpty();
    }

    @Test
    void excludesPlacesWithoutPhone() {
        assertThat(check(place("Clínica Sorriso", "Clínica", null, null, 50))).contains("No phone number");
    }

    @Test
    void excludesClosedPlaces() {
        ScrapedPlace closed = new ScrapedPlace("id", "Loja", "Loja", List.of(), null, null, null, "923456789",
                null, BigDecimal.ONE, 10, null, null, null, true, false, "{}");

        assertThat(check(closed)).contains("Permanently closed");
    }

    @Test
    void excludesBuiltInKeywordsInNameOrCategory() {
        assertThat(check(place("Banco Económico", "Banco", "923456789", null, 900)))
                .hasValueSatisfying(reason -> assertThat(reason).contains("banco"));
        assertThat(check(place("Loja Unitel Talatona", "Loja de telemóveis", "923456789", null, 40)))
                .hasValueSatisfying(reason -> assertThat(reason).contains("unitel"));
    }

    @Test
    void excludesCampaignKeywordsAndNames() {
        assertThat(check(place("Hospital Geral", "Hospital", "923456789", null, 300)))
                .hasValueSatisfying(reason -> assertThat(reason).contains("hospital"));
        assertThat(check(place("KINTEXIA Lda", "Empresa", "923456789", null, 10)))
                .contains("Excluded name 'Kintexia'");
    }
}
