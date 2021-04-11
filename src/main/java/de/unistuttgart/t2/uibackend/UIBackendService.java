package de.unistuttgart.t2.uibackend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.t2.common.domain.CartContent;
import de.unistuttgart.t2.common.domain.Product;
import de.unistuttgart.t2.common.domain.ReservationRequest;
import de.unistuttgart.t2.common.domain.saga.SagaRequest;

/**
 * collects data from other services. 
 * 
 * @author maumau
 *
 */
public class UIBackendService {

	@Autowired
	RestTemplate template;

	private final Logger LOG = LoggerFactory.getLogger(getClass());
	
	private final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
			false); 

	// they have all the trailing '/' and stuff.
	private String orchestratorUrl;
	private String cartUrl;
	private String inventoryUrl;

	private String reservationEndpoint;

	// because i moved the @value stuff to the configuration thing-y
	public UIBackendService(String cartUrl, String inventoryUrl, String orchestratorUrl, String reservationEndpoint) {
		this.cartUrl = cartUrl;
		this.inventoryUrl = inventoryUrl;
		this.orchestratorUrl = orchestratorUrl;
		
		//TODO some validation? like, make sure all the urls are valid and stuff like that 
	}

	/**
	 * Get all products available at the store.
	 * 
	 * TODO : the generated endpoint does things with pages. currectly get the first twenty items only.
	 * 
	 * @return a list of all products available
	 */
	public List<Product> getAllProducts() {
		LOG.debug("get from " + inventoryUrl);

		List<Product> rval = new ArrayList<>();

		try {
			ResponseEntity<String> response = template.getForEntity(inventoryUrl, String.class);

			JsonNode root = mapper.readTree(response.getBody());
			JsonNode inventory = root.findPath("inventory");

			for (JsonNode node : inventory) {
				try {
					Product p = mapper.treeToValue(node, Product.class);
					p.setId(getIdfromJson(inventory));
					rval.add(p);
				} catch (JsonProcessingException e) {
					e.printStackTrace(); // single malformed product, continue with next one
				}
			}
		} catch (RestClientException e) { // 404 or something like that.
			e.printStackTrace();
		} catch (JsonProcessingException e) { // malformed JSON, or whatever :x
			e.printStackTrace();
		}
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
		String ressourceUrl = cartUrl + sessionId;
		LOG.debug("put to " + ressourceUrl);

		Optional<CartContent> optCartContent = getCartContent(sessionId);

		if (optCartContent.isPresent()) {
			CartContent cartContent = optCartContent.get();
			cartContent.getContent().put(productId, units);
			template.put(ressourceUrl, cartContent);

		} else {
			template.put(ressourceUrl, new CartContent(Map.of(productId, units)));
		}
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
	public void deleteItemFromCart(String sessionId, String productId, Integer units) {
		String ressourceUrl = cartUrl + sessionId;
		LOG.debug("put to " + ressourceUrl);

		Optional<CartContent> optCartContent = getCartContent(sessionId);

		if (optCartContent.isPresent()) {
			CartContent cartContent = optCartContent.get();
			Integer remainingUnitsInCart = cartContent.getUnits(productId) - units;
			if (remainingUnitsInCart > 0) {
				cartContent.getContent().put(productId, remainingUnitsInCart);
			} else {
				cartContent.getContent().remove(productId);
			}
			template.put(ressourceUrl, cartContent);
		}
	}

	/**
	 * Get all products in a users cart.
	 * 
	 * @param sessionId for identification
	 * @return list of all product in the cart
	 */
	public List<Product> getProductsInCart(String sessionId) {
		List<Product> rval = new ArrayList<Product>();

		Optional<CartContent> optCartContent = getCartContent(sessionId);

		if (optCartContent.isPresent()) {
			CartContent cartContent = optCartContent.get();

			for (String productId : cartContent.getProductIds()) {
				getSingleProduct(productId).ifPresent((p) -> {
					p.setId(productId);
					rval.add(p);
				});
			}
		}

		return rval;
	}

	/**
	 * start the saga to process the order.
	 * 
	 * @param sessionId   for identification
	 * @param paymentInfo
	 */
	public void confirmOrder(String cardNumber, String cardOwner, String checksum, String sessionId, double total) {
		String ressourceUrl = orchestratorUrl;
		LOG.debug("post to " + ressourceUrl);

		SagaRequest request = new SagaRequest(sessionId, cardNumber, cardOwner, checksum, total);
		
		ResponseEntity<Void> response = template.postForEntity(ressourceUrl, request, Void.class);

		LOG.info(response.getStatusCode().toString() + " - orchestrator accepted request :)");
	}

	/**
	 * query cart service for content of a sessions cart.
	 * 
	 * if request returns 404 or deserialization failed, there is no cart content.
	 * 
	 * @param sessionId
	 * @return content of cart iff it exists, empty optional otherwise
	 */
	protected Optional<CartContent> getCartContent(String sessionId) {
		String ressourceUrl = cartUrl + sessionId;
		LOG.debug("get from " + ressourceUrl);

		try {
			ResponseEntity<String> response = template.getForEntity(ressourceUrl, String.class);

			JsonNode root = mapper.readTree(response.getBody());
			JsonNode name = root.path("content");
			// TODO : do i need this acces to content??

			return Optional.of(mapper.treeToValue(name, CartContent.class));
		} catch (RestClientException e) { // 404 or something like that.
			e.printStackTrace();
		} catch (JsonProcessingException e) { // whatever we received, it was no cart content.
			e.printStackTrace();
		}
		return Optional.empty();
	}

	/**
	 * 
	 * @param productId
	 * @return
	 */
	protected Optional<Product> getSingleProduct(String productId) {
		String ressourceUrl = inventoryUrl + productId;
		LOG.debug("get from " + ressourceUrl);

		try {
			ResponseEntity<String> response = template.getForEntity(ressourceUrl, String.class);

			// important, because inventory api returns more fields than we need.
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			Product product = mapper.readValue(response.getBody(), Product.class);
			product.setId(productId);

			return Optional.of(product);
		} catch (RestClientException e) { // 404 or something like that.
			e.printStackTrace();
		} catch (JsonProcessingException e) { // whatever we received, it was no cart content.
			e.printStackTrace();
		}
		return Optional.empty();
	}

	/**
	 * extract the id under which a resource can be found.
	 * 
	 * TODO find a better was to do this. there must be one.
	 * 
	 * @param node some parent node
	 * @return the id
	 */
	private String getIdfromJson(JsonNode node) {
		JsonNode link = node.findPath("href");
		String s = link.asText();
		String id = s.split("/")[s.split("/").length - 1];
		return id;
	}

	/**
	 * reserve units of product to be put into cart.
	 * 
	 * @param sessionId
	 * @param productId
	 * @param units
	 * @throws TODO iff reservation failed
	 */
	public void makeReservations(String sessionId, String productId, Integer units) throws Exception {

		String ressourceUrl = inventoryUrl + reservationEndpoint;
		LOG.debug("get from " + ressourceUrl);

		try {
			ReservationRequest request = new ReservationRequest(productId, sessionId, units);
			ResponseEntity<Void> inventoryResponse = template.postForEntity(ressourceUrl, request, Void.class);
		} catch (RestClientException e) { // no reservation for what ever reason
			e.printStackTrace();
			throw new Exception(e); // TODO or something like that
		}
	}
}
