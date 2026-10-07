package me.iofdev.leadhunter.api;

import me.iofdev.leadhunter.auth.OrgId;
import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.company.CompanyProfileParser;
import me.iofdev.leadhunter.company.CompanyRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The company profile as `company show` prints it, and `company update` replaces it, as JSON. */
@RestController
@RequestMapping("/api/company")
class CompanyController {

    private final CompanyRepository companies;

    CompanyController(CompanyRepository companies) {
        this.companies = companies;
    }

    @GetMapping
    CompanyProfile get(OrgId org) {
        return companies.find(org)
                .orElseThrow(() -> new IllegalArgumentException("no company profile yet. Run: company setup"));
    }

    /** Replaces the profile with the CLI's validation. Creates it when there is none. */
    @PutMapping
    SaveResult<CompanyProfile> put(OrgId org, @RequestBody CompanyProfile body) {
        CompanyProfileParser.validate(body);
        companies.save(org, body);
        return new SaveResult<>(body, CompanyProfileParser.warnings(body));
    }
}
