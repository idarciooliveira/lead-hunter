package me.iofdev.leadhunter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LeadHunterApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(LeadHunterApplication.class);
        ConfigurableApplicationContext context = application.run(args);
        // The CLI runs to completion and exits with the command's code. The web
        // profile (ADR 0031) stays up serving /api; exiting here would stop Tomcat.
        if (application.getWebApplicationType() == WebApplicationType.NONE) {
            System.exit(SpringApplication.exit(context));
        }
    }
}
