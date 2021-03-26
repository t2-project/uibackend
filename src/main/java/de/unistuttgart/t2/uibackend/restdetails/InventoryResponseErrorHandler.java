package de.unistuttgart.t2.uibackend.restdetails;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

public class InventoryResponseErrorHandler extends DefaultResponseErrorHandler {

	@Override
	protected void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
		if (statusCode == HttpStatus.NOT_FOUND) {
			return;
		} 
		super.handleError(response, statusCode);
	}

	
}
