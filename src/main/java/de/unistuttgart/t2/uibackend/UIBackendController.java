package de.unistuttgart.t2.uibackend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.unistuttgart.t2.common.CartContent;
import de.unistuttgart.t2.common.OrderRequest;
import de.unistuttgart.t2.common.Product;

@RestController
public class UIBackendController {

	@Autowired
	private UIBackendService service;

	@GetMapping("/products/all")
	public List<Product> getAllProducts() {
		return service.getAllProducts();
	}

	@PostMapping("/products/add")
	public List<Product> addItemsToCart(HttpSession session, @RequestBody CartContent products) {
		List<Product> successfullyAddedProducts = new ArrayList<>();

		for (Entry<String, Integer> product : products.getContent().entrySet()) {
			try {
				// contact inventory first, cause i'd rather have a dangling reservation than
				// thing in the cart that are not backed with reservations
				Product addedProduct = service.makeReservations(session.getId(), product.getKey(), product.getValue());
				service.addItemToCart(session.getId(), product.getKey(), product.getValue());
				successfullyAddedProducts.add(addedProduct);

			} catch (Exception e) {
				e.printStackTrace();
			}
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
	public void confirmOrder(HttpSession session, @RequestBody OrderRequest request) {
		service.confirmOrder(session.getId(), request.getCardNumber(), request.getCardOwner(), request.getChecksum());
	}

	@GetMapping("/")
	public String index() {
		return "Greetings from UI Backend :)";
	}
}