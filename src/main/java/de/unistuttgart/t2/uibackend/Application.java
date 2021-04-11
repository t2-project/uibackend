package de.unistuttgart.t2.uibackend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Application {
	
	@Value("${t2.orchestrator.url}")
	private String orchestratorUrl;
	@Value("${t2.cart.url}")
	private String cartUrl;
	@Value("${t2.inventory.url}")
	private String inventoryUrl;
	@Value("${t2.inventory.reservationendpoint}")
	private String reservationEndpoint;


	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public RestTemplate template() {
		return new RestTemplate();
	}

	@Bean
	public UIBackendService backendService() {
		return new UIBackendService(cartUrl, inventoryUrl, orchestratorUrl, reservationEndpoint);
	}
}
