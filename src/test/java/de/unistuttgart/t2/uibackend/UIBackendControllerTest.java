package de.unistuttgart.t2.uibackend;

import de.unistuttgart.t2.common.Product;
import de.unistuttgart.t2.common.UpdateCartRequest;
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

import java.util.List;
import java.util.Map;

import static de.unistuttgart.t2.uibackend.supplicants.JSONs.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
            .andRespond(withSuccess(inventoryResponseAllProducts(), MediaType.APPLICATION_JSON));

        List<Product> actual = controller.getAllProducts();

        assertEquals(2, actual.size());
    }

    @Test
    public void testDontChangeCart() {

        mockServer.expect(ExpectedCount.never(), requestTo(cartUrl + "/" + sessionId));

        UpdateCartRequest request = new UpdateCartRequest(Map.of(productId, 0));
        controller.updateCart(sessionId, request);
    }

    @Test
    public void testAddToCart() {

        mockServer.expect(ExpectedCount.once(), requestTo(reservationUrl)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(inventoryResponse(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.twice(), requestTo(cartUrl + "/" + sessionId)).andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        int unitsToAdd = 3;
        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + "/" + sessionId)).andExpect(method(HttpMethod.PUT))
            .andExpect(jsonPath("$.content." + productId).value(unitsToAdd))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        UpdateCartRequest request = new UpdateCartRequest(Map.of(productId, unitsToAdd));
        List<Product> addedProducts = controller.updateCart(sessionId, request);

        assertEquals(1, addedProducts.size());
        assertEquals(unitsToAdd, addedProducts.get(0).getUnits());
    }

    @Test
    public void testIncreaseCart() {

        mockServer.expect(ExpectedCount.once(), requestTo(reservationUrl)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(inventoryResponse(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + "/" + sessionId)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + "/" + sessionId)).andExpect(method(HttpMethod.PUT))
            .andExpect(jsonPath("$.content." + productId).value(units * 2))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        UpdateCartRequest request = new UpdateCartRequest(Map.of(productId, units));
        List<Product> addedProducts = controller.updateCart(sessionId, request);

        assertEquals(1, addedProducts.size());
        assertEquals(units, addedProducts.get(0).getUnits());
    }

    @Test
    public void testDecreaseCart() {

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + "/" + sessionId)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + "/" + sessionId)).andExpect(method(HttpMethod.PUT))
            .andExpect(jsonPath("$.content." + productId).value(units - 1))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        UpdateCartRequest request = new UpdateCartRequest(Map.of(productId, -1));
        List<Product> addedProducts = controller.updateCart(sessionId, request);

        assertEquals(0, addedProducts.size());
    }

    @Test
    public void testRemoveFromCart() {

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + "/" + sessionId)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        mockServer.expect(ExpectedCount.once(), requestTo(cartUrl + "/" + sessionId)).andExpect(method(HttpMethod.PUT))
            .andExpect(jsonPath("$.content." + productId).doesNotExist())
            .andRespond(withSuccess(cartResponse(), MediaType.APPLICATION_JSON));

        UpdateCartRequest request = new UpdateCartRequest(Map.of(productId, -units));
        List<Product> addedProducts = controller.updateCart(sessionId, request);

        assertEquals(0, addedProducts.size());
    }
}
