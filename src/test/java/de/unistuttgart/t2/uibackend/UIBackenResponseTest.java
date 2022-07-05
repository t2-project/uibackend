package de.unistuttgart.t2.uibackend;

import static de.unistuttgart.t2.uibackend.supplicants.JSONs.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import de.unistuttgart.t2.common.CartContent;
import de.unistuttgart.t2.common.Product;
import de.unistuttgart.t2.uibackend.supplicants.JSONs;

/**
 * Test whether UIBackendservice handles all responses correctly.
 * <p>
 * The Set up is like this:
 * <ul>
 * <li>Call the operation under test.
 * <li>Mock the responses that the operation would receive from other services.
 * <li>Assert that the operation under test processes the replies as intended.
 * </ul>
 * 
 * @author maumau
 */
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
public class UIBackenResponseTest {

    @Mock // mock the rest template
    private RestTemplate template;

    @InjectMocks // inject the mocked rest template into service
    private UIBackendService service = new UIBackendService(JSONs.cartUrl, JSONs.inventoryUrl, JSONs.orchestratorUrl,
        JSONs.reservationEndpoint);

    @Test
    public void getCartContentTest() {
        ResponseEntity<String> entity = new ResponseEntity(JSONs.cartResponse(), new HttpHeaders(), HttpStatus.OK);

        Mockito.when((template.getForEntity(JSONs.cartUrl + JSONs.sessionId, String.class))).thenReturn(entity);

        CartContent products = service.getCartContent(JSONs.sessionId).get();

        assertNotNull(products);
        assertNotNull(products.getContent());
        assertFalse(products.getContent().isEmpty());
        assertTrue(products.getContent().containsKey(JSONs.productId));
        assertEquals(42, products.getContent().get(JSONs.productId));
    }

    @Test
    public void getProductsInCartTest() {
        // setup cart responses
        ResponseEntity<String> cartEntity = new ResponseEntity(JSONs.cartResponse(), new HttpHeaders(), HttpStatus.OK);
        Mockito.when((template.getForEntity(JSONs.cartUrl + JSONs.sessionId, String.class))).thenReturn(cartEntity);

        // setup inventory response
        ResponseEntity<String> inventoryEntity = new ResponseEntity(JSONs.inventoryResponse(), HttpStatus.OK);
        Mockito.when((template.getForEntity(JSONs.inventoryUrl + JSONs.productId, String.class)))
            .thenReturn(inventoryEntity);

        // execute
        List<Product> products = service.getProductsInCart(JSONs.sessionId);

        // assert
        assertNotNull(products);
        assertEquals(1, products.size());
        // TODO assert content
        // once again: had i only implemented this earlier :x
        assertEquals(productId, products.get(0).getId());
        assertEquals(units, products.get(0).getUnits());
    }

    @Test
    public void getSingleProductTest() {
        // setup inventory responses
        ResponseEntity<String> entity = new ResponseEntity(inventoryResponse(), HttpStatus.OK);
        Mockito.when((template.getForEntity(JSONs.inventoryUrl + JSONs.productId, String.class))).thenReturn(entity);

        // execute
        Product product = service.getSingleProduct(JSONs.productId).get();

        // assert
        assertNotNull(product);
        assertEquals(JSONs.productId, product.getId());
        assertEquals("name", product.getName());
        assertEquals("description", product.getDescription());
        assertEquals(5, product.getUnits());
        assertEquals(1.0, product.getPrice());
    }

    @Test
    public void getAllProductsTest() {
        // setup inventory responses
        ResponseEntity<String> entity = new ResponseEntity(inventoryresponseAllProducts(), HttpStatus.OK);
        Mockito.when((template.getForEntity(JSONs.inventoryUrl, String.class))) // no id, we want ALL.
            .thenReturn(entity);

        // execute
        List<Product> products = service.getAllProducts();

        // assert
        assertNotNull(products);
        assertFalse(products.isEmpty());
        assertEquals(productId, products.get(0).getId());
        assertEquals(anotherproductId, products.get(1).getId());
    }
}
