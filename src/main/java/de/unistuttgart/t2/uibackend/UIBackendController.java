package de.unistuttgart.t2.uibackend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.unistuttgart.t2.common.OrderRequest;
import de.unistuttgart.t2.common.Product;
import de.unistuttgart.t2.common.UpdateCartRequest;
import de.unistuttgart.t2.uibackend.exceptions.CartInteractionFailedException;
import de.unistuttgart.t2.uibackend.exceptions.OrderNotPlacedException;
import de.unistuttgart.t2.uibackend.exceptions.ReservationFailedException;

/**
 * Defines the http enpoints of the UIBackend.
 * 
 * @author maumau
 *
 */
@RestController
public class UIBackendController {

    private UIBackendService service;

    public UIBackendController(@Autowired UIBackendService service) {
        this.service = service;
    }
    
    /**
     * Get a list of all products in the inventory.
     * 
     * The session exists such that i can get a cookie even though i am not using
     * the ui (frontend), e.g. as the load generator does.
     * 
     * @param session http session
     * @return a list of all product in the inventory.
     */
    @GetMapping("/products")
    public List<Product> getAllProducts() {
        return service.getAllProducts();
    }

    /**
     * Add units of the given products to the cart.
     * 
     * <p>
     * Only add the products to the cart if the requested number of unit is
     * available. To achieve this, at first a reservations are placed in the
     * inventory and only after the reservations are succeeded be are the products
     * added to the cart.
     * 
     * <p>
     * Replies as long as at least on product is added to the cart.
     * 
     * @param sessionId sessionId of user
     * @param products  products to be added, including the number of units thereof
     * @return a list of all products that were added with {@code units} being the
     *         number of unit that were added / reserved.
     * @throws ReservationFailedException if all reservations failed.
     */
    @PostMapping("/cart/{sessionId}")
    public List<Product> updateCart(@PathVariable String sessionId, @RequestBody UpdateCartRequest updateCartRequest)
            throws ReservationFailedException, CartInteractionFailedException {
        List<Product> successfullyAddedProducts = new ArrayList<>();

        for (Entry<String, Integer> product : updateCartRequest.getContent().entrySet()) {
            if (product.getValue() == 0) {
                continue;
            }
            if (product.getValue() > 0) {
                try {
                    // contact inventory first, cause i'd rather have a dangling reservation than a
                    // products in the cart that are not backed with reservations.
                    Product addedProduct = service.makeReservations(sessionId, product.getKey(),
                            product.getValue());
                    service.addItemToCart(sessionId, product.getKey(), product.getValue());
                    successfullyAddedProducts.add(addedProduct);

                } catch (ReservationFailedException e) {
                    e.printStackTrace();
                }
            } else { //product.getValue() < 0
                try {
                    service.deleteItemFromCart(sessionId, product.getKey(), product.getValue());
                } catch (Exception e) {
                    e.printStackTrace();
                }                
            }
            
        }
        return successfullyAddedProducts;
    }


    /**
     * Get a list of all products in users cart.
     * 
     * @param sessionId sessionId of user
     * @return a list of all products in the users cart.
     */
    @GetMapping("/cart/{sessionId}")
    public List<Product> getCart(@PathVariable String sessionId) {
        return service.getProductsInCart(sessionId);
    }

    /**
     * place an order, i.e. start a transaction.
     * 
     * upon successfully placing the order the cart is cleared and the session gets
     * invalidated. if the user wants to place another order he needs a new http
     * session.
     * 
     * @param request request to place an Order
     * @throws OrderNotPlacedException if the order could not be placed.
     */
    @PostMapping("/confirm")
    public void confirmOrder(@RequestBody OrderRequest request)
            throws OrderNotPlacedException, CartInteractionFailedException {
        service.confirmOrder(request.getSessionId(), request.getCardNumber(), request.getCardOwner(),
                request.getChecksum());
        service.deleteCart(request.getSessionId());
    }

    /**
     * Creates the response entity if a request could not be served because placing
     * an order failed.
     * 
     * @param exception
     * @return a response entity with an exceptional message
     */
    @ExceptionHandler(OrderNotPlacedException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<String> handleOrderNotPlacesException(OrderNotPlacedException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
    }

    /**
     * Creates the response entity if a request could not be served because of a
     * failed reservation.
     * 
     * @param exception
     * @return a response entity with an exceptional message
     */
    @ExceptionHandler(ReservationFailedException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<String> handleReservationFailedException(ReservationFailedException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
    }

    /**
     * Creates the response entity if a request could not be served because the
     * interaction with the cart service failed.
     * 
     * @param exception
     * @return a response entity with an exceptional message
     */
    @ExceptionHandler(CartInteractionFailedException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<String> handleCartInteractionFailedException(CartInteractionFailedException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
    }
}