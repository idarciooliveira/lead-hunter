package me.iofdev.leadhunter.scoring;

import java.util.EnumSet;
import java.util.Set;

import me.iofdev.leadhunter.maps.ScrapedPlace;
import me.iofdev.leadhunter.place.WebsiteKind;

/**
 * What a campaign can see about a place on Google Maps. A campaign marks each signal as wanted or
 * disqualifying. Disqualifying signals exclude the place at stage 1; wanted ones go to the pitch.
 * Neither changes the weights in {@link Stage1Scorer}. See ADR 0019.
 */
public enum MapsSignal {

    NO_WEBSITE("no website"),
    SOCIAL_ONLY("only a social page"),
    OWN_WEBSITE("has its own website"),
    FEW_REVIEWS("fewer than " + Stage1Scorer.ACTIVE_MIN_REVIEWS + " reviews"),
    MANY_REVIEWS("more than " + Stage1Scorer.SWEET_SPOT_MAX_REVIEWS + " reviews"),
    LOW_RATING("rating below 4.0");

    /** Every place has exactly one of these, so disqualifying all of them would exclude everything. */
    public static final Set<MapsSignal> WEBSITE_SIGNALS = EnumSet.of(NO_WEBSITE, SOCIAL_ONLY, OWN_WEBSITE);

    /** Places below this many reviews show {@link #FEW_REVIEWS}. */
    public static final int FEW_REVIEWS_BELOW = Stage1Scorer.ACTIVE_MIN_REVIEWS;

    private final String label;

    MapsSignal(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** A place without a rating counts as unknown, never as a low rating. */
    public boolean matches(ScrapedPlace place, WebsiteKind website) {
        return switch (this) {
            case NO_WEBSITE -> website == WebsiteKind.NONE;
            case SOCIAL_ONLY -> website == WebsiteKind.SOCIAL_ONLY;
            case OWN_WEBSITE -> website == WebsiteKind.OWN;
            case FEW_REVIEWS -> place.reviewsCount() < Stage1Scorer.ACTIVE_MIN_REVIEWS;
            case MANY_REVIEWS -> place.reviewsCount() > Stage1Scorer.SWEET_SPOT_MAX_REVIEWS;
            case LOW_RATING -> place.rating() != null && place.reviewsCount() > 0
                    && place.rating().doubleValue() < 4.0;
        };
    }
}
