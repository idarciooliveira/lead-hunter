package me.iofdev.leadhunter.pipeline;

import java.util.Optional;

import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignRepository;
import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.company.CompanyRepository;
import me.iofdev.leadhunter.llm.PitchWriter;
import me.iofdev.leadhunter.llm.PitchWriter.Pitch;
import org.springframework.stereotype.Service;

/** Writes and stores a lead's pitch (ADR 0040). Enrichment, {@code leads pitch} and the API share it. */
@Service
public class PitchService {

    private final PitchWriter writer;
    private final LeadRepository leads;
    private final CampaignRepository campaigns;
    private final CompanyRepository company;

    public PitchService(PitchWriter writer, LeadRepository leads, CampaignRepository campaigns,
                        CompanyRepository company) {
        this.writer = writer;
        this.leads = leads;
        this.campaigns = campaigns;
        this.company = company;
    }

    /** Writes and stores the pitch. Empty when the model failed or the guard dropped the answer. */
    public Optional<Pitch> write(Campaign campaign, CompanyProfile profile, long leadId) {
        LeadView lead = leads.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("no lead with id " + leadId));
        Optional<Pitch> pitch = writer.write(campaign, profile, lead);
        pitch.ifPresent(written -> leads.savePitch(leadId, written.text(), written.model()));
        return pitch;
    }

    /**
     * Writes a new pitch for one lead, replacing the old one. The old pitch stays when no new one comes.
     *
     * @throws IllegalArgumentException with a {@code no ...} message for an unknown lead, campaign or
     *         company profile (the API maps it to 404), and another message when no pitch came back (400)
     */
    public LeadView regenerate(long leadId) {
        LeadView lead = leads.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("no lead with id " + leadId));
        Campaign campaign = campaigns.findBySlug(lead.campaignSlug())
                .orElseThrow(() -> new IllegalArgumentException("no campaign '" + lead.campaignSlug() + "'"));
        CompanyProfile profile = company.find().orElseThrow(
                () -> new IllegalArgumentException("no company profile yet. Run: company setup"));
        if (write(campaign, profile, leadId).isEmpty()) {
            throw new IllegalArgumentException("could not write a pitch for lead " + leadId
                    + ". The model failed or invented a number; try again");
        }
        return leads.findById(leadId).orElseThrow();
    }
}
