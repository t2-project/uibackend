package de.unistuttgart.t2.uibackend.exceptions;

/**
 * Indicates that interaction with the cart service failed.
 *
 * @author maumau
 */
public final class CartInteractionFailedException extends Exception {

    private static final long serialVersionUID = 1L;

    public CartInteractionFailedException(String message) {
        super(message);
    }
}
