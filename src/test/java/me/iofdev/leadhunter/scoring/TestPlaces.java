package me.iofdev.leadhunter.scoring;

import java.math.BigDecimal;
import java.util.List;

import me.iofdev.leadhunter.maps.ScrapedPlace;

final class TestPlaces {

    private TestPlaces() {
    }

    static ScrapedPlace place(String name, String category, String phone, String website, int reviews) {
        return new ScrapedPlace("id-" + name, name, category, List.of(), null, null, "Luanda", phone, website,
                BigDecimal.valueOf(4.2), reviews, null, null, null, false, false, "{}");
    }
}
