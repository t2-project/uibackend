package de.unistuttgart.t2.uibackend.exceptions;

/**
 * Indicates that the placement of an order failed.
 *
 * @author maumau
 */
public final class OrderNotPlacedException extends Exception {

    private static final long serialVersionUID = 1L;

    public OrderNotPlacedException(String message) {
        super(message);
    }
}
