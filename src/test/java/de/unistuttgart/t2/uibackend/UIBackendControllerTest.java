package de.unistuttgart.t2.uibackend;

import static de.unistuttgart.t2.uibackend.supplicants.JSONs.inventoryUrl;
import static de.unistuttgart.t2.uibackend.supplicants.JSONs.inventoryresponseAllProducts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.util.List;
import java.util.Map;

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

import de.unistuttgart.t2.common.Product;
import de.unistuttgart.t2.common.UpdateCartRequest;
import de.unistuttgart.t2.uibackend.exceptions.CartInteractionFailedException;
import de.unistuttgart.t2.uibackend.exceptions.ReservationFailedException;
import de.unistuttgart.t2.uibackend.supplicants.TestContext;

import static de.unistuttgart.t2.uibackend.supplicants.JSONs.*;

/**
 * Test the logic in the {@link UIBackendController}.
 * 
 * @author maumau
 */
@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(TestContext.class)
@ActiveProfiles("test")
public class UIBackendControllerTest {

    @Autowired
    UIBackendService service;

    @Autowired
    private RestTemplate template;

    private MockRestServiceServer mockServer;

    UIBackendController controller;

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(template);
        controller = new UIBackendController(service);
    }

    @Test
    public void test() {
        mockServer.expect(ExpectedCount.twice(), requestTo(inventoryUrl)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(inventoryresponseAllProducts(), MediaType.APPLICATION_JSON));

        List<Product> actual = controller.getAllProducts();

        assertEquals(2, actual.size());
    }

    @Test
    public void testDontChangeCart() throws ReservationFailedException, CartInteractionFailedException {

        mockServer.expect(ExpectedCount.never(), requestTo(cartUrl + sessionId));

        UpdateCartRequest request = new UpdateCartRequest(Map.of(productId, 0));
        controller.updateCart(sessionId, request);
    }

    @Test
    public void testAddToCart() throws ReservationFailedException, CartInteractionFailedException {

        mockServer.expect(ExpectedCount.once(), requestTo(reservationUrl)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.twice(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.PUT))
            .andExpect(jsonPath("$.content." + productId).value(units))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        UpdateCartRequest request = new UpdateCartRequest(Map.of(productId, units));
        controller.updateCart(sessionId, request);
    }

    @Test
    public void testIncreaseCart() throws ReservationFailedException, CartInteractionFailedException {

        mockServer.expect(ExpectedCount.once(), requestTo(reservationUrl)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.PUT))
            .andExpect(jsonPath("$.content." + productId).value(units * 2))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        UpdateCartRequest request = new UpdateCartRequest(Map.of(productId, units));
        controller.updateCart(sessionId, request);
    }

    @Test
    public void testDecreaseCart() throws ReservationFailedException, CartInteractionFailedException {

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.PUT))
            .andExpect(jsonPath("$.content." + productId).value(units - 1))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        UpdateCartRequest request = new UpdateCartRequest(Map.of(productId, -1));
        controller.updateCart(sessionId, request);

    }

    @Test
    public void testRemoveFromCart() throws ReservationFailedException, CartInteractionFailedException {

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + sessionId)).andExpect(method(HttpMethod.PUT))
            .andExpect(jsonPath("$.content." + productId).doesNotExist())
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        UpdateCartRequest request = new UpdateCartRequest(Map.of(productId, -units));
        controller.updateCart(sessionId, request);
    }
}
