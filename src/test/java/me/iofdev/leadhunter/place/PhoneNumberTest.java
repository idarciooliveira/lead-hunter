package me.iofdev.leadhunter.place;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumberTest {

    @ParameterizedTest
    @CsvSource({
            "'+244 923 456 789', +244923456789, true",
            "'00244923456789', +244923456789, true",
            "'923 456 789', +244923456789, true",
            "'244 222 123 456', +244222123456, false",
            "'222-123-456', +244222123456, false"
    })
    void normalizesAngolanNumbers(String raw, String e164, boolean mobile) {
        PhoneNumber phone = PhoneNumber.parse(raw).orElseThrow();

        assertThat(phone.e164()).isEqualTo(e164);
        assertThat(phone.angolan()).isTrue();
        assertThat(phone.mobile()).isEqualTo(mobile);
    }

    @Test
    void keepsForeignNumbersWithoutMarkingThemMobile() {
        PhoneNumber phone = PhoneNumber.parse("+351 912 345 678").orElseThrow();

        assertThat(phone.e164()).isEqualTo("+351912345678");
        assertThat(phone.angolan()).isFalse();
        assertThat(phone.mobile()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "n/a", "123"})
    void rejectsMissingOrTooShort(String raw) {
        assertThat(PhoneNumber.parse(raw)).isEmpty();
    }

    @Test
    void buildsWhatsappLink() {
        assertThat(PhoneNumber.parse("923456789").orElseThrow().whatsappLink())
                .isEqualTo("https://wa.me/244923456789");
    }
}
