package de.unistuttgart.t2.uibackend.exceptions;

/**
 * Indicates that placing reservation for product failed.
 * 
 * @author maumau
 *
 */
public class CartInteractionFailedException extends Exception {
	
	public CartInteractionFailedException(String message) {
		super(message);
	}
}
