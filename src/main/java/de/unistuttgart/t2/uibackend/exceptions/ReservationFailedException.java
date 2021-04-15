package de.unistuttgart.t2.uibackend.exceptions;

/**
 * Indicates that placing reservation for product failed.
 * 
 * @author maumau
 *
 */
public class ReservationFailedException extends Exception {
	
	public ReservationFailedException(String message) {
		super(message);
	}
}
