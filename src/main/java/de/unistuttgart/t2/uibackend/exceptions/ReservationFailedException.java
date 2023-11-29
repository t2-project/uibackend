package de.unistuttgart.t2.uibackend.exceptions;

import java.io.Serial;

/**
 * Indicates that placing a reservation for a product failed.
 *
 * @author maumau
 */
public final class ReservationFailedException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public ReservationFailedException(String message) {
        super(message);
    }
}
