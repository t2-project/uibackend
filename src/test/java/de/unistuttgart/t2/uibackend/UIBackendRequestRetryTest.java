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

import de.unistuttgart.t2.common.SagaRequest;
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
 * Test whether UIBackendservice retries requests as it should. The set up is similar to {@link UIBackendRequestTest}
 * except that the mock server does now always reply with a {@link HttpStatus#INTERNAL_SERVER_ERROR} at some point and
 * that the the test always assert that the request that received the Error is placed twice.
 * 
 * @author maumau
 */
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
    public void testConfirmOrder_failAtOrchestrator() throws Exception {
        SagaRequest request = new SagaRequest(JSONs.sessionId, "cardNumber", "cardOwner", "checksum", 42.0);
        System.out.println(mapper.writeValueAsString(request));

        // mock cart resonse (normal)
        mockServer.expect(ExpectedCount.once(), requestTo(JSONs.cartUrl + JSONs.sessionId))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(JSONs.cartResponse(), MediaType.APPLICATION_JSON));

        // mock inventory response (normal)
        mockServer.expect(ExpectedCount.once(), requestTo(JSONs.inventoryUrl + JSONs.productId))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(JSONs.inventoryResponse(), MediaType.APPLICATION_JSON));

        // what i actually want : verify request to orchestrator (failed)
        mockServer.expect(ExpectedCount.twice(), requestTo(JSONs.orchestratorUrl))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // execute
        assertThrows(OrderNotPlacedException.class, () -> service.confirmOrder(request.getSessionId(),
            request.getCardNumber(), request.getCardOwner(), request.getChecksum()));
        mockServer.verify();
    }

    @Test
    public void testGetSingleProduct_failAtInventory() throws Exception {

        mockServer.expect(ExpectedCount.twice(), requestTo(JSONs.inventoryUrl + JSONs.productId))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // execute
        service.getSingleProduct(JSONs.productId);
        mockServer.verify();
    }

    // Cart fails & should be requested twice.
    @Test
    public void testGetCart_failAtCart() throws Exception {

        mockServer.expect(ExpectedCount.twice(), requestTo(JSONs.cartUrl + JSONs.sessionId))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // execute
        service.getCartContent(JSONs.sessionId);
        mockServer.verify();
    }

    @Test
    public void testMakeReservation_failAtInventory() throws Exception {

        mockServer.expect(ExpectedCount.twice(), requestTo(reservationUrl)).andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // execute
        assertThrows(ReservationFailedException.class, () -> {
            service.makeReservations(sessionId, productId, 2);
        });

        mockServer.verify();
    }

    @Test
    public void testGetAllProducts_failAtInventory() throws Exception {

        mockServer.expect(ExpectedCount.twice(), requestTo(inventoryUrl))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // execute
        assertTrue(service.getAllProducts().isEmpty());
        mockServer.verify();
    }

    @Test
    public void testAddItemToCart_failAtCart() throws Exception {
        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + sessionId))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // execute
        assertThrows(CartInteractionFailedException.class, () -> service.addItemToCart(sessionId, productId, 1));
        mockServer.verify();

    }

    @Test
    public void testDeleteItemFromCart_failAtCart() throws Exception {
        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.twice(), requestTo(cartUrl + sessionId))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // execute
        assertThrows(CartInteractionFailedException.class, () -> service.deleteItemFromCart(sessionId, productId, 1));
        mockServer.verify();
    }

    @Test
    public void testGetProductsInCart_failAtCart() throws Exception {
        mockServer.expect(ExpectedCount.twice(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertTrue(service.getProductsInCart(sessionId).isEmpty());
        mockServer.verify();
    }

    @Test
    public void testGetProductsInCart_failAtInventory() throws Exception {
        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(cartResponseMulti(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.twice(), requestTo(inventoryUrl + productId))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        mockServer.expect(ExpectedCount.twice(), requestTo(inventoryUrl + anotherproductId))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertTrue(service.getProductsInCart(sessionId).isEmpty());
        mockServer.verify();
    }
}
