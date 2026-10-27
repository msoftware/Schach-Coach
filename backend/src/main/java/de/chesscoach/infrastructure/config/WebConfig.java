package de.chesscoach.infrastructure.config;

import de.chesscoach.coaching.CoachingProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** CORS (nur konfigurierte Frontend-Ursprünge), Scheduling und OpenAPI-Metadaten. */
@Configuration
@EnableScheduling
public class WebConfig implements WebMvcConfigurer {

    private final ApiProperties apiProperties;

    public WebConfig(ApiProperties apiProperties) {
        this.apiProperties = apiProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(apiProperties.allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT")
                .allowedHeaders("Content-Type")
                .maxAge(3600);
    }

    @Bean
    public OpenAPI chessCoachOpenApi(CoachingProperties coaching) {
        return new OpenAPI().info(new Info()
                .title("Schach-Coach API")
                .version("1")
                .description("REST-API der Schach-Coaching-Anwendung. Alle Bewertungen sind aus Sicht des Benutzers angegeben; "
                        + "Fehler folgen RFC 9457 (application/problem+json). Regelversion " + coaching.rulesVersion() + ".")
                .license(new License().name("MIT")));
    }
}
