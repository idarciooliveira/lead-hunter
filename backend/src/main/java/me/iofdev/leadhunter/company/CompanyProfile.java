package me.iofdev.leadhunter.company;

import java.util.List;
import java.util.Optional;

import me.iofdev.leadhunter.scoring.Text;

/**
 * Who we are and what we sell, answered once and shared by every campaign. See ADR 0019.
 * Stored as jsonb in the {@code company} table.
 */
public record CompanyProfile(
        String name,
        String intro,
        List<Service> services,
        String entryOffer,
        List<String> area,
        List<Client> clients,
        List<CaseStudy> cases,
        List<Objection> objections,
        Integer weeklyCapacity,
        QuarterTarget quarterTarget) {

    public static final int DEFAULT_WEEKLY_CAPACITY = 35;
    public static final String DEFAULT_AREA = "Luanda";

    public CompanyProfile {
        services = services == null ? List.of() : List.copyOf(services);
        area = area == null || area.isEmpty() ? List.of(DEFAULT_AREA) : List.copyOf(area);
        clients = clients == null ? List.of() : List.copyOf(clients);
        cases = cases == null ? List.of() : List.copyOf(cases);
        objections = objections == null ? List.of() : List.copyOf(objections);
        weeklyCapacity = weeklyCapacity == null ? DEFAULT_WEEKLY_CAPACITY : weeklyCapacity;
    }

    /** Finds a service by name, ignoring case and accents. */
    public Optional<Service> service(String name) {
        String wanted = Text.normalize(name);
        return services.stream().filter(s -> Text.normalize(s.name()).equals(wanted)).findFirst();
    }

    /** One thing we sell. {@code price} is free text such as "150 a 300 mil Kz". */
    public record Service(String name, String price, String deliveryTime) {
    }

    /** A current client. They never show up as a lead. The phone matches better than the name. */
    public record Client(String name, String phone) {
    }

    /** A result we can prove. The pitch may only claim cases from this list. */
    public record CaseStudy(String sector, String client, String problem, String built, String result,
                            Boolean mayName) {

        public CaseStudy {
            mayName = mayName != null && mayName;
        }

        /** The client's name when we may use it, otherwise a description without it. */
        public String label() {
            String who = mayName && client != null && !client.isBlank() ? client : "anonymous client";
            return sector + ", " + who + ": " + result;
        }
    }

    /** Something a lead says to say no, and how we answer it. */
    public record Objection(String objection, String answer) {
    }

    public record QuarterTarget(Integer newClients, Long revenueKz) {
    }
}
