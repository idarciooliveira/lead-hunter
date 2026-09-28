package me.iofdev.leadhunter.scoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import me.iofdev.leadhunter.maps.ScrapedPlace;
import me.iofdev.leadhunter.place.PhoneNumber;
import me.iofdev.leadhunter.place.WebsiteKind;

/**
 * Scores a place using only what the stage 1 scrape returns. Website quality and review complaints
 * are scored in stage 2. Weights come from ADR 0007; change them there first.
 */
public final class Stage1Scorer {

    static final int NO_WEBSITE_ACTIVE = 30;
    static final int SOCIAL_ONLY = 20;
    static final int REVIEWS_SWEET_SPOT = 15;
    static final int MOBILE_PHONE = 10;
    static final int TARGET_SECTOR = 10;
    static final int FEW_REVIEWS = -15;
    static final int TOO_BIG = -20;

    static final int ACTIVE_MIN_REVIEWS = 20;
    static final int SWEET_SPOT_MAX_REVIEWS = 300;
    static final int FEW_REVIEWS_BELOW = 5;
    static final int TOO_BIG_ABOVE = 1000;

    private Stage1Scorer() {
    }

    public static Score score(ScrapedPlace place, Optional<PhoneNumber> phone, WebsiteKind website,
                              List<String> targetKeywords) {
        List<ScoreItem> items = new ArrayList<>();
        int reviews = place.reviewsCount();

        if (website == WebsiteKind.NONE && reviews >= ACTIVE_MIN_REVIEWS) {
            items.add(new ScoreItem("NO_WEBSITE_ACTIVE", NO_WEBSITE_ACTIVE,
                    "No website, but " + reviews + " Google reviews"));
        }
        if (website == WebsiteKind.SOCIAL_ONLY) {
            items.add(new ScoreItem("SOCIAL_ONLY", SOCIAL_ONLY,
                    "Website is only a social or link page: " + place.website()));
        }
        if (reviews >= ACTIVE_MIN_REVIEWS && reviews <= SWEET_SPOT_MAX_REVIEWS) {
            items.add(new ScoreItem("REVIEWS_SWEET_SPOT", REVIEWS_SWEET_SPOT,
                    reviews + " reviews: busy enough to pay, small enough to need help"));
        }
        if (reviews < FEW_REVIEWS_BELOW) {
            items.add(new ScoreItem("FEW_REVIEWS", FEW_REVIEWS,
                    "Only " + reviews + " reviews, may be too small or inactive"));
        }
        if (reviews > TOO_BIG_ABOVE) {
            items.add(new ScoreItem("TOO_BIG", TOO_BIG,
                    reviews + " reviews, likely has an agency or in-house team"));
        }
        if (phone.map(PhoneNumber::mobile).orElse(false)) {
            items.add(new ScoreItem("MOBILE_PHONE", MOBILE_PHONE,
                    "Angolan mobile number, likely on WhatsApp"));
        }
        matchedKeyword(place, targetKeywords).ifPresent(keyword -> items.add(new ScoreItem("TARGET_SECTOR",
                TARGET_SECTOR, "In a target sector: matches '" + keyword + "'")));

        return Score.of(items);
    }

    private static Optional<String> matchedKeyword(ScrapedPlace place, List<String> keywords) {
        String haystack = String.join(" | ", Stream.concat(
                Stream.of(place.name(), place.category()),
                place.categories().stream()).filter(v -> v != null).toList());
        return keywords.stream().filter(keyword -> Text.containsWord(haystack, keyword)).findFirst();
    }
}
