package me.iofdev.leadhunter.api;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class ApiWebConfig implements WebMvcConfigurer {

    private final OrgIdArgumentResolver orgIds;

    ApiWebConfig(OrgIdArgumentResolver orgIds) {
        this.orgIds = orgIds;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(orgIds);
    }
}
