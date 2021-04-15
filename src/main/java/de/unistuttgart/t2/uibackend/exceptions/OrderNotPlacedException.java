package de.unistuttgart.t2.uibackend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Indicates that an Order was not placed.
 * 
 * For now the reasons are mostly unknown.
 * 
 * @author maumau
 *
 */
public class OrderNotPlacedException extends Exception {

	public OrderNotPlacedException(String message) {
		super(message);
	}
}
