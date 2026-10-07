package me.iofdev.leadhunter.cli;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import me.iofdev.leadhunter.auth.AuthRepository;
import me.iofdev.leadhunter.auth.AuthRepository.Organization;
import me.iofdev.leadhunter.auth.AuthRepository.OrganizationRow;
import me.iofdev.leadhunter.auth.OrgId;
import me.iofdev.leadhunter.llm.LlmProperties;
import me.iofdev.leadhunter.llm.OrgModelRepository;
import me.iofdev.leadhunter.pipeline.BudgetService;
import me.iofdev.leadhunter.usage.Money;
import me.iofdev.leadhunter.usage.UsageRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** Organizations are the tenants (ADR 0043). */
@Command(
        name = "orgs",
        description = "The organizations that own companies and campaigns.",
        mixinStandardHelpOptions = true,
        subcommands = {OrgsCommand.Add.class, OrgsCommand.ListOrgs.class, OrgsCommand.Budget.class, OrgsCommand.Model.class})
class OrgsCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "add", description = "Create an organization.", mixinStandardHelpOptions = true)
    static class Add implements Runnable {

        @Spec
        CommandSpec spec;

        @Parameters(index = "0", paramLabel = "NAME", description = "The company name, like \"Clínica Sorriso\".")
        String name;

        @Option(names = "--slug", paramLabel = "SLUG", description = "Short id for commands. Default: made from the name.")
        String slug;

        private final AuthRepository auth;

        Add(AuthRepository auth) {
            this.auth = auth;
        }

        @Override
        public void run() {
            String chosen = slug != null ? slug : CampaignWizard.slugify(name);
            if (chosen == null) {
                throw new IllegalArgumentException("the name has no letters or digits to make a slug from. Pass --slug");
            }
            Organization organization = auth.createOrganization(name, chosen);
            PrintWriter out = spec.commandLine().getOut();
            out.printf("Created organization %s (%s).%n", organization.name(), organization.slug());
            out.println("Next: users add <email> --name <name> --org " + organization.slug());
        }
    }

    @Command(name = "list", description = "List the organizations.", mixinStandardHelpOptions = true)
    static class ListOrgs implements Runnable {

        @Spec
        CommandSpec spec;

        private final AuthRepository auth;

        ListOrgs(AuthRepository auth) {
            this.auth = auth;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            var organizations = auth.listOrganizations();
            if (organizations.isEmpty()) {
                out.println("No organizations yet. Run: orgs add <name>");
                return;
            }
            out.printf("%-24s %-30s %s%n", "SLUG", "NAME", "MEMBERS");
            for (OrganizationRow organization : organizations) {
                out.printf("%-24s %-30s %d%n", Format.truncate(organization.slug(), 24),
                        Format.truncate(organization.name(), 30), organization.members());
            }
        }
    }

    /** Only the operator sets a budget, because the operator pays (ADR 0044). */
    @Command(name = "budget", description = "Show or set what an organization may spend a month.", mixinStandardHelpOptions = true)
    static class Budget implements Runnable {

        @Spec
        CommandSpec spec;

        @Parameters(index = "0", paramLabel = "SLUG", description = "The organization.")
        String slug;

        @Parameters(index = "1", arity = "0..1", paramLabel = "USD", description = "New monthly budget in USD. Leave out to show it.")
        BigDecimal amount;

        @Option(names = "--reset", description = "Go back to the default budget.")
        boolean reset;

        private final AuthRepository auth;
        private final UsageRepository usage;
        private final BudgetService budget;

        Budget(AuthRepository auth, UsageRepository usage, BudgetService budget) {
            this.auth = auth;
            this.usage = usage;
            this.budget = budget;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            if (reset && amount != null) {
                throw new IllegalArgumentException("pass an amount or --reset, not both");
            }
            if (amount != null && amount.signum() < 0) {
                throw new IllegalArgumentException("the budget cannot be negative");
            }
            Organization organization = auth.requireOrganization(slug);
            OrgId id = new OrgId(organization.id());
            if (reset || amount != null) {
                usage.setOrgBudget(id, reset ? null : amount);
            }
            boolean custom = usage.orgBudget(id).isPresent();
            out.printf("%s: $%s a month (%s). This month: %s spent or reserved.%n", organization.slug(),
                    budget.budgetFor(id).setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    custom ? "set by the operator" : "default",
                    Money.usd(budget.committedThisMonth(id)));
        }
    }

    /** The model comes from a short list the operator has tested (ADR 0044). */
    @Command(name = "model", description = "Show or set the model an organization's LLM calls use.", mixinStandardHelpOptions = true)
    static class Model implements Runnable {

        @Spec
        CommandSpec spec;

        @Parameters(index = "0", paramLabel = "SLUG", description = "The organization.")
        String slug;

        @Parameters(index = "1", arity = "0..1", paramLabel = "MODEL", description = "A model from LEADHUNTER_LLM_ALLOWED_MODELS. Leave out to show it.")
        String model;

        @Option(names = "--reset", description = "Go back to the default model.")
        boolean reset;

        private final AuthRepository auth;
        private final OrgModelRepository models;
        private final LlmProperties llm;

        Model(AuthRepository auth, OrgModelRepository models, LlmProperties llm) {
            this.auth = auth;
            this.models = models;
            this.llm = llm;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            if (reset && model != null) {
                throw new IllegalArgumentException("pass a model or --reset, not both");
            }
            List<String> allowed = new ArrayList<>(List.of(llm.model()));
            llm.allowedModels().stream().filter(m -> !allowed.contains(m)).forEach(allowed::add);
            if (model != null && !allowed.contains(model)) {
                throw new IllegalArgumentException("'" + model + "' is not an allowed model. Allowed: "
                        + String.join(", ", allowed) + ". Add it to LEADHUNTER_LLM_ALLOWED_MODELS after testing it");
            }
            Organization organization = auth.requireOrganization(slug);
            OrgId id = new OrgId(organization.id());
            if (reset || model != null) {
                models.set(id, reset ? null : model);
            }
            String chosen = models.find(id).orElse(null);
            out.printf("%s: %s%s%n", organization.slug(), chosen == null ? llm.model() : chosen,
                    chosen == null ? " (default)" : "");
            if (reset || model != null) {
                return;
            }
            out.println("Allowed: " + String.join(", ", allowed));
        }
    }
}
