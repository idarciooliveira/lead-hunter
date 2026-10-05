package me.iofdev.leadhunter.cli;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

import me.iofdev.leadhunter.campaign.CampaignChecks;
import me.iofdev.leadhunter.campaign.CampaignFile;
import me.iofdev.leadhunter.campaign.CampaignFile.Answers;
import me.iofdev.leadhunter.campaign.CampaignFile.Goal;
import me.iofdev.leadhunter.campaign.CampaignFile.StopRule;
import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.company.CompanyProfile.CaseStudy;
import me.iofdev.leadhunter.company.CompanyProfile.Objection;
import me.iofdev.leadhunter.scoring.MapsSignal;
import me.iofdev.leadhunter.scoring.Text;

/**
 * Asks the campaign questions plus the search settings in the terminal and builds a campaign.
 * Facts about the company come from the company profile. See ADR 0018 and ADR 0019.
 */
final class CampaignWizard {

    static final String DEFAULT_COUNTRY = "Angola";
    static final int DEFAULT_MEETINGS = 3;
    static final int DEFAULT_WINS = 1;
    static final int DEFAULT_WEEKS = 6;
    static final int DEFAULT_MIN_INTERESTED = 2;
    static final int STOP_RULE_WEEKS = 2;

    private final Prompter p;
    private final CompanyProfile company;
    private final ToIntFunction<String> freeCapacityBySlug;
    private final LocalDate today;
    private int freeCapacity;

    /** {@code freeCapacityBySlug} gives the contacts a week left by the other campaigns running today. */
    CampaignWizard(Prompter prompter, CompanyProfile company, ToIntFunction<String> freeCapacityBySlug, LocalDate today) {
        this.p = prompter;
        this.company = company;
        this.freeCapacityBySlug = freeCapacityBySlug;
        this.today = today;
    }

    CampaignFile run() {
        p.out().println("New campaign for " + company.name() + ". Press Enter to accept the value in [brackets].");
        p.out().println("Lists take one item per line; an empty line finishes the list.");
        p.out().println();

        String name = p.ask("Campaign name", "e.g. Clínicas em Luanda", null, true);
        String slug = p.ask("Short id for commands", "lowercase and dashes", slugify(name), true);
        freeCapacity = freeCapacityBySlug.applyAsInt(slug);
        if (freeCapacity < 1) {
            throw new IllegalStateException("other running campaigns already use all " + company.weeklyCapacity()
                    + " contacts a week. End one, or raise weeklyCapacity with company update");
        }

        p.section("The bet");
        String sector = p.ask("1/11  Sector, in a word or two", "e.g. clínicas dentárias", null, true);
        String problem = p.ask("2/11  What problem are you betting this sector has? One sentence, in the customer's words.",
                "e.g. Perdemos marcações porque os pacientes só conseguem ligar", null, true);
        String service = askService();
        String hook = p.ask("4/11  What does the lead get for free for replying?",
                "e.g. a mockup of their site, a 15 minute demo", Answers.DEFAULT_HOOK, true);
        String whyNow = p.ask("5/11  Optional. Why would they buy now rather than next year?",
                "e.g. recently opened, enrolment season", null, false);

        p.section("Who qualifies");
        List<MapsSignal> wanted = new ArrayList<>();
        List<MapsSignal> disqualifying = new ArrayList<>();
        askSignals(wanted, disqualifying);
        int minReviews = p.askInt("7/11  Minimum Google reviews for a business that can pay",
                "places below it are excluded", 0, 0, 100_000);
        CampaignChecks.fewReviewsConflict(wanted, minReviews)
                .ifPresent(message -> p.note("Warning: " + message));

        p.section("The pitch");
        String phoneRoutine = p.ask("8/11  On the phone: who answers, who do you ask for, when not to call?",
                null, Answers.DEFAULT_PHONE_ROUTINE, true);
        List<Objection> objections = p.askRows("9/11  Optional. Objections specific to this sector",
                        "objection | answer. The company objections are already included", List.of(), 2, false)
                .stream().map(row -> new Objection(row.get(0), row.get(1))).toList();
        CaseStudy caseStudy = askCase(sector);
        String tone = p.ask("11/11 Tone of the messages", null, Answers.DEFAULT_TONE, true);

        p.out().println();
        p.out().println("— Metas de campanha —");
        Goal goal = askGoal();

        p.out().println();
        p.out().println("— Procura no Google Maps —");
        p.out().println("A \"search term\" é o que digitamos no Google Maps. "
                + "As \"keywords\" filtram que resultados contam como alvo do setor.");
        List<String> terms = p.askList("Google Maps search terms", "e.g. clínica, clínica dentária", List.of(), true);
        List<String> locations = p.askList("Locations, one scraper run each", "e.g. Talatona, Luanda, Angola",
                defaultLocations(), true);
        int maxPlacesPerSearch = p.askInt("Max places per term per location", null,
                CampaignFile.Search.DEFAULT_MAX_PLACES_PER_SEARCH, 1, 200);
        List<String> targetKeywords = p.askList(
                "Keywords that mark a target sector — matched as whole words in the name or category. "
                        + "Defaults to the search terms.",
                null, terms, false);
        List<String> excludeKeywords = p.askList("Words to exclude",
                "matched in the name or category, e.g. agência digital", List.of(), false);
        List<String> excludeNames = p.askList("Extra names to exclude",
                "current clients from the company profile are always excluded", List.of(), false);

        return new CampaignFile(slug, name,
                new Answers(sector, problem, service, hook, whyNow, phoneRoutine, objections, caseStudy, goal, tone),
                new CampaignFile.Search(terms, locations, maxPlacesPerSearch, null, targetKeywords, excludeKeywords,
                        excludeNames, null, wanted, disqualifying, minReviews));
    }

