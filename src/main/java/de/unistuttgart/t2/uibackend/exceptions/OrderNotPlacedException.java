package de.unistuttgart.t2.uibackend.exceptions;

/**
 * Indicates that the placement of an order failed.
 * 
 * @author maumau
 *
 */
public class OrderNotPlacedException extends Exception {

    public OrderNotPlacedException(String message) {
        super(message);
    }
}
