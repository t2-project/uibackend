package de.unistuttgart.t2.uibackend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.t2.common.ReservationRequest;
import de.unistuttgart.t2.common.saga.SagaRequest;
import de.unistuttgart.t2.uibackend.exceptions.CartInteractionFailedException;
import de.unistuttgart.t2.uibackend.exceptions.OrderNotPlacedException;
import de.unistuttgart.t2.uibackend.exceptions.ReservationFailedException;
import de.unistuttgart.t2.uibackend.supplicants.JSONs;
import de.unistuttgart.t2.uibackend.supplicants.TestContext;

import static de.unistuttgart.t2.uibackend.supplicants.JSONs.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * 
 * What i am doing here: - set up the mock server to expect a certain request -
 * execute a service operation that calls some other service - that call now
 * ends up at the mock server - verify that mock server received the expected
 * request
 * 
 * 
 * @author maumau
 *
 */
//@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(TestContext.class)
@ActiveProfiles("test")
public class UIBackendRequestRetryTest {

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	UIBackendService service;

	@Autowired
	private RestTemplate template;

	private MockRestServiceServer mockServer;

	@BeforeEach
	public void setUp() {
		mockServer = MockRestServiceServer.createServer(template);
	}

	@Test
	public void testConfirmOrder() throws Exception {

		SagaRequest reqest = new SagaRequest(JSONs.sessionId, "cardNumber", "cardOwner", "checksum", 42.0);
		System.out.println(mapper.writeValueAsString(reqest));

		// mock cart resonse (normal)
		mockServer.expect(ExpectedCount.once(), requestTo(JSONs.cartUrl + JSONs.sessionId))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(JSONs.cartResponse, MediaType.APPLICATION_JSON));

		// mock inventory response (normal)
		mockServer.expect(ExpectedCount.once(), requestTo(JSONs.inventoryUrl + JSONs.productId))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(JSONs.inventoryResponse, MediaType.APPLICATION_JSON));

		// what i actually want : verify request to orchestrator (failed)
		mockServer.expect(ExpectedCount.twice(), requestTo(JSONs.orchestratorUrl)).andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json(mapper.writeValueAsString(reqest))).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		// execute
		assertThrows(OrderNotPlacedException.class, () -> service.confirmOrder(reqest.getSessionId(), reqest.getCardNumber(), reqest.getCardOwner(),
				reqest.getChecksum()));
		mockServer.verify();
	}

	@Test
	public void testGetSingleProduct() throws Exception {

		mockServer.expect(ExpectedCount.twice(), requestTo(JSONs.inventoryUrl + JSONs.productId))
				.andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		// execute
		service.getSingleProduct(JSONs.productId);
		mockServer.verify();
	}

	@Test
	public void testGetCart() throws Exception {

		mockServer.expect(ExpectedCount.twice(), requestTo(JSONs.cartUrl + JSONs.sessionId))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		// execute
		service.getCartContent(JSONs.sessionId);
		mockServer.verify();
	}

	@Test
	public void testMakeReservation() throws Exception {
		ReservationRequest request = new ReservationRequest(productId, sessionId, 2);

		mockServer.expect(ExpectedCount.twice(), requestTo(reservationUrl)).andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json(mapper.writeValueAsString(request)))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		// execute
		assertThrows(ReservationFailedException.class, () -> {
			service.makeReservations(sessionId, productId, 2);
		});

		mockServer.verify();
	}

	@Test
	public void testGetAllProducts() throws Exception {

		mockServer.expect(ExpectedCount.twice(), requestTo(inventoryUrl)).andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		// execute
		assertTrue(service.getAllProducts().isEmpty());
		mockServer.verify();
	}

	@Test
	public void testAddItemToCart() throws Exception {
		mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(cartResponse, MediaType.APPLICATION_JSON));

		mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.PUT))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json(updatedCartResponse)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		// execute
		assertThrows(ReservationFailedException.class, () -> service.addItemToCart(sessionId, productId, 1));
		mockServer.verify();

	}

	@Test
	public void testDeleteItemFromCart() throws Exception {
		mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(cartResponse, MediaType.APPLICATION_JSON));

		mockServer.expect(ExpectedCount.twice(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.PUT))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		// execute
		assertThrows(CartInteractionFailedException.class, () -> service.deleteItemFromCart(sessionId, productId, 1));
		mockServer.verify();
	}

	// TODO
	@Test
	public void testGetProductsInCart() throws Exception {
		mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(cartResponseMulti, MediaType.APPLICATION_JSON));

		mockServer.expect(ExpectedCount.twice(), requestTo(inventoryUrl + productId)).andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		mockServer.expect(ExpectedCount.twice(), requestTo(inventoryUrl + anotherproductId))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		assertTrue(service.getProductsInCart(sessionId).isEmpty());
		mockServer.verify();
	}

}
