package de.unistuttgart.t2.uibackend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import de.unistuttgart.t2.common.CartContent;
import de.unistuttgart.t2.common.OrderRequest;
import de.unistuttgart.t2.common.Product;
import de.unistuttgart.t2.uibackend.exceptions.CartInteractionFailedException;
import de.unistuttgart.t2.uibackend.exceptions.OrderNotPlacedException;
import de.unistuttgart.t2.uibackend.exceptions.ReservationFailedException;

@RestController
public class UIBackendController {

	@Autowired
	private UIBackendService service;

	@GetMapping("/products/all")
	public List<Product> getAllProducts() {
		return service.getAllProducts();
	}

	@PostMapping("/products/add")
	public List<Product> addItemsToCart(HttpSession session, @RequestBody CartContent products) throws ReservationFailedException {
		List<Product> successfullyAddedProducts = new ArrayList<>();
		
		StringBuilder failures = new StringBuilder(); 

		for (Entry<String, Integer> product : products.getContent().entrySet()) {
			try {
				// contact inventory first, cause i'd rather have a dangling reservation than
				// thing in the cart that are not backed with reservations
				Product addedProduct = service.makeReservations(session.getId(), product.getKey(), product.getValue());
				service.addItemToCart(session.getId(), product.getKey(), product.getValue());
				successfullyAddedProducts.add(addedProduct);

			} catch (ReservationFailedException e) {
				failures.append(e.getMessage()).append("\n");
			}
		}
		
		// unless all reservations fail, we'll just ignore the failures :x
		if (successfullyAddedProducts.isEmpty()) {
			throw new ReservationFailedException(failures.toString());
		}
		
		return successfullyAddedProducts;
	}

	@PostMapping("/products/delete")
	public void deleteItemsFromCart(HttpSession session, @RequestBody CartContent products) {
		// might leave dangling reservations :/
		for (Entry<String, Integer> product : products.getContent().entrySet()) {
			try {
				service.deleteItemFromCart(session.getId(), product.getKey(), product.getValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@GetMapping("/cart")
	public List<Product> getCart(HttpSession session) {
		return service.getProductsInCart(session.getId());
	}

	@PostMapping("/confirm")
	public void confirmOrder(HttpSession session, @RequestBody OrderRequest request) throws OrderNotPlacedException {
		service.confirmOrder(session.getId(), request.getCardNumber(), request.getCardOwner(), request.getChecksum());
		service.deleteCart(session.getId());
		// session stops after order is placed.
		session.invalidate();
	}

	@GetMapping("/")
	public String index() {
		return "Greetings from UI Backend :)";
	}

	@ExceptionHandler(OrderNotPlacedException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ResponseEntity<String> handleOrderNotPlacesException(OrderNotPlacedException exception) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
	}
	
	@ExceptionHandler(ReservationFailedException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ResponseEntity<String> handleReservationFailedException(ReservationFailedException exception) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
	}
	
	@ExceptionHandler(CartInteractionFailedException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ResponseEntity<String> handleCartInteractionFailedException(CartInteractionFailedException exception) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
	}
}