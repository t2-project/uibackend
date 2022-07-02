package de.unistuttgart.t2.uibackend.exceptions;

/**
 * Indicates that interaction with the cart service failed.
 * 
 * @author maumau
 */
public class CartInteractionFailedException extends Exception {

    public CartInteractionFailedException(String message) {
        super(message);
    }
}
