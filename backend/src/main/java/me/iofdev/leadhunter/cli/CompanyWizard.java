package me.iofdev.leadhunter.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.company.CompanyProfile.CaseStudy;
import me.iofdev.leadhunter.company.CompanyProfile.Client;
import me.iofdev.leadhunter.company.CompanyProfile.Objection;
import me.iofdev.leadhunter.company.CompanyProfile.QuarterTarget;
import me.iofdev.leadhunter.company.CompanyProfile.Service;

/**
 * Asks the company questions once. Run again, it offers the saved answers as defaults. See ADR 0019.
 */
final class CompanyWizard {

    /** Objections heard in every sector. The owner only types the answers. */
    static final List<String> COMMON_OBJECTIONS = List.of(
            "É caro",
            "Já tenho Instagram ou Facebook",
            "O meu sobrinho faz isso",
            "Agora não tenho tempo");

    static final int MAX_CASES_IN_WIZARD = 2;

    private final Prompter p;
    private final Optional<CompanyProfile> existing;

    CompanyWizard(Prompter prompter, Optional<CompanyProfile> existing) {
        this.p = prompter;
        this.existing = existing;
    }

    CompanyProfile run() {
        p.out().println(existing.isPresent()
                ? "Update the company profile. Press Enter to keep the value in [brackets]."
                : "Company profile. You answer this once; every campaign uses it.");
        p.out().println("Lists take one item per line; an empty line finishes the list.");
        Optional<CompanyProfile> old = existing;

        p.section("Who you are");
        String name = p.ask("C1/10  Company name", null, old.map(CompanyProfile::name).orElse(null), true);
        String intro = p.ask("C2/10  How do you introduce the company in one sentence, as you'd say it on the phone?",
                "e.g. Somos a X, fazemos sites e sistemas para PMEs em Luanda.",
                old.map(CompanyProfile::intro).orElse(null), true);

        p.section("What you sell");
        List<Service> services = p.askRows("C3/10  Services you sell",
                        "name | price range in Kz | delivery time (optional)",
                        old.map(c -> c.services().stream().map(s -> row(s.name(), s.price(), s.deliveryTime())).toList())
                                .orElse(List.of()),
                        2, true).stream()
                .map(row -> new Service(row.get(0), row.get(1), Prompter.field(row, 2)))
                .toList();
        String entryOffer = askEntryOffer(services, old.map(CompanyProfile::entryOffer).orElse(null));
        List<String> area = p.askList("C5/10  Where do you serve clients in person?",
                "municipalities or provinces; campaigns search here by default",
                old.map(CompanyProfile::area).orElse(List.of(CompanyProfile.DEFAULT_AREA)), true);

        p.section("Clients and proof");
        List<Client> clients = p.askRows("C6/10  Current clients. They never show up as leads, in any campaign.",
                        "name | phone. Names on Google Maps vary, so add the phone",
                        old.map(c -> c.clients().stream().map(cl -> row(cl.name(), cl.phone())).toList())
                                .orElse(List.of()),
                        1, false).stream()
                .map(row -> new Client(row.get(0), Prompter.field(row, 1)))
                .toList();
        long withoutPhone = clients.stream().filter(c -> c.phone() == null).count();
        if (withoutPhone > 0) {
            p.note(withoutPhone + " client(s) without a phone will be matched by name only.");
        }
        List<CaseStudy> cases = askCases(old.map(CompanyProfile::cases).orElse(List.of()));
        List<Objection> objections = askObjections(old.map(CompanyProfile::objections).orElse(List.of()));

        p.section("Capacity and target");
        int capacity = p.askInt("C9/10  How many leads can you contact per week, across all campaigns?", null,
                old.map(CompanyProfile::weeklyCapacity).orElse(CompanyProfile.DEFAULT_WEEKLY_CAPACITY), 1, 500);
        QuarterTarget target = askTarget(old.map(CompanyProfile::quarterTarget).orElse(null));

        return new CompanyProfile(name, intro, services, entryOffer, area, clients, cases, objections, capacity, target);
    }

