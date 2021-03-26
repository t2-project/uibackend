package de.unistuttgart.t2.uibackend;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
public class TestContext {

	@Bean
	public UIBackendService backendService() {
		return new UIBackendService();
	}

	
}
