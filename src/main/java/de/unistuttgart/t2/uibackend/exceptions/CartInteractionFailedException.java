package de.unistuttgart.t2.uibackend.exceptions;

import java.io.Serial;

/**
 * Indicates that interaction with the cart service failed.
 *
 * @author maumau
 */
public final class CartInteractionFailedException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public CartInteractionFailedException(String message) {
        super(message);
    }
}
