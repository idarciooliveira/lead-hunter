package me.iofdev.leadhunter.llm;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import me.iofdev.leadhunter.place.PlaceReview;
import me.iofdev.leadhunter.scoring.Stage2Scorer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Asks the model which complaint kinds a lead's reviews mention. The answer only feeds the
 * {@code REVIEW_COMPLAINTS} rule in {@link Stage2Scorer}; anything unparseable means no complaints,
 * never a failed enrichment. Calls are attributed to the campaign as {@code review-analysis}.
 */
@Component
public class ReviewComplaints {

    private static final Logger log = LoggerFactory.getLogger(ReviewComplaints.class);

    static final String SYSTEM = """
            You read Google Maps reviews of a small business in Luanda and say which of these problems
            customers complain about: contact (hard to reach, phone not answered), booking (hard to book
            an appointment or table), waiting (long waits, slow service). Answer with only a JSON object
            like {"complaints": ["contact", "waiting"]}, using only those three words. Use [] when the
            reviews complain about none of them.""";

    private final LlmClient llm;
    private final JsonMapper json;

    public ReviewComplaints(LlmClient llm, JsonMapper json) {
        this.llm = llm;
        this.json = json;
    }

    /** Complaint kinds found in the reviews, lowercased and limited to what the scorer knows. */
    public Set<String> classify(Long campaignId, List<PlaceReview> reviews) {
        List<String> texts = reviews.stream()
                .map(PlaceReview::text)
                .filter(text -> text != null && !text.isBlank())
                .limit(20)
                .toList();
        if (texts.isEmpty()) {
            return Set.of();
        }
        StringBuilder user = new StringBuilder("Reviews:\n");
        for (int i = 0; i < texts.size(); i++) {
            user.append(i + 1).append(". ").append(texts.get(i).strip()).append('\n');
        }
        LlmResponse response;
        try {
            response = llm.complete(LlmRequest.json(SYSTEM, user.toString()).forCampaign(campaignId, "review-analysis"));
        } catch (RuntimeException e) {
            log.warn("review classification failed, treating as no complaints: {}", e.getMessage());
            return Set.of();
        }
        return parse(response.text());
    }

    private Set<String> parse(String text) {
        Set<String> kinds = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return kinds;
        }
        try {
            JsonNode root = json.readTree(text);
            JsonNode complaints = root.path("complaints");
            if (complaints.isArray()) {
                for (JsonNode kind : complaints) {
                    String name = kind.asString("").trim().toLowerCase();
                    if (Stage2Scorer.COMPLAINT_KINDS.contains(name)) {
                        kinds.add(name);
                    }
                }
            }
        } catch (RuntimeException e) {
            log.warn("could not parse the complaint classes '{}': {}", text, e.getMessage());
        }
        return kinds;
    }
}
