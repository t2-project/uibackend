package de.unistuttgart.t2.uibackend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import de.unistuttgart.t2.common.domain.CartContent;
import de.unistuttgart.t2.common.domain.PaymentInfo;
import de.unistuttgart.t2.common.domain.Product;
import de.unistuttgart.t2.uibackend.restdetails.InventoryResponseErrorHandler;

public class UIBackendService {

	private final Logger LOG = LoggerFactory.getLogger(getClass());

	@Value("${t2.orchestrator.url}")
	private String orchestratorUrl;
	@Value("${t2.cart.url}")
	private String cartUrl;
	@Value("${t2.inventory.url}")
	private String inventoryUrl;

	@PostConstruct
	public void init() {
		if (orchestratorUrl.isEmpty() || cartUrl.isEmpty() || inventoryUrl.isEmpty()) {
			throw new IllegalStateException("Missing URLS");
		}
	}

	/**
	 * Get all products available at the store.
	 * 
	 * @return a list of all products available
	 */
	public List<Product> getAllProducts() {
		// request to inventory service
		List<Product> rval = new ArrayList<Product>();

		String ressourceUrl = inventoryUrl + "/get";
		LOG.info("query to " + ressourceUrl);

		RequestEntity<Void> request = RequestEntity.get(ressourceUrl).build();
		ResponseEntity<List<Product>> response = (new RestTemplate()).exchange(request,
				new ParameterizedTypeReference<List<Product>>() {
				});

		rval = response.getBody();
		return rval;
	}

	/**
	 * Add units of product to a users shopping cart.
	 * 
	 * If product is already present in the cart, the units of that product will be
	 * updated.
	 * 
	 * @param sessionId for identification
	 * @param productId id of product to be added
	 * @param units     number of units to be added
	 */
	public void addItemToCart(String sessionId, String productId, Integer units) {
		// TODO make this work with a request body. currently the deserialization does
		// not work.
		String ressourceUrl = cartUrl + "/add/" + sessionId + "/" + productId + "/" + units;

		LOG.info("put to " + ressourceUrl);

		(new RestTemplate()).put(ressourceUrl, null);
	}

	/**
	 * Delete units of product from a users shopping cart.
	 * 
	 * If units of product decrease to zero or less, remove product from cart.
	 * 
	 * @param sessionId for identification
	 * @param productId id of product to be deleted
	 * @param units     number of units to be deleted
	 */
	public void deleteItemFromCart(String sessionID, String productId, Integer units) {
		// request to cart service
		// TODO
	}

	/**
	 * Get all products in a users cart.
	 * 
	 * @param sessionId for identification
	 * @return list of all product in the cart
	 */
	public List<Product> getCart(String sessionId) {
		List<Product> rval = new ArrayList<Product>();

		// request cart
		String ressourceUrl = cartUrl + "/get/" + sessionId;
		LOG.info("get from " + ressourceUrl);

		CartContent cartContent = (new RestTemplate()).getForEntity(ressourceUrl, CartContent.class).getBody();

		//call inventory with special template to handle 404 personally
		RestTemplate template = new RestTemplate();
		template.setErrorHandler(new InventoryResponseErrorHandler());
		
		for (String productId : cartContent.getProducts()) {
			ressourceUrl = inventoryUrl + "/get/" + productId;
			LOG.info("get from " + ressourceUrl);

			ResponseEntity<Product> inventoryResponse = template.getForEntity(ressourceUrl, Product.class);
			
			if (inventoryResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
				continue;
			}
			
			Product product = inventoryResponse.getBody();
			rval.add(new Product(productId, product.getName(), cartContent.getUnits(productId), product.getPrice()));
		}

		return rval;
	}

	/**
	 * start the saga to process the order.
	 * 
	 * @param sessionId   for identification
	 * @param paymentInfo
	 */
	public void confirmOrder(String sessionId) {
		String ressourceUrl = orchestratorUrl + "/order/" + sessionId;
		LOG.info("post to " + ressourceUrl);

		ResponseEntity<Void> response = (new RestTemplate()).postForEntity(ressourceUrl, "", Void.class);
		
		LOG.info(response.getStatusCode().toString() + " - orchestrator accepted request :)");
	}
	
	/**
	 * UHM... not sure whether i'll really use this??
	 * 
	 * @param sessionId
	 * @param paymentInfo
	 */
	public void putPayment(String sessionId, PaymentInfo paymentInfo) {
		
	}

}
