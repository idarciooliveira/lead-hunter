package me.iofdev.leadhunter.api;

import me.iofdev.leadhunter.company.CompanyProfile;
import me.iofdev.leadhunter.company.CompanyRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The company profile as `company show` prints it, as JSON. */
@RestController
@RequestMapping("/api/company")
class CompanyController {

    private final CompanyRepository companies;

    CompanyController(CompanyRepository companies) {
        this.companies = companies;
    }

    @GetMapping
    CompanyProfile get() {
        return companies.find()
                .orElseThrow(() -> new IllegalArgumentException("no company profile yet. Run: company setup"));
    }
}
