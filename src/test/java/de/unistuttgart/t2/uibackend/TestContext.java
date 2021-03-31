package de.unistuttgart.t2.uibackend;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import de.unistuttgart.t2.uibackend.UIBackendService;

@Configuration
public class TestContext {

	@Bean
	public RestTemplate template() {
		return new RestTemplate ();
	}
	
	@Bean
	public UIBackendService service() {
		return new UIBackendService("http://localhost:8080", "http://localhost:8081", "http://localhost:8082");
	}

	
}
