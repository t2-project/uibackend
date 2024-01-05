package de.unistuttgart.t2.uibackend;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Interacts with other services to prepare data for the actual UI. (If did it right, this service is a API Gateway)
 *
 * @author maumau
 */
@SpringBootApplication
public class UIBackendApplication {

    @Value("${t2.orchestrator.url}")
    private String orchestratorUrl;
    @Value("${t2.cart.url}")
    private String cartUrl;
    @Value("${t2.inventory.url}")
    private String inventoryUrl;
    @Value("${t2.inventory.reservationendpoint}")
    private String reservationEndpoint;

    public static void main(String[] args) {
        SpringApplication.run(UIBackendApplication.class, args);
    }

    @Bean
    public RestTemplate template() {
        return new RestTemplate();
    }

    @Bean
    public UIBackendService service() {
        return new UIBackendService(cartUrl, inventoryUrl, orchestratorUrl, reservationEndpoint);
    }

    @Bean
    public OpenAPI customOpenAPI(@Value("${info.app.version:unknown}") String version) {
        return new OpenAPI().components(new Components()).info(new Info()
            .title("T2 UIBackend service API")
            .description("API of the T2-Project's UIBackend service.")
            .version(version));
    }
}
