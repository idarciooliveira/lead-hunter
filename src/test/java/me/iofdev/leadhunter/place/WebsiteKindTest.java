package me.iofdev.leadhunter.place;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class WebsiteKindTest {

    @ParameterizedTest
    @NullAndEmptySource
    void noUrlMeansNone(String url) {
        assertThat(WebsiteKind.classify(url)).isEqualTo(WebsiteKind.NONE);
    }

    @ParameterizedTest
    @CsvSource({
            "https://www.facebook.com/clinica, SOCIAL_ONLY",
            "http://m.facebook.com/clinica, SOCIAL_ONLY",
            "instagram.com/loja.ao, SOCIAL_ONLY",
            "https://linktr.ee/escola, SOCIAL_ONLY",
            "https://wa.me/244923456789, SOCIAL_ONLY",
            "https://clinica-sorriso.business.site, SOCIAL_ONLY",
            "https://www.clinicasorriso.co.ao, OWN",
            "clinicasorriso.ao/contactos, OWN",
            "https://notfacebook.com, OWN"
    })
    void classifiesByHost(String url, WebsiteKind expected) {
        assertThat(WebsiteKind.classify(url)).isEqualTo(expected);
    }
}
