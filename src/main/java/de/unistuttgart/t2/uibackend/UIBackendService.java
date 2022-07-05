package de.unistuttgart.t2.uibackend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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

    // they have all the trailing '/' and stuff.
    private String orchestratorUrl;
    private String cartUrl;
    private String inventoryUrl;

    private String reservationEndpoint;

    // retry stuff
    RetryConfig config = RetryConfig.custom().maxAttempts(2).build();
    RetryRegistry registry = RetryRegistry.of(config);
    Retry retry = registry.retry("uibackendRetry");

    // because i moved the @value stuff to the configuration thing-y
    public UIBackendService(String cartUrl, String inventoryUrl, String orchestratorUrl, String reservationEndpoint) {
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

    /**
     * Get a list of all products from the inventory.
     * <p>
     * TODO : the generated endpoints do things with pages. this gets the first twenty items only.
     * 
     * @return a list of all products in the inventory. (might be incomplete)
     */
    public List<Product> getAllProducts() {
        List<Product> rval = new ArrayList<>();

        LOG.debug("get from " + inventoryUrl);

        try {

            // first page
            rval.addAll(getSomeProducts(inventoryUrl));

            // additional pages
            ResponseEntity<String> response = Retry
                .decorateSupplier(retry, () -> template.getForEntity(inventoryUrl, String.class)).get();

            JsonNode root = mapper.readTree(response.getBody());

            while (hasNext(root)) {
                String url = getNext(root);
                rval.addAll(getSomeProducts(url));

                root = mapper.readTree(
                    Retry.decorateSupplier(retry, () -> template.getForEntity(url, String.class)).get().getBody());

            }

        } catch (RestClientException e) { // 404 or something like that.
            e.printStackTrace();
        } catch (JsonProcessingException e) { // malformed JSON, or whatever :x
            e.printStackTrace();
        }
        return rval;
    }

    /**
     * Check whether there is another page after this in the inventory
     * 
     * @param root inventory page as json node
     * @return true iff there is a next page
     */
    private boolean hasNext(JsonNode root) {
        JsonNode next = root.findPath("next");
        if (next.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Get the url to the next inventory page
     * 
     * @param root json node that contains url too next page
     * @return url to next page
     */
    private String getNext(JsonNode root) {
        JsonNode next = root.findPath("next").findPath("href");
        return next.asText();
    }

    private List<Product> getSomeProducts(String url) {
        List<Product> rval = new ArrayList<>();
        ResponseEntity<String> response = Retry.decorateSupplier(retry, () -> template.getForEntity(url, String.class))
            .get();

        try {
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode inventory = root.findPath("inventory");

            for (JsonNode node : inventory) {
                try {
                    Product p = mapper.treeToValue(node, Product.class);
                    p.setId(getIdfromJson(node));
                    rval.add(p);
                } catch (JsonProcessingException e) {
                    e.printStackTrace(); // single malformed product, continue with next one
                }
            }
        } catch (JsonProcessingException e1) {
            e1.printStackTrace();
        }

        return rval;
    }

    /**
     * Add the given number units of product to a users cart.
     * <p>
     * If the product is already in the cart, the units of that product will be updated.
     * 
     * @param sessionId identifies the cart to add to
     * @param productId id of product to be added
     * @param units     number of units to be added
     * @throws CartInteractionFailedException if the request number of unit could not be placed in the cart.
     */
    public void addItemToCart(String sessionId, String productId, Integer units) throws CartInteractionFailedException {
        String ressourceUrl = cartUrl + sessionId;
        LOG.debug("put to " + ressourceUrl);

        Optional<CartContent> optCartContent = getCartContent(sessionId);

        try {
            if (optCartContent.isPresent()) {
                CartContent cartContent = optCartContent.get();
                cartContent.getContent().put(productId, units + cartContent.getUnits(productId));
                template.put(ressourceUrl, cartContent);

            } else {
                template.put(ressourceUrl, new CartContent(Map.of(productId, units)));
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
     * @param units     number of units to be deleted
     * @throws CartInteractionFailedException if anything went wrong while talking to the cart
     */
    public void deleteItemFromCart(String sessionId, String productId, Integer units)
        throws CartInteractionFailedException {
        String ressourceUrl = cartUrl + sessionId;
        LOG.debug("put to " + ressourceUrl);

        Optional<CartContent> optCartContent = getCartContent(sessionId);

        if (optCartContent.isPresent()) {
            try {
                CartContent cartContent = optCartContent.get();
                Integer remainingUnitsInCart = cartContent.getUnits(productId) + units;
                if (remainingUnitsInCart > 0) {
                    cartContent.getContent().put(productId, remainingUnitsInCart);
                } else {
                    cartContent.getContent().remove(productId);
                }
                Retry.decorateRunnable(retry, () -> template.put(ressourceUrl, cartContent)).run();
            } catch (RestClientException e) {
                LOG.info(e.getMessage());
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
        String ressourceUrl = cartUrl + sessionId;
        LOG.debug("delete to " + ressourceUrl);

        try {
            template.delete(ressourceUrl);
        } catch (RestClientException e) { // 404 or something like that.
            LOG.info(e.getMessage());
        }
    }

    /**
     * Get a list of all products in a users cart.
     * 
     * @param sessionId identfies the cart content to get
     * @return a list of the product in the cart
     */
    public List<Product> getProductsInCart(String sessionId) {
        List<Product> rval = new ArrayList<Product>();

        Optional<CartContent> optCartContent = getCartContent(sessionId);

        if (optCartContent.isPresent()) {
            CartContent cartContent = optCartContent.get();

            for (String productId : cartContent.getProductIds()) {
                getSingleProduct(productId).ifPresent((p) -> {
                    p.setUnits(cartContent.getUnits(productId));
                    rval.add(p);
                });
            }
        }

        return rval;
    }

    /**
     * Reserve a the given number of units of the given product.
     * 
     * @param sessionId identifies the session to reserve for
     * @param productId identifies the product to reserve of
     * @param units     number of units to reserve
     * @return the product the reservation was made for
     * @throws ReservationFailedException if the reservation could not be placed
     */
    public Product makeReservations(String sessionId, String productId, Integer units)
        throws ReservationFailedException {

        String ressourceUrl = inventoryUrl + reservationEndpoint;
        LOG.debug("post to " + ressourceUrl);

        try {
            ReservationRequest request = new ReservationRequest(productId, sessionId, units);

            ResponseEntity<Product> inventoryResponse = Retry
                .decorateSupplier(retry, () -> template.postForEntity(ressourceUrl, request, Product.class)).get();

            return inventoryResponse.getBody();
        } catch (RestClientException e) { // no reservation for what ever reason
            LOG.info(e.getMessage());
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
     */
    public void confirmOrder(String sessionId, String cardNumber, String cardOwner, String checksum)
        throws OrderNotPlacedException {
        // is it more reasonable to get total from cart service, or is it more
        // reasonable to pass the total from the front end (where it was displayed and
        // therefore is known) ??
        double total = getTotal(sessionId);

        if (total <= 0) {
            throw new OrderNotPlacedException(String
                .format("No Order placed for session %s. Cart is either empty or not available. ", sessionId));
        }

        SagaRequest request = new SagaRequest(sessionId, cardNumber, cardOwner, checksum, total);

        try {
            ResponseEntity<Void> response = Retry
                .decorateSupplier(retry, () -> template.postForEntity(orchestratorUrl, request, Void.class)).get();

            LOG.info(String.format("orchestrator accepted request for session %s with status code %s ", sessionId,
                response.getStatusCode().toString()));

            deleteCart(sessionId);
            LOG.info("deleted cart for session " + sessionId);

        } catch (RestClientException e) {
            LOG.info(String.format("Failed to contact orchestrator for session %s : %s", sessionId, e.getMessage()));
            throw new OrderNotPlacedException(
                String.format("No Order placed for session %s. Orchestrator not available. ", sessionId));
        } catch (CartInteractionFailedException e) {
            LOG.info(String.format("Failed to delete cart for session %s", sessionId));
        }
    }

    /**
     * Get the content of the cart belonging to the given sessionId.
     * <p>
     * If there is either no cart content for the given sessionId, or the retrieval of the content failed, an empty
     * optional is returned.
     * 
     * @param sessionId
     * @return content of cart iff it exists
     */
    protected Optional<CartContent> getCartContent(String sessionId) {
        String ressourceUrl = cartUrl + sessionId;
        LOG.debug("get from " + ressourceUrl);

        try {
            ResponseEntity<String> response = Retry
                .decorateFunction(retry, (String url) -> template.getForEntity(url, String.class))
                .apply(ressourceUrl);

            JsonNode root = mapper.readTree(response.getBody());
            JsonNode name = root.path("content");

            return Optional.of(mapper.treeToValue(name, CartContent.class));
        } catch (RestClientException e) { // 404 or something like that.
            LOG.info(String.format("not yet any cart content for %s : %s  ", sessionId, e.getMessage()));
        } catch (JsonProcessingException e) { // whatever we received, it was no cart content.
            LOG.info(e.getMessage());
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
        String ressourceUrl = inventoryUrl + productId;
        LOG.debug("get from " + ressourceUrl);

        try {
            ResponseEntity<String> response = Retry
                .decorateFunction(retry, (String url) -> template.getForEntity(url, String.class))
                .apply(ressourceUrl);

            // important, because inventory api may (did) return more fields than we need.
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            Product product = mapper.readValue(response.getBody(), Product.class);
            product.setId(productId);

            return Optional.of(product);
        } catch (RestClientException e) { // 404 or something like that.
            LOG.debug(String.format("get for %s failed: %s %s", productId, e.getClass().toGenericString(),
                e.getMessage()));
        } catch (JsonProcessingException e) { // whatever we received, it was no product.
            LOG.debug(String.format("get for %s failed: %s %s", productId, e.getClass().toGenericString(),
                e.getMessage()));
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
        String id = s.split("/")[s.split("/").length - 1];
        return id;
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
}
