package de.unistuttgart.t2.uibackend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.t2.common.CartContent;
import de.unistuttgart.t2.common.Product;
import de.unistuttgart.t2.common.ReservationRequest;
import de.unistuttgart.t2.common.SagaRequest;
import de.unistuttgart.t2.uibackend.exceptions.CartInteractionFailedException;
import de.unistuttgart.t2.uibackend.exceptions.OrderNotPlacedException;
import de.unistuttgart.t2.uibackend.exceptions.ReservationFailedException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages interaction with other services.
 *
 * @author maumau
 */
public class UIBackendService {

    @Autowired
    RestTemplate template;

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        false);

    // URLs don't have a trailing slash
    private String orchestratorUrl;
    private String cartUrl;
    private String inventoryUrl;
    private String reservationEndpoint;

    // simulate compute intensive task
    private final String computationSimulatorUrl;
    private final boolean simulateComputeIntensiveTask;

    // retry stuff
    RetryConfig config = RetryConfig.custom().maxAttempts(2).build();
    RetryRegistry registry = RetryRegistry.of(config);
    Retry retry = registry.retry("uibackendRetry");

    private void initialize(String cartUrl, String inventoryUrl, String orchestratorUrl, String reservationEndpoint) {
        if (cartUrl == null || inventoryUrl == null || orchestratorUrl == null || reservationEndpoint == null) {
            throw new IllegalArgumentException(
                String.format("urls must not be 'null' but one of these is: %s, %s, %s, %s ", cartUrl, inventoryUrl,
                    orchestratorUrl, reservationEndpoint));
        }
        this.cartUrl = cartUrl;
        this.inventoryUrl = inventoryUrl;
        this.orchestratorUrl = orchestratorUrl;
        this.reservationEndpoint = reservationEndpoint;
    }

    public UIBackendService(String cartUrl, String inventoryUrl, String orchestratorUrl, String reservationEndpoint) {
        initialize(cartUrl, inventoryUrl, orchestratorUrl, reservationEndpoint);

        this.simulateComputeIntensiveTask = false;
        this.computationSimulatorUrl = null;
    }

    public UIBackendService(String cartUrl, String inventoryUrl, String orchestratorUrl, String reservationEndpoint,
                            boolean simulateComputeIntensiveTask, String computationSimulatorUrl) {
        initialize(cartUrl, inventoryUrl, orchestratorUrl, reservationEndpoint);

        this.simulateComputeIntensiveTask = simulateComputeIntensiveTask;
        this.computationSimulatorUrl = computationSimulatorUrl;

        if (simulateComputeIntensiveTask) {
            LOG.warn("Simulate compute intensive task enabled! Service '{}' will be called when an order gets confirmed.",
                computationSimulatorUrl);
        }
    }

    /**
     * Get a list of all products from the inventory.
     * <p>
     * TODO : the generated endpoints do things with pages. this gets the first twenty items only.
     *
     * @return a list of all products in the inventory. (might be incomplete)
     */
    public List<Product> getAllProducts() {
        List<Product> result = new ArrayList<>();

        LOG.debug("get from " + inventoryUrl);

        try {

            // first page
            result.addAll(getSomeProducts(inventoryUrl));

            // additional pages
            ResponseEntity<String> response = Retry
                .decorateSupplier(retry, () -> template.getForEntity(inventoryUrl, String.class)).get();

            JsonNode root = mapper.readTree(response.getBody());

            while (hasNext(root)) {
                String url = getNext(root);
                result.addAll(getSomeProducts(url));

                root = mapper.readTree(
                    Retry.decorateSupplier(retry, () -> template.getForEntity(url, String.class)).get().getBody());

            }

        } catch (RestClientException | JsonProcessingException e) {
            LOG.error("Cannot retrieve all products", e);
        }
        return result;
    }

    /**
     * Check whether there is another page after this in the inventory
     *
     * @param root inventory page as json node
     * @return true iff there is a next page
     */
    private boolean hasNext(JsonNode root) {
        return !root.findPath("next").isEmpty();
    }

    /**
     * Get the url to the next inventory page
     *
     * @param root json node that contains url too next page
     * @return url to next page
     */
    private String getNext(JsonNode root) {
        return root.findPath("next").findPath("href").asText();
    }

    private List<Product> getSomeProducts(String url) {
        List<Product> result = new ArrayList<>();
        ResponseEntity<String> response = Retry.decorateSupplier(retry, () -> template.getForEntity(url, String.class))
            .get();

        try {
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode inventory = root.findPath("inventory");

            for (JsonNode node : inventory) {
                try {
                    Product p = mapper.treeToValue(node, Product.class);
                    p.setId(getIdfromJson(node));
                    result.add(p);
                } catch (JsonProcessingException e) {
                    LOG.error("Cannot deserialize a product received from {}. Exception: {}", url, e.getMessage(), e);
                }
            }
        } catch (JsonProcessingException e) {
            LOG.error("Cannot deserialize some products received from {}. Exception: {}", url, e.getMessage(), e);
        }

        return result;
    }

    /**
     * Add the given number units of product to a users cart.
     * <p>
     * If the product is already in the cart, the units of that product will be updated.
     *
     * @param sessionId identifies the cart to add to
     * @param productId id of product to be added
     * @param units     number of units to be added (must not be negative)
     * @throws CartInteractionFailedException if the request number of unit could not be placed in the cart.
     */
    public void addItemToCart(String sessionId, String productId, int units) throws CartInteractionFailedException {
        String resourceUrl = cartUrl + "/" + sessionId;
        LOG.debug("put to " + resourceUrl);

        if (units < 0) {
            throw new IllegalArgumentException("Value of units must not be negative.");
        }

        Optional<CartContent> optCartContent = getCartContent(sessionId);

        try {
            if (optCartContent.isPresent()) {
                CartContent cartContent = optCartContent.get();
                cartContent.getContent().put(productId, units + cartContent.getUnits(productId));
                template.put(resourceUrl, cartContent);

            } else {
                template.put(resourceUrl, new CartContent(Map.of(productId, units)));
            }
        } catch (RestClientException e) {
            throw new CartInteractionFailedException(
                String.format("Could not add %d units of product %s to cart.", units, productId));
        }
    }

    /**
     * Delete the given number units of product from a users cart.
     * <p>
     * If the number of units in the cart decrease to zero or less, the product is remove from the cart. If the no such
     * product is in cart, do nothing.
     *
     * @param sessionId identifies the cart to delete from
     * @param productId id of the product to be deleted
     * @param units     number of units to be deleted (must not be negative)
     * @throws CartInteractionFailedException if anything went wrong while talking to the cart
     */
    public void deleteItemFromCart(String sessionId, String productId, int units)
        throws CartInteractionFailedException {
        String resourceUrl = cartUrl + "/" + sessionId;
        LOG.debug("put to " + resourceUrl);

        if (units < 0) {
            throw new IllegalArgumentException("Value of units must not be negative.");
        }

        Optional<CartContent> optCartContent = getCartContent(sessionId);

        if (optCartContent.isPresent()) {
            try {
                CartContent cartContent = optCartContent.get();
                int remainingUnitsInCart = cartContent.getUnits(productId) - units;
                if (remainingUnitsInCart > 0) {
                    cartContent.getContent().put(productId, remainingUnitsInCart);
                } else {
                    cartContent.getContent().remove(productId);
                }
                Retry.decorateRunnable(retry, () -> template.put(resourceUrl, cartContent)).run();
            } catch (RestClientException e) {
                LOG.error("Cannot delete {} unit(s) of {} for {}. Exception: {}", units, productId, sessionId, e.getMessage(), e);
                throw new CartInteractionFailedException(
                    String.format("Deletion for session %s failed : %s, %d", sessionId, productId, units));
            }
        }
    }

    /**
     * Delete the entire cart for the given sessionId.
     *
     * @param sessionId identifies the cart content to delete
     * @throws CartInteractionFailedException if anything went wrong while talking to the cart
     */
    public void deleteCart(String sessionId) throws CartInteractionFailedException {
        String resourceUrl = cartUrl + "/" + sessionId;
        LOG.debug("delete to " + resourceUrl);

        try {
            template.delete(resourceUrl);
        } catch (RestClientException e) {
            LOG.error("Cannot delete cart.", e);
        }
    }

    /**
     * Get a list of all products in a users cart.
     *
     * @param sessionId identifies the cart content to get
     * @return a list of the product in the cart
     */
    public List<Product> getProductsInCart(String sessionId) {
        List<Product> results = new ArrayList<>();

        Optional<CartContent> optCartContent = getCartContent(sessionId);

        if (optCartContent.isPresent()) {
            CartContent cartContent = optCartContent.get();

            for (String productId : cartContent.getProductIds()) {
                getSingleProduct(productId).ifPresent(p -> {
                    p.setUnits(cartContent.getUnits(productId));
                    results.add(p);
                });
            }
        }

        return results;
    }

    /**
     * Reserve a given number of units of the given product.
     *
     * @param sessionId identifies the session to reserve for
     * @param productId identifies the product to reserve of
     * @param units     number of units to reserve
     * @return the product the reservation was made for
     * @throws ReservationFailedException if the reservation could not be placed
     */
    public Product makeReservations(String sessionId, String productId, int units)
        throws ReservationFailedException {

        String resourceUrl = inventoryUrl + "/" + reservationEndpoint;
        LOG.debug("post to " + resourceUrl);

        try {
            ReservationRequest request = new ReservationRequest(productId, sessionId, units);

            ResponseEntity<Product> inventoryResponse = Retry
                .decorateSupplier(retry, () -> template.postForEntity(resourceUrl, request, Product.class)).get();

            return inventoryResponse.getBody();
        } catch (RestClientException e) {
            LOG.error("Cannot reserve {} units of {} for {}. Exception: {}", units, productId, sessionId, e.getMessage(), e);
            throw new ReservationFailedException(
                String.format("Reservation for session %s failed : %s, %d", sessionId, productId, units));
        }
    }

    /**
     * Posts a request to start a transaction to the orchestrator. Attempts to delete the cart of the given sessionId
     * once the orchestrator accepted the request. Nothing happens if the deletion of a cart fails, as the cart service
     * supposed to periodically remove out dated cart entries anyway.
     *
     * @param sessionId  identifies the session
     * @param cardNumber part of payment details
     * @param cardOwner  part of payment details
     * @param checksum   part of payment details
     * @throws OrderNotPlacedException if the order to confirm is empty/ would result in a negative sum
     */
    public void confirmOrder(String sessionId, String cardNumber, String cardOwner, String checksum)
        throws OrderNotPlacedException {
        double total = getTotal(sessionId);

        if (total <= 0) {
            throw new OrderNotPlacedException(String
                .format("No Order placed for session %s. Cart is either empty or not available. ", sessionId));
        }

        SagaRequest request = new SagaRequest(sessionId, cardNumber, cardOwner, checksum, total);

        try {
            ResponseEntity<Void> response = Retry
                .decorateSupplier(retry, () -> template.postForEntity(orchestratorUrl, request, Void.class)).get();

            LOG.info("orchestrator accepted request for session {} with status code {}.", sessionId,
                response.getStatusCode());

            deleteCart(sessionId);
            LOG.info("deleted cart for session {}.", sessionId);

        } catch (RestClientException e) {
            LOG.error("Failed to contact orchestrator for session {}. Exception: {}", sessionId, e.getMessage(), e);
            throw new OrderNotPlacedException(
                String.format("No Order placed for session %s. Orchestrator not available. ", sessionId));
        } catch (CartInteractionFailedException e) {
            LOG.error("Failed to delete cart for session {}. Exception: {}", sessionId, e.getMessage(), e);
        }

        if (simulateComputeIntensiveTask) {
            simulateComputeIntensiveTask(sessionId);
        }
    }

    /**
     * Get the content of the cart belonging to the given sessionId.
     * <p>
     * If there is either no cart content for the given sessionId, or the retrieval of the content failed, an empty
     * optional is returned.
     *
     * @param sessionId the session id of the client whose cart content to retrieve
     * @return content of cart iff it exists
     */
    protected Optional<CartContent> getCartContent(String sessionId) {
        String resourceUrl = cartUrl + "/" + sessionId;
        LOG.debug("get from " + resourceUrl);

        try {
            ResponseEntity<String> response = Retry
                .decorateFunction(retry, (String url) -> template.getForEntity(url, String.class))
                .apply(resourceUrl);

            JsonNode root = mapper.readTree(response.getBody());
            JsonNode name = root.path("content");

            return Optional.of(mapper.treeToValue(name, CartContent.class));
        } catch (HttpStatusCodeException e) { // expected 404
            HttpStatusCode statusCode = e.getStatusCode();
            if (statusCode == HttpStatus.NOT_FOUND) {
                LOG.debug("Cart of {} is empty.", sessionId);
            } else {
                LOG.error("Getting cart content for {} returned unexpected status code {}. Exception: {} ",
                    sessionId, statusCode, e.getMessage(), e);
            }
        } catch (RestClientException e) { // unexpected exception
            LOG.error("Error getting cart content for {}. Exception: {} ", sessionId, e.getMessage(), e);
        } catch (JsonProcessingException e) { // whatever we received, it was no cart content.
            LOG.error("Cannot deserialize cart content. Exception: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Get the product with the given productId from the inventory.
     * <p>
     * If there is either no product with the given sessionId, or the retrieval of the product failed, an empty optional
     * is returned.
     *
     * @param productId id of the product to be retrieved
     * @return product with given id iff it exists
     */
    protected Optional<Product> getSingleProduct(String productId) {
        String resourceUrl = inventoryUrl + "/" + productId;
        LOG.debug("get from " + resourceUrl);

        try {
            ResponseEntity<String> response = Retry
                .decorateFunction(retry, (String url) -> template.getForEntity(url, String.class))
                .apply(resourceUrl);

            // important, because inventory api may (did) return more fields than we need.
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            Product product = mapper.readValue(response.getBody(), Product.class);
            product.setId(productId);

            return Optional.of(product);
        } catch (RestClientException | JsonProcessingException e) {
            LOG.error("Cannot get product {}. Exception: {}", productId, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Extracts the id under which a resource can be found from JSON.
     * <p>
     * TODO i'm pretty sure there are better ways to do this, but i didn't find them.
     *
     * @param node a json node that contains a link to a resource
     * @return the resources id
     */
    private String getIdfromJson(JsonNode node) {
        JsonNode link = node.findPath("href");
        String s = link.asText();
        final String[] parts = s.split("/");
        return parts[parts.length - 1];
    }

    /**
     * Calculates the total of a users cart.
     * <p>
     * Depends on the cart service to get the cart content and depends on the inventory service to get the price per
     * unit. If either of them fails, the returned total is 0. This is because the store cannot handle partial orders.
     * Its either ordering all items in the cart or none.
     *
     * @param sessionId identifies the session to get total for
     * @return the total money to pay for products in the cart
     */
    private double getTotal(String sessionId) {
        CartContent cart = getCartContent(sessionId).orElse(new CartContent());

        double total = 0;

        for (String productId : cart.getProductIds()) {
            Optional<Product> product = getSingleProduct(productId);
            if (product.isEmpty()) {
                return 0;
            }
            total += product.get().getPrice() * cart.getUnits(productId);
        }
        return total;
    }

    /**
     * Calls the computation-simulator service to simulate a compute intensive scenario.
     * This method is blocking and waits until the computation is finished!
     */
    private void simulateComputeIntensiveTask(String sessionId) {
        try {
            LOG.info("start computation simulation for session {} ...", sessionId);
            template.postForEntity(computationSimulatorUrl, HttpEntity.EMPTY, Void.class);
            LOG.info("finished computation simulation for session {}.", sessionId);
        } catch (RestClientException e) {
            LOG.error("Failed to contact computation-simulator for session {}. Exception: {}", sessionId, e.getMessage(), e);
        }
    }
}
