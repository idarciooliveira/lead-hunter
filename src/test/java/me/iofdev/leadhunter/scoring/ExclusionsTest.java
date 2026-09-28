package me.iofdev.leadhunter.scoring;

import static me.iofdev.leadhunter.scoring.TestPlaces.place;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import me.iofdev.leadhunter.campaign.CampaignFile;
import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.maps.ScrapedPlace;
import me.iofdev.leadhunter.place.PhoneNumber;
import me.iofdev.leadhunter.place.WebsiteKind;
import org.junit.jupiter.api.Test;

class ExclusionsTest {

    private static final CampaignFile.Search SEARCH = search(List.of(), 0);

    private static final List<CompanyProfile.Client> CLIENTS = List.of(
            new CompanyProfile.Client("Horizonte Tour", "+244 923 111 222"),
            new CompanyProfile.Client("ajabalg", null));

    private static CampaignFile.Search search(List<MapsSignal> disqualifying, int minReviews) {
        return new CampaignFile.Search(List.of("clínica"), List.of("Luanda"), null, null, null, List.of("hospital"),
                List.of("Kintexia"), null, null, disqualifying, minReviews);
    }

    private static Optional<String> check(ScrapedPlace place) {
        return check(place, SEARCH);
    }

    private static Optional<String> check(ScrapedPlace place, CampaignFile.Search search) {
        return Exclusions.check(place, PhoneNumber.parse(place.phone()), WebsiteKind.classify(place.website()),
                search, CLIENTS);
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

    @Test
    void excludesCurrentClientsByPhoneWhateverTheNameOnMaps() {
        assertThat(check(place("Agência de Viagens HT", "Agência", "00244923111222", null, 40)))
                .contains("Current client 'Horizonte Tour'");
        assertThat(check(place("Ajabalg Comércio", "Loja", "923999888", null, 40)))
                .contains("Current client 'ajabalg'");
    }

    @Test
    void excludesPlacesBelowTheCampaignMinimumReviews() {
        assertThat(check(place("Clínica Nova", "Clínica", "923456789", null, 3), search(List.of(), 10)))
                .contains("Only 3 reviews, the campaign needs at least 10");
    }

    @Test
    void excludesDisqualifyingSignals() {
        CampaignFile.Search noOwnSite = search(List.of(MapsSignal.OWN_WEBSITE), 0);

        assertThat(check(place("Clínica Girassol", "Clínica", "923456789", "https://girassol.ao", 50), noOwnSite))
                .contains("Disqualifying signal: has its own website");
        assertThat(check(place("Clínica Vida", "Clínica", "923456789", "https://facebook.com/vida", 50), noOwnSite))
                .isEmpty();
    }
}