    boolean confirm(String question) {
        return p.confirm(question);
    }

    private String askService() {
        List<CompanyProfile.Service> services = company.services();
        p.out().println("3/11  Which service do you pitch? One per campaign, so you can tell which one sells.");
        int defaultIndex = 1;
        for (int i = 0; i < services.size(); i++) {
            CompanyProfile.Service s = services.get(i);
            boolean entry = s.name().equals(company.entryOffer());
            p.note((i + 1) + ". " + s.name() + " (" + s.price() + ")" + (entry ? ", entry offer" : ""));
            if (entry) {
                defaultIndex = i + 1;
            }
        }
        return services.get(p.askInt("Number", null, defaultIndex, 1, services.size()) - 1).name();
    }

    private void askSignals(List<MapsSignal> wanted, List<MapsSignal> disqualifying) {
        while (true) {
            wanted.clear();
            disqualifying.clear();
            p.out().println("6/11  Google Maps signals. w = wanted, d = disqualifying, Enter = doesn't matter.");
            p.note("Disqualifying signals exclude the place. Wanted ones go to the pitch.");
            for (MapsSignal signal : MapsSignal.values()) {
                Character answer = p.askLetter("      " + signal.label(), "wd");
                if (answer == null) {
                    continue;
                }
                (answer == 'w' ? wanted : disqualifying).add(signal);
            }
            if (!MapsSignal.excludesEverything(disqualifying)) {
                return;
            }
            p.note("Every place has no website, a social page or its own website. Disqualifying all three "
                    + "excludes everything. Choose again.");
        }
    }

    private CaseStudy askCase(String sector) {
        List<CaseStudy> cases = company.cases();
        p.out().println("10/11 Which case proves it? With none, the pitch makes no claims.");
        if (cases.isEmpty()) {
            p.note("The company profile has no cases. Add them with company update.");
            return null;
        }
        for (int i = 0; i < cases.size(); i++) {
            p.note((i + 1) + ". " + cases.get(i).label());
        }
        Integer picked = p.askOptionalInt("Number, or Enter for none", null, 1, cases.size());
        if (picked == null) {
            return null;
        }
        CaseStudy chosen = cases.get(picked - 1);
        CampaignChecks.caseSectorMismatch(sector, chosen)
                .ifPresent(message -> p.note("Warning: " + message));
        return chosen;
    }

    private Goal askGoal() {
        int meetings = p.askInt("      Meetings", null, DEFAULT_MEETINGS, 0, 1000);
        int wins = p.askInt("      Won clients", null, DEFAULT_WINS, 0, 1000);
        LocalDate endDate = p.askDate("      End date", today.plusWeeks(DEFAULT_WEEKS), today);
        int leadsPerWeek = p.askInt("      Leads per week for this campaign",
                freeCapacity + " of " + company.weeklyCapacity() + " free", freeCapacity, 1, freeCapacity);
        p.out().println("      Stop or rethink when fewer than N leads are interested after M contacts");
        int minInterested = p.askInt("      N, interested", null, DEFAULT_MIN_INTERESTED, 1, 1000);
        int afterContacted = p.askInt("      M, contacted", STOP_RULE_WEEKS + " weeks of contacts",
                leadsPerWeek * STOP_RULE_WEEKS, 1, 10_000);
        return new Goal(meetings, wins, endDate, leadsPerWeek, new StopRule(minInterested, afterContacted));
    }

    private List<String> defaultLocations() {
        return company.area().stream()
                .map(area -> Text.normalize(area).contains(DEFAULT_COUNTRY.toLowerCase(Locale.ROOT))
                        ? area : area + ", " + DEFAULT_COUNTRY)
                .toList();
    }

    static String slugify(String value) {
        String slug = Text.normalize(value).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        return slug.isEmpty() ? null : slug;
    }
}
