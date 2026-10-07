package me.iofdev.leadhunter.cli;

import java.util.List;

import me.iofdev.leadhunter.auth.AuthRepository;
import me.iofdev.leadhunter.auth.AuthRepository.OrganizationRow;
import me.iofdev.leadhunter.auth.OrgId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Which organization a command works in (ADR 0043). The CLI acts as the operator, so it checks no membership:
 * {@code --org} wins, then {@code LEADHUNTER_ORG}, then the only organization. With several and no choice it fails.
 */
@Component
class CliOrg {

    private final AuthRepository auth;
    private final String configured;

    CliOrg(AuthRepository auth, @Value("${leadhunter.org:}") String configured) {
        this.auth = auth;
        this.configured = configured;
    }

    OrgId require(CommandSpec spec) {
        return require(((RootCommand) spec.root().userObject()).org);
    }

    OrgId require(String slug) {
        if (slug == null || slug.isBlank()) {
            slug = configured;
        }
        if (slug != null && !slug.isBlank()) {
            return new OrgId(auth.requireOrganization(slug.trim()).id());
        }
        List<OrganizationRow> all = auth.listOrganizations();
        if (all.isEmpty()) {
            throw new IllegalArgumentException("there are no organizations yet. Run: orgs add");
        }
        if (all.size() > 1) {
            throw new IllegalArgumentException("choose an organization with --org or LEADHUNTER_ORG: "
                    + String.join(", ", all.stream().map(OrganizationRow::slug).toList()));
        }
        return new OrgId(auth.requireOrganization(all.get(0).slug()).id());
    }
}