    private String askEntryOffer(List<Service> services, String previous) {
        p.out().println("C4/10  Which one is your entry offer, the cheapest first step a new client can buy?");
        for (int i = 0; i < services.size(); i++) {
            p.note((i + 1) + ". " + services.get(i).name() + " (" + services.get(i).price() + ")");
        }
        int defaultIndex = 1;
        for (int i = 0; i < services.size(); i++) {
            if (services.get(i).name().equals(previous)) {
                defaultIndex = i + 1;
            }
        }
        int picked = p.askInt("Number", null, defaultIndex, 1, services.size());
        return services.get(picked - 1).name();
    }

    private List<CaseStudy> askCases(List<CaseStudy> previous) {
        p.out().println("C7/10  Results you can prove. The pitch only ever claims these.");
        if (!previous.isEmpty()) {
            previous.forEach(c -> p.note("[" + c.label() + "]"));
            if (!p.confirm("      Replace these cases?")) {
                return previous;
            }
        }
        p.note("Up to " + MAX_CASES_IN_WIZARD + " now. Add more later in company.yml.");
        p.note("Um caso mostra o que já consegues: setor, cliente, problema, resultado com número "
                + "— e.g. clínicas dentárias, Clínica X, marcava só por ligação, +40 marcações em 3 meses.");
        List<CaseStudy> cases = new ArrayList<>();
        while (cases.size() < MAX_CASES_IN_WIZARD && p.confirm("      Add a case?")) {
            String sector = p.ask("      Sector", "e.g. clínica dentária", null, true);
            String client = p.ask("      Client name", null, "anonymous", true);
            String problem = p.ask("      The problem they had", null, null, true);
            String built = p.ask("      What you built", null, null, true);
            String result;
            while (true) {
                result = p.ask("      The result, with a number", "e.g. marcações passaram de 40 para 90 por mês",
                        null, true);
                if (result.chars().anyMatch(Character::isDigit)) {
                    break;
                }
                p.note("A result without a number is not proof. Add one.");
            }
            boolean mayName = !client.equals("anonymous") && p.confirm("      May the pitch name the client?");
            cases.add(new CaseStudy(sector, client, problem, built, result, mayName));
        }
        return cases;
    }

    private List<Objection> askObjections(List<Objection> previous) {
        p.out().println("C8/10  Objections you hear in every sector. Type your answer, or Enter to skip.");
        Map<String, String> answers = new LinkedHashMap<>();
        previous.forEach(o -> answers.put(o.objection(), o.answer()));
        for (String objection : COMMON_OBJECTIONS) {
            String answer = p.ask("      \"" + objection + "\"", null, answers.get(objection), false);
            if (answer != null) {
                answers.put(objection, answer);
            }
        }
        List<List<String>> extra = p.askRows("      Other objections", "objection | answer", List.of(), 2, false);
        extra.forEach(row -> answers.put(row.get(0), row.get(1)));
        return answers.entrySet().stream().map(e -> new Objection(e.getKey(), e.getValue())).toList();
    }

    private QuarterTarget askTarget(QuarterTarget previous) {
        p.out().println("C10/10 Optional. Target for this quarter. Enter skips.");
        if (previous != null) {
            if (previous.newClients() != null || previous.revenueKz() != null) {
                p.note("[" + orDash(previous.newClients()) + " clientes novos, "
                        + orDash(previous.revenueKz()) + " Kz]");
            }
            if (!p.confirm("      Change it?")) {
                return previous;
            }
        }
        Integer clients = p.askOptionalInt("      New clients", null, 0, 1000);
        Long revenue = p.askOptionalLong("      Revenue in Kz", null, 0, Long.MAX_VALUE);
        return clients == null && revenue == null ? null : new QuarterTarget(clients, revenue);
    }

    private static String orDash(Object value) {
        return value == null ? "—" : value.toString();
    }

    private static List<String> row(String... fields) {
        List<String> row = new ArrayList<>();
        for (String field : fields) {
            if (field != null) {
                row.add(field);
            }
        }
        return row;
    }
}
