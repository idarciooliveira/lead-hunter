package me.iofdev.leadhunter.campaign;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.input.InvalidInputException;
import me.iofdev.leadhunter.scoring.MapsSignal;
import me.iofdev.leadhunter.scoring.Text;

/** Checks a campaign against the company profile and the other campaigns. See ADR 0019. */
public final class CampaignChecks {

    private CampaignChecks() {
    }

    /** Fails when the campaign pitches a service the company does not sell. */
    public static void requireFits(CampaignFile campaign, CompanyProfile company) {
        String service = campaign.answers().service();
        if (company.service(service).isEmpty()) {
            throw new InvalidInputException("campaign file", List.of("answers.service '" + service + "' is not one of the company's "
                    + "services: " + String.join(", ", company.services().stream().map(CompanyProfile.Service::name).toList())
                    + ". Add it with company update, or pick another"));
        }
    }

    /** Contacts per week still free after the other campaigns that are running on {@code today}. */
    public static int freeCapacity(CompanyProfile company, List<Campaign> campaigns, String slug, LocalDate today) {
        int used = campaigns.stream()
                .filter(c -> !c.slug().equals(slug))
                .map(c -> c.answers() == null ? null : c.answers().goal())
                .filter(goal -> goal != null && goal.leadsPerWeek() != null)
                .filter(goal -> goal.endDate() == null || !goal.endDate().isBefore(today))
                .mapToInt(CampaignFile.Goal::leadsPerWeek)
                .sum();
        return company.weeklyCapacity() - used;
    }

    /** Things that are allowed but probably a mistake. */
    public static List<String> warnings(CampaignFile campaign, CompanyProfile company, List<Campaign> campaigns,
                                        LocalDate today) {
        List<String> warnings = new ArrayList<>();
        CampaignFile.Search search = campaign.search();
        fewReviewsConflict(search).ifPresent(warnings::add);
        caseSectorMismatch(campaign.answers()).ifPresent(warnings::add);
        CampaignFile.Goal goal = campaign.answers().goal();
        if (goal != null && goal.leadsPerWeek() != null && (goal.endDate() == null || !goal.endDate().isBefore(today))) {
            int free = freeCapacity(company, campaigns, campaign.slug(), today);
            if (goal.leadsPerWeek() > free) {
                warnings.add("this campaign plans " + goal.leadsPerWeek() + " contacts a week, but only " + Math.max(free, 0)
                        + " of the company's " + company.weeklyCapacity() + " are free");
            }
        }
        return warnings;
    }

    static Optional<String> fewReviewsConflict(CampaignFile.Search search) {
        if (search.wantedSignals().contains(MapsSignal.FEW_REVIEWS)
                && search.minReviews() >= MapsSignal.FEW_REVIEWS_BELOW) {
            return Optional.of("'" + MapsSignal.FEW_REVIEWS.label() + "' is wanted, but places need at least "
                    + search.minReviews() + " reviews, so no wanted place can qualify");
        }
        return Optional.empty();
    }

    static Optional<String> caseSectorMismatch(CampaignFile.Answers answers) {
        if (answers.caseStudy() == null || answers.sector() == null || sectorsMatch(answers.sector(), answers.caseStudy().sector())) {
            return Optional.empty();
        }
        return Optional.of("the case is from '" + answers.caseStudy().sector() + "', not '" + answers.sector()
                + "'. The pitch will present it as a case from another sector");
    }

    /** True when a word of one sector appears in the other, ignoring accents and a plural s. */
    public static boolean sectorsMatch(String a, String b) {
        String right = Text.normalize(b);
        for (String word : Text.normalize(a).split("[^a-z0-9]+")) {
            String stem = word.endsWith("s") ? word.substring(0, word.length() - 1) : word;
            if (stem.length() >= 3 && right.contains(stem)) {
                return true;
            }
        }
        return false;
    }
}
