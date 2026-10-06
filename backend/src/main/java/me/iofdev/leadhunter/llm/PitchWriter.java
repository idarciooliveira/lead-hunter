package me.iofdev.leadhunter.llm;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignFile.Answers;
import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.company.CompanyProfile.CaseStudy;
import me.iofdev.leadhunter.pipeline.LeadView;
import me.iofdev.leadhunter.scoring.ScoreItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Writes the WhatsApp pitch for one lead (ADR 0040). The pitch only gets to say what the prompt says:
 * an answer with a number the prompt did not contain is dropped, and so is a failed or empty call.
 * Calls are attributed to the campaign as {@code pitch}.
 */
@Component
public class PitchWriter {

    private static final Logger log = LoggerFactory.getLogger(PitchWriter.class);

    private static final Pattern NUMBER = Pattern.compile("\\d+");

    static final String SYSTEM = """
            You write the first WhatsApp message from a software factory in Luanda to the owner of a small
            business. Write in Portuguese from Angola, in the tone the user gives. Rules:
            - At most 600 characters, one short paragraph, no greeting line on its own.
            - Open with one concrete hook taken from the lead facts, such as a website that does not open
              on a phone or customers who say nobody answers. Never mention scores or points.
            - Offer only the service and the first step the user gives. Do not invent prices, delivery
              times, results, clients or statistics. Use a price or a number only if it is written in the
              user message.
            - You may use at most one case study, and only from the list. Name the client only when the
              list says the name may be used.
            - End with one simple question the owner can answer in a few words.
            Answer with the message only, no quotes and no explanation.""";

    private final LlmClient llm;

    public PitchWriter(LlmClient llm) {
        this.llm = llm;
    }

    /** A written pitch and the model that wrote it. */
    public record Pitch(String text, String model) {
    }

    public Optional<Pitch> write(Campaign campaign, CompanyProfile company, LeadView lead) {
        String user = prompt(campaign, company, lead);
        LlmResponse response;
        try {
            response = llm.complete(LlmRequest.text(SYSTEM, user).forCampaign(campaign.id(), "pitch"));
        } catch (RuntimeException e) {
            log.warn("pitch for lead {} failed, leaving it without one: {}", lead.id(), e.getMessage());
            return Optional.empty();
        }
        String text = response.text() == null ? "" : response.text().strip();
        if (text.isEmpty()) {
            return Optional.empty();
        }
        Set<String> unknown = numbers(text);
        unknown.removeAll(numbers(user));
        if (!unknown.isEmpty()) {
            log.warn("pitch for lead {} dropped, it has numbers the data does not: {}", lead.id(), unknown);
            return Optional.empty();
        }
        return Optional.of(new Pitch(text, response.model()));
    }

    static String prompt(Campaign campaign, CompanyProfile company, LeadView lead) {
        Answers answers = campaign.answers();
        StringBuilder out = new StringBuilder();
        out.append("Company: ").append(company.name()).append('\n');
        line(out, "About us", company.intro());
        line(out, "Service to offer", answers.service());
        company.service(answers.service()).ifPresent(service -> {
            line(out, "Price", service.price());
            line(out, "Delivery time", service.deliveryTime());
        });
        line(out, "First step to offer", firstNonBlank(answers.hook(), company.entryOffer()));
        line(out, "Problem we bet on", answers.problem());
        line(out, "Why now", answers.whyNow());
        line(out, "Tone", firstNonBlank(answers.tone(), Answers.DEFAULT_TONE));

        out.append("\nCase studies (use at most one):\n");
        List<CaseStudy> cases = answers.caseStudy() != null ? List.of(answers.caseStudy()) : company.cases();
        if (cases.isEmpty()) {
            out.append("- none\n");
        }
        for (CaseStudy study : cases) {
            out.append("- ").append(study.label()).append(" (built: ").append(study.built())
                    .append(", problem: ").append(study.problem()).append(")\n");
        }

        out.append("\nLead:\n");
        out.append("Name: ").append(lead.name()).append('\n');
        line(out, "Category", lead.category());
        line(out, "Neighbourhood", lead.neighborhood());
        if (lead.rating() != null) {
            out.append("Rating: ").append(lead.rating()).append(" from ").append(lead.reviewsCount())
                    .append(" reviews\n");
        }
        out.append("Website: ").append(lead.websiteKind()).append('\n');
        out.append("Facts about this business:\n");
        boolean any = false;
        for (ScoreItem item : lead.breakdown()) {
            if (item.points() > 0) {
                out.append("- ").append(item.reason()).append('\n');
                any = true;
            }
        }
        if (!lead.complaintKinds().isEmpty()) {
            out.append("- Customers complain about: ").append(String.join(", ", lead.complaintKinds())).append('\n');
            any = true;
        }
        if (!any) {
            out.append("- none beyond the category\n");
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String label, String value) {
        if (value != null && !value.isBlank()) {
            out.append(label).append(": ").append(value.strip()).append('\n');
        }
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static Set<String> numbers(String text) {
        Set<String> found = new HashSet<>();
        Matcher matcher = NUMBER.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return found;
    }
}
