package me.iofdev.leadhunter.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import me.iofdev.leadhunter.place.PlaceReview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ReviewComplaintsTest {

    private FakeLlmClient llm;
    private ReviewComplaints complaints;

    @BeforeEach
    void setUp() {
        llm = new FakeLlmClient();
        complaints = new ReviewComplaints(llm, new JsonMapper());
    }

    private static PlaceReview review(String text) {
        return new PlaceReview(2, text, "2026-08-01");
    }

    @Test
    void noReviewsMeansNoCall() {
        assertThat(complaints.classify(7L, List.of())).isEmpty();
        assertThat(llm.last).isNull();
    }

    @Test
    void keepsKnownKindsAndDropsTheRest() {
        llm.answer = "{\"complaints\": [\"contact\", \"price\", \"WAITING\"]}";

        assertThat(complaints.classify(7L, List.of(review("Ninguém atende"), review("Muita espera"))))
                .containsExactly("contact", "waiting");
    }

    @Test
    void attributesTheCallToTheCampaignAsReviewAnalysis() {
        complaints.classify(7L, List.of(review("Boa clínica")));

        assertThat(llm.last.json()).isTrue();
        assertThat(llm.last.campaignId()).isEqualTo(7L);
        assertThat(llm.last.purpose()).isEqualTo("review-analysis");
        assertThat(llm.last.user()).contains("Boa clínica");
    }

    @Test
    void babbleMeansNoComplaints() {
        llm.answer = "contact and waiting, definitely";

        assertThat(complaints.classify(7L, List.of(review("x")))).isEmpty();
    }

    @Test
    void aFailedCallMeansNoComplaints() {
        llm.failure = new LlmException("gateway is down");

        assertThat(complaints.classify(7L, List.of(review("x")))).isEmpty();
    }
}
