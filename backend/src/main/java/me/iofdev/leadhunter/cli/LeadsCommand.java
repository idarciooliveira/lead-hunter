package me.iofdev.leadhunter.cli;

import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;

import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import me.iofdev.leadhunter.pipeline.LeadRepository;
import me.iofdev.leadhunter.pipeline.LeadStage;
import me.iofdev.leadhunter.pipeline.LeadStatus;
import me.iofdev.leadhunter.pipeline.LeadView;
import me.iofdev.leadhunter.pipeline.LostReason;
import me.iofdev.leadhunter.pipeline.PitchService;
import me.iofdev.leadhunter.scoring.ScoreItem;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(
        name = "leads",
        description = "Browse ranked leads.",
        mixinStandardHelpOptions = true,
        subcommands = {LeadsCommand.ListLeads.class, LeadsCommand.Show.class, LeadsCommand.Pitch.class,
                LeadsCommand.Mark.class})
class LeadsCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    enum StageFilter { QUALIFIED, BELOW_CUT, EXCLUDED, ALL }

    @Command(name = "list", description = "Ranked leads for a campaign, best first.")
    static class ListLeads implements Runnable {

        @Spec
        CommandSpec spec;

        @Parameters(index = "0", description = "Campaign slug.")
        String slug;

        @Option(names = "--stage", defaultValue = "QUALIFIED",
                description = "QUALIFIED, BELOW_CUT, EXCLUDED or ALL. Default: ${DEFAULT-VALUE}.")
        StageFilter stage;

        @Option(names = "--limit", defaultValue = "20", description = "Rows to show. Default: ${DEFAULT-VALUE}.")
        int limit;

        private final CampaignRepository campaigns;
        private final LeadRepository leads;

        ListLeads(CampaignRepository campaigns, LeadRepository leads) {
            this.campaigns = campaigns;
            this.leads = leads;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            Campaign campaign = CampaignCommand.requireCampaign(campaigns, slug);
            Optional<LeadStage> filter = stage == StageFilter.ALL
                    ? Optional.empty()
                    : Optional.of(LeadStage.valueOf(stage.name()));
            List<LeadView> rows = leads.list(campaign.id(), filter, limit);
            if (rows.isEmpty()) {
                String which = stage == StageFilter.ALL ? "" : stage.name().toLowerCase().replace('_', ' ') + " ";
                out.printf("No %sleads in '%s'. Run: campaign run %s%n", which, slug, slug);
                return;
            }
            out.printf("%-6s %-5s %-34s %-22s %7s %-11s %-15s%n",
                    "ID", "SCORE", "NAME", "CATEGORY", "REVIEWS", "WEBSITE", "PHONE");
            for (LeadView lead : rows) {
                out.printf("%-6d %-5d %-34s %-22s %7d %-11s %-15s%n",
                        lead.id(), lead.score(), Format.truncate(lead.name(), 34),
                        Format.truncate(lead.category(), 22), lead.reviewsCount(), lead.websiteKind(),
                        Format.orDash(lead.phoneE164()));
            }
            out.println("Details: leads show <id>");
        }
    }

    @Command(name = "show", description = "Full lead card with the score breakdown.")
    static class Show implements Runnable {

        @Spec
        CommandSpec spec;

        @Parameters(index = "0", description = "Lead id.")
        long id;

        private final LeadRepository leads;

        Show(LeadRepository leads) {
            this.leads = leads;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            LeadView lead = leads.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("no lead with id " + id));
            out.printf("%s  ·  score %d  ·  %s  ·  %s%n", lead.name(), lead.score(), lead.stage(), lead.status());
            out.printf("Campaign:  %s%n", lead.campaignSlug());
            out.printf("Category:  %s%n", Format.orDash(lead.category()));
            out.printf("Address:   %s%n", Format.orDash(lead.address()));
            out.printf("Reviews:   %d, rating %s%n", lead.reviewsCount(), lead.rating() == null ? "-" : lead.rating());
            out.printf("Website:   %s (%s)%n", Format.orDash(lead.website()), lead.websiteKind());
            out.printf("Phone:     %s%s%n", Format.orDash(lead.phoneE164()), lead.phoneMobile() ? ", mobile" : "");
            if (lead.whatsappLink() != null) {
                out.printf("WhatsApp:  %s%n", lead.whatsappLink());
            }
            out.printf("Maps:      %s%n", Format.orDash(lead.mapsUrl()));
            if (lead.stageReason() != null) {
                out.printf("Stage:     %s%n", lead.stageReason());
            }
            if (!lead.breakdown().isEmpty()) {
                out.println("Why:");
                for (ScoreItem item : lead.breakdown()) {
                    out.printf("  %+4d  %s%n", item.points(), item.reason());
                }
            }
            if (lead.pitch() != null) {
                out.println("Pitch:");
                out.println(lead.pitch());
            } else if (lead.stage() == LeadStage.QUALIFIED) {
                out.printf("Pitch:     none yet. Run: leads pitch %d%n", lead.id());
            }
        }
    }

    @Command(name = "pitch", description = "Write a new WhatsApp pitch for a lead. Spends LLM credit. See ADR 0040.")
    static class Pitch implements Runnable {

        @Spec
        CommandSpec spec;

        @Parameters(index = "0", description = "Lead id.")
        long id;

        private final PitchService pitches;

        Pitch(PitchService pitches) {
            this.pitches = pitches;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            LeadView lead = pitches.regenerate(id);
            out.printf("Pitch for lead %d '%s':%n%s%n", lead.id(), lead.name(), lead.pitch());
        }
    }

    @Command(name = "mark", description = "Mark a contact outcome on a lead. See ADR 0012 and ADR 0020.")
    static class Mark implements Runnable {

        @Spec
        CommandSpec spec;

        @Parameters(index = "0", description = "Lead id.")
        long id;

        @Option(names = "--status", required = true,
                description = "NEW, CONTACTED, NO_ANSWER, INTERESTED, MEETING, PROPOSAL_SENT, WON or LOST.")
        LeadStatus status;

        @Option(names = "--lost-reason",
                description = "Required when --status LOST: NO_BUDGET, WRONG_PERSON, HAS_SUPPLIER, NOT_INTERESTED or NOT_NOW.")
        LostReason lostReason;

        @Option(names = "--note", description = "Optional note stored with the outcome.")
        String note;

        private final LeadRepository leads;

        Mark(LeadRepository leads) {
            this.leads = leads;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            LeadView lead = leads.updateOutcome(id, status, lostReason, note);
            if (lead.status() == LeadStatus.LOST) {
                out.printf("Marked lead %d '%s' as LOST (%s).%n", lead.id(), lead.name(), lead.lostReason());
            } else {
                out.printf("Marked lead %d '%s' as %s.%n", lead.id(), lead.name(), lead.status());
            }
        }
    }
}
