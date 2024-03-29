package de.unistuttgart.t2.uibackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.t2.common.ReservationRequest;
import de.unistuttgart.t2.common.SagaRequest;
import de.unistuttgart.t2.uibackend.supplicants.JSONs;
import de.unistuttgart.t2.uibackend.supplicants.TestContext;
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

import static de.unistuttgart.t2.uibackend.supplicants.JSONs.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Test whether UIBackendService makes the right requests.
 * <p>
 * The Setup is like this:
 * <ul>
 * <li>Call the operation under test.
 * <li>Uses the mock server to receive the request and verify that it is placed as intended.
 * </ul>
 * 
 * @author maumau
 */
@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(TestContext.class)
@ActiveProfiles("test")
public class UIBackendRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

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

        SagaRequest request = new SagaRequest(JSONs.sessionId, "cardNumber", "cardOwner", "checksum", 42.0);
        System.out.println(mapper.writeValueAsString(request));

        // mock cart response
        mockServer.expect(ExpectedCount.once(), requestTo(JSONs.cartUrl + "/" + JSONs.sessionId))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(JSONs.cartResponse(), MediaType.APPLICATION_JSON));

        // mock inventory response
        mockServer.expect(ExpectedCount.once(), requestTo(JSONs.inventoryUrl + "/" + JSONs.productId))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(inventoryResponse(), MediaType.APPLICATION_JSON));

        // what i actually want : verify request to orchestrator
        mockServer.expect(ExpectedCount.once(), requestTo(JSONs.orchestratorUrl)).andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.cardNumber").value("cardNumber"))
            .andExpect(jsonPath("$.cardOwner").value("cardOwner"))
            .andExpect(jsonPath("$.checksum").value("checksum"))
            .andExpect(jsonPath("$.sessionId").value(JSONs.sessionId))
            .andExpect(jsonPath("$.total").value(42))
            .andExpect(content().json(mapper.writeValueAsString(request))).andRespond(withStatus(HttpStatus.OK));

        // mock delete cart
        mockServer.expect(ExpectedCount.once(), requestTo(JSONs.cartUrl + "/" + JSONs.sessionId))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withSuccess());

        // execute
        service.confirmOrder(request.getSessionId(), request.getCardNumber(), request.getCardOwner(),
            request.getChecksum());
        mockServer.verify();
    }

    @Test
    public void testGetSingleProduct() throws Exception {

        mockServer.expect(ExpectedCount.once(), requestTo(JSONs.inventoryUrl + "/" + JSONs.productId))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(inventoryResponse(), MediaType.APPLICATION_JSON));

        // execute
        service.getSingleProduct(JSONs.productId);
        mockServer.verify();
    }

    @Test
    public void testGetCart() {

        mockServer.expect(ExpectedCount.once(), requestTo(JSONs.cartUrl + "/" + JSONs.sessionId))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        // execute
        service.getCartContent(JSONs.sessionId);
        mockServer.verify();
    }

    @Test
    public void testMakeReservation() throws Exception {
        ReservationRequest request = new ReservationRequest(productId, sessionId, 2);

        mockServer.expect(ExpectedCount.once(), requestTo(reservationUrl)).andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(mapper.writeValueAsString(request)))
            .andRespond(withSuccess(inventoryResponse(), MediaType.APPLICATION_JSON));

        // execute
        service.makeReservations(sessionId, productId, 2);
        mockServer.verify();
    }

    @Test
    public void testGetAllProducts() {

        // twice = once for products and once for page
        mockServer.expect(ExpectedCount.twice(), requestTo(inventoryUrl)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(inventoryResponseAllProducts(), MediaType.APPLICATION_JSON));

        // execute
        service.getAllProducts();
        mockServer.verify();
    }

    @Test
    public void testAddItemToCart() throws Exception {
        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + "/" + sessionId)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + "/" + sessionId)).andExpect(method(HttpMethod.PUT))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(updatedCartResponse())).andRespond(withStatus(HttpStatus.OK));

        // execute
        service.addItemToCart(sessionId, productId, 1);
        mockServer.verify();

    }

    @Test
    public void testDeleteItemFromCart() throws Exception {
        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + "/" + sessionId)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + "/" + sessionId)).andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(HttpStatus.OK));

        // execute
        service.deleteItemFromCart(sessionId, productId, 1);
        mockServer.verify();
    }

    @Test
    public void testGetProductsInCart() {
        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + "/" + sessionId)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(cartResponseMulti(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(inventoryUrl + "/" + productId)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(inventoryResponse(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(inventoryUrl + "/" + anotherproductId))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(anotherInventoryResponse(), MediaType.APPLICATION_JSON));

        service.getProductsInCart(sessionId);
        mockServer.verify();
    }
}
