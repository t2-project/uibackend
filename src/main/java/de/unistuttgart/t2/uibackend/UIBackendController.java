package de.unistuttgart.t2.uibackend;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.unistuttgart.t2.common.domain.PaymentInfo;
import de.unistuttgart.t2.common.domain.Product;

@RestController
public class UIBackendController {

	@Autowired
	private UIBackendService service;

	@GetMapping("/all")
	public List<Product> getAllProducts() {
		return service.getAllProducts();
	}

	@PostMapping("/add")
	public void addItemToCart(HttpSession session, @RequestParam(value = "id", defaultValue = "foo") String id, @RequestParam(value = "units", defaultValue = "2") Integer units) {
		try {
			service.makeReservations(session.getId(), id, units);
			service.addItemToCart(session.getId(), id, units);
		} catch (Exception e) {
			e.printStackTrace();
			// return error....
		}
		// return success....
	}

	@PostMapping("/delete")
	public String deleteItemFromCart(HttpSession session, @RequestBody Map<String, Integer> products) {
		// get blob
		String productId = products.keySet().iterator().next();
		Integer units = products.get(productId);

		service.deleteItemFromCart(session.getId(), productId, units);

		return products.toString();
	}

	@GetMapping("/cart")
	public List<Product> getCart(HttpSession session) {
		return service.getProductsInCart(session.getId());
	}
	
	@PutMapping("/payment")
	public void putPayment(HttpSession session, @RequestBody PaymentInfo paymentInfo) {
		service.putPayment(session.getId(), paymentInfo);
	}

	@PostMapping("/confirm")
	public void confirmOrder(HttpSession session) {
		// TODO
		service.confirmOrder(session.getId());
	}

	@GetMapping("/")
	public String index() {
		return "Greetings from Controller";
	}
}