import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.t2.common.domain.CartContent;
import de.unistuttgart.t2.common.domain.Product;

public class Playground {

	public static void main(String[] args) {
		(new Playground()).test3();
		Integer i = null;
		int a = i;
		System.out.println(a);
	}
	
	public void test3() {

		String foobody = "{\n"
				+ "  \"id\" : \"id\",\n"
    			+ "  \"name\" : \"name\",\n"
    			+ "  \"description\" : \"description\",\n"
    			+ "  \"units\" : 5,\n"
    			+ "  \"price\" : 1.0,\n"
    			+ "  \"reservations\" : { }}";

		try {
			Product cc = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
				     false).readValue(foobody, Product.class);
			int a = 2 +3;
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
		
	}
	
	
	public void test() {

		String foobody = "{\"content\" : { \"a\" : \"42\" }}";

		try {
			CartContent cc = new ObjectMapper().readValue(foobody, CartContent.class);
			int a = 2 +3;
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
		
	}
	
	public void test2() {
		//ResponseEntity<String> foo = (new RestTemplate()).getForEntity("http://localhost:8080/gen-cart/A7CF3C1113DEF8CBE56F28C64A15E25A", String.class);
		//ResponseEntity<CartContent> foocc = (new RestTemplate()).getForEntity("http://localhost:8080/gen-cart/A7CF3C1113DEF8CBE56F28C64A15E25A", CartContent.class);
		//CartContent foobodycc = foocc.getBody();
		String foobody = "{\"content\" : { \"605dc7ex4ef03a23812804600\" : \"42\" }}";
//	,
//		  "_links" : {
//		    "self" : {
//		      "href" : "http://localhost:8080/gen-cart/A7CF3C1113DEF8CBE56F28C64A15E25A"
//		    },
//		    "gen-cart" : {
//		      "href" : "http://localhost:8080/gen-cart/A7CF3C1113DEF8CBE56F28C64A15E25A"
//		    }
//		  }
//		}"
		try {
			CartContent cc = new ObjectMapper().readValue(foobody, CartContent.class);
			int a = 2 +3;
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
		
	}
	
}
