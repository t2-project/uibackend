package de.unistuttgart.t2.uibackend.TODO;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import de.unistuttgart.t2.uibackend.UIBackendService;

@Configuration
@EnableAutoConfiguration
public class TestContext {

//	@Bean
//	public UIBackendService backendService() {
//		return new UIBackendService();
//	}

	@Bean
	public RestTemplate template() {
		return new RestTemplate ();
	}

	
}
