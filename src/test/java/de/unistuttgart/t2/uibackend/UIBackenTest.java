package de.unistuttgart.t2.uibackend;


import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import de.unistuttgart.t2.common.domain.CartContent;
import de.unistuttgart.t2.common.domain.Product;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestContext.class)
@RunWith(MockitoJUnitRunner.class)
/**
 * Test UIBackendservice for responses. 
 * 
 * TODO : all special cases!
 * 
 * @author maumau
 *
 */
public class UIBackenTest {

    @Mock
    private RestTemplate template;

    @InjectMocks
    private UIBackendService service = new UIBackendService("http://localhost:8080/cart", "http://localhost:8082/inventory", null);
        
    @Test
    public void getCartContentTest() {
    	ResponseEntity<String> entity = new ResponseEntity(JSONs.cartResponse, new HttpHeaders(), HttpStatus.OK);
  
        Mockito
          .when((template.getForEntity(
            JSONs.cartUrl+JSONs.sessionId, String.class)))
          .thenReturn(entity);

        CartContent products = service.getCartContent(JSONs.sessionId).get();
        
        assertNotNull(products);
        assertNotNull(products.getContent());
        assertFalse(products.getContent().isEmpty());
        assertTrue(products.getContent().containsKey(JSONs.productId));
        assertEquals(42, products.getContent().get(JSONs.productId));
    }
    
    @Test
    public void getCartTest() {
    	
    	//setup cart responses
    	ResponseEntity<String> cartEntity = new ResponseEntity(JSONs.cartResponse, new HttpHeaders(), HttpStatus.OK);
        Mockito
          .when((template.getForEntity(
            JSONs.cartUrl+JSONs.sessionId, String.class)))
          .thenReturn(cartEntity);
        
        //setup inventory response
    	ResponseEntity<String> inventoryEntity = new ResponseEntity(JSONs.inventoryResponse, HttpStatus.OK);
        Mockito
          .when((template.getForEntity(
        		  JSONs.inventoryUrl+JSONs.productId, String.class)))
          .thenReturn(inventoryEntity);
        
        // execute 
        List<Product> products = service.getProductsInCart(JSONs.sessionId);

        // assert
        assertNotNull(products);
        assertFalse(products.isEmpty());
        //TODO assert content
    }
    
    @Test
    public void getSingleProductTest() {
    	//setup inventory responses
    	ResponseEntity<String> entity = new ResponseEntity(JSONs.inventoryResponse, HttpStatus.OK);
        Mockito
          .when((template.getForEntity(
        		  JSONs.inventoryUrl+JSONs.productId, String.class)))
          .thenReturn(entity);

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
    	//setup inventory responses
    	ResponseEntity<String> entity = new ResponseEntity(JSONs.inventoryresponsefoo, HttpStatus.OK);
        Mockito
          .when((template.getForEntity(JSONs.inventoryUrl, String.class))) // no id, we want ALL.
          .thenReturn(entity);

        // execute
        List<Product> products = service.getAllProducts();
        
        // assert
        assertNotNull(products);
        assertFalse(products.isEmpty());
        // TODO : assert that content is actually correct :x
    }
    

}
