package me.iofdev.leadhunter.scoring;

import static me.iofdev.leadhunter.scoring.TestPlaces.place;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import me.iofdev.leadhunter.maps.ScrapedPlace;
import me.iofdev.leadhunter.place.PhoneNumber;
import me.iofdev.leadhunter.place.WebsiteKind;
import org.junit.jupiter.api.Test;

class Stage1ScorerTest {

    private static final List<String> TARGETS = List.of("clínica", "escola");

    private static Score score(ScrapedPlace place) {
        return Stage1Scorer.score(place, PhoneNumber.parse(place.phone()), WebsiteKind.classify(place.website()), TARGETS);
    }

    private static List<String> codes(Score score) {
        return score.items().stream().map(ScoreItem::code).toList();
    }

    @Test
    void busyClinicWithoutWebsiteIsTheBestCase() {
        Score score = score(place("Clínica Sorriso", "Clínica dentária", "+244923456789", null, 142));

        assertThat(codes(score)).containsExactly("NO_WEBSITE_ACTIVE", "REVIEWS_SWEET_SPOT", "MOBILE_PHONE", "TARGET_SECTOR");
        assertThat(score.total()).isEqualTo(65);
    }

    @Test
    void socialOnlyWebsiteScoresButLessThanNone() {
        Score score = score(place("Escola Futuro", "Escola", "222123456", "https://facebook.com/escolafuturo", 35));

        assertThat(codes(score)).containsExactly("SOCIAL_ONLY", "REVIEWS_SWEET_SPOT", "TARGET_SECTOR");
        assertThat(score.total()).isEqualTo(45);
    }

    @Test
    void tinyPlacesArePenalizedAndClampedAtZero() {
        Score score = score(place("Loja X", "Loja", "222123456", "https://lojax.ao", 2));

        assertThat(codes(score)).containsExactly("FEW_REVIEWS");
        assertThat(score.total()).isZero();
    }

    @Test
    void bigPlacesArePenalized() {
        Score score = score(place("Clínica Girassol", "Clínica", "923456789", "https://girassol.ao", 2500));

        assertThat(codes(score)).containsExactly("TOO_BIG", "MOBILE_PHONE", "TARGET_SECTOR");
        assertThat(score.total()).isZero();
    }

    @Test
    void everyItemExplainsItself() {
        Score score = score(place("Clínica Sorriso", "Clínica", "923456789", null, 142));

        assertThat(score.items()).allSatisfy(item -> assertThat(item.reason()).isNotBlank());
        assertThat(score.items().getFirst().reason()).contains("142");
    }
}
