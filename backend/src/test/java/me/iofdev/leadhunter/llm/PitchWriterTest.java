package me.iofdev.leadhunter.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import me.iofdev.leadhunter.campaign.Campaign;
import me.iofdev.leadhunter.campaign.CampaignFile.Answers;
import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.company.CompanyProfile.CaseStudy;
import me.iofdev.leadhunter.llm.PitchWriter.Pitch;
import me.iofdev.leadhunter.pipeline.LeadStage;
import me.iofdev.leadhunter.pipeline.LeadStatus;
import me.iofdev.leadhunter.pipeline.LeadView;
import me.iofdev.leadhunter.place.WebsiteKind;
import me.iofdev.leadhunter.scoring.ScoreItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PitchWriterTest {

    private FakeLlmClient llm;
    private PitchWriter writer;

    private final CompanyProfile company = new CompanyProfile(
            "Exemplo Software",
            "Fazemos sites e sistemas para clínicas",
            List.of(new CompanyProfile.Service("Site", "400 mil Kz", "2 semanas")),
            "Análise gratuita do perfil",
            List.of("Luanda"),
            List.of(),
            List.of(new CaseStudy("clínicas", "Clínica Vida", "marcações por telefone", "site com marcação online",
                    "30% mais marcações", false)),
            List.of(),
            40,
            null);

    private final Campaign campaign = new Campaign(
            7L, "clinicas", "Clínicas",
            new Answers("clínicas", "marcações só por telefone", "Site", null, null, null, null, null, null, null),
            null, null, BigDecimal.ZERO);

    private final LeadView lead = new LeadView(
            42L, "clinicas", LeadStage.QUALIFIED, LeadStatus.NEW, null, null, 80,
            List.of(new ScoreItem("REVIEWS_SWEET_SPOT", 35, "142 reviews, the sweet spot"),
                    new ScoreItem("NO_HTTPS", 25, "Website without HTTPS"),
                    new ScoreItem("STAGE2_NO_ISSUES", 0, "nothing")),
            null, "Clínica Sorriso", "Clínica", "Rua 1", "Talatona", "+244923456789", true, null,
            WebsiteKind.NONE, new BigDecimal("4.3"), 142, "https://maps.example/p1", List.of("contact"), null);

    @BeforeEach
    void setUp() {
        llm = new FakeLlmClient();
        writer = new PitchWriter(llm);
    }

    @Test
    void keepsAPitchThatOnlyUsesNumbersFromTheData() {
        llm.answer = "  Bom dia! O site custa 400 mil Kz e ficou pronto em 2 semanas. Podemos falar?  ";

        Optional<Pitch> pitch = writer.write(campaign, company, lead);

        assertThat(pitch).hasValueSatisfying(p -> {
            assertThat(p.text()).startsWith("Bom dia!").endsWith("Podemos falar?");
            assertThat(p.model()).isEqualTo("test-model");
        });
    }

    @Test
    void dropsAPitchWithANumberTheDataDoesNotHave() {
        llm.answer = "Fazemos o site por 250 mil Kz. Podemos falar?";

        assertThat(writer.write(campaign, company, lead)).isEmpty();
    }

    @Test
    void anEmptyAnswerOrAFailedCallLeavesNoPitch() {
        llm.answer = "   ";
        assertThat(writer.write(campaign, company, lead)).isEmpty();

        llm.failure = new LlmException("gateway is down");
        assertThat(writer.write(campaign, company, lead)).isEmpty();
    }

    @Test
    void attributesTheCallToTheCampaignAsPitch() {
        llm.answer = "Bom dia, podemos falar?";

        writer.write(campaign, company, lead);

        assertThat(llm.last.json()).isFalse();
        assertThat(llm.last.campaignId()).isEqualTo(7L);
        assertThat(llm.last.purpose()).isEqualTo("pitch");
    }

    @Test
    void thePromptCarriesTheEvidenceTheProfileAndTheCases() {
        writer.write(campaign, company, lead);

        assertThat(llm.last.user())
                .contains("Clínica Sorriso", "Talatona", "4.3 from 142 reviews")
                .contains("Website without HTTPS", "Customers complain about: contact")
                .contains("Price: 400 mil Kz", "Delivery time: 2 semanas")
                .contains("clínicas, anonymous client: 30% mais marcações")
                .doesNotContain("STAGE2_NO_ISSUES", "nothing");
    }
}
