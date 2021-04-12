package de.unistuttgart.t2.uibackend;

/**
 * Here i store sting representations of what ever jsons i need for my test
 * cases.
 * 
 * I'm pretty sure there is a better was to do this, that i do not yet know of.
 * 
 * maybe move this to common. maybe i want to use this to test other services as
 * well.
 * 
 * i would be cool if i had something like... build json from object. but can
 * not use default serialization, cause i gotta fake the acutal inventory
 * response, which is more, than just a serialized product.
 * 
 * @author maumau
 *
 */
public class JSONs {
	
	public static String productId = "foo";
	public static String anotherproductId = "foo2";
	public static String sessionId = "bar";
	public static String orchestratorUrl = "http://localhost:8083/order/";
	public static String cartUrl = "http://localhost:8080/cart/";
	public static String inventoryUrl = "http://localhost:8082/inventory/";
	public static String reservationEndpoint = "reservation/";
	public static String reservationUrl = inventoryUrl + reservationEndpoint;
    
    public static String cartResponse =  "{\n"
    	    	+ "  \"content\" : {\n"
    	    	+ "    \"" + productId + "\" : 42\n"
    	    	+ "  },\n"
    	    	+ "  \"_links\" : {\n"
    	    	+ "    \"self\" : {\n"
    	    	+ "      \"href\" : \""+ cartUrl + sessionId + "\"\n"
    	    	+ "    },\n"
    	    	+ "    \"cart\" : {\n"
    	    	+ "      \"href\" : \""+ cartUrl + sessionId + "\"\n"
    	    	+ "    }\n"
    	    	+ "  }\n"
    	    	+ "}";
    
    public static String cartResponseMulti =  "{\n"
	    	+ "  \"content\" : {\n"
	    	+ "    \"" + productId + "\" : 42,\n"
	    	+ "    \"" + anotherproductId + "\" : 3\n"
	    	+ "  },\n"
	    	+ "  \"_links\" : {\n"
	    	+ "    \"self\" : {\n"
	    	+ "      \"href\" : \""+ cartUrl + sessionId + "\"\n"
	    	+ "    },\n"
	    	+ "    \"cart\" : {\n"
	    	+ "      \"href\" : \""+ cartUrl + sessionId + "\"\n"
	    	+ "    }\n"
	    	+ "  }\n"
	    	+ "}";
    	
    public static String inventoryResponse = "{\n"
    			+ "  \"name\" : \"name\",\n"
    			+ "  \"description\" : \"description\",\n"
    			+ "  \"units\" : 5,\n"
    			+ "  \"price\" : 1.0,\n"
    			+ "  \"reservations\" : { },\n"
    			+ "  \"availableUnits\" : 0,\n"
    			+ "  \"_links\" : {\n"
    			+ "    \"self\" : {\n"
    			+ "      \"href\" : \"" + inventoryUrl + productId + "\"\n"
    			+ "    },\n"
    			+ "    \"inventory\" : {\n"
    			+ "      \"href\" : \"" + inventoryUrl + productId +"\"\n"
    			+ "    }\n"
    			+ "  }\n"
    			+ "}";
    	
    public static String anotherInventoryResponse = "{\n"
    			+ "  \"name\" : \"name2\",\n"
    			+ "  \"description\" : \"description2\",\n"
    			+ "  \"units\" : 5,\n"
    			+ "  \"price\" : 1.0,\n"
    			+ "  \"reservations\" : { },\n"
    			+ "  \"availableUnits\" : 0,\n"
    			+ "  \"_links\" : {\n"
    			+ "    \"self\" : {\n"
    			+ "      \"href\" : \"" + inventoryUrl + anotherproductId + "\"\n"
    			+ "    },\n"
    			+ "    \"inventory\" : {\n"
    			+ "      \"href\" : \"" + inventoryUrl + anotherproductId +"\"\n"
    			+ "    }\n"
    			+ "  }\n"
    			+ "}";
    

	public static String inventoryresponseAllProducts = "{\n"
			+ "  \"_embedded\" : {\n"
			+ "    \"inventory\" : [ " + inventoryResponse + "," +  anotherInventoryResponse + " ]\n"
			+ "  },\n"
			+ "  \"_links\" : {\n"
			+ "    \"self\" : {\n"
			+ "      \"href\" : \"http://localhost:8082/inventory\"\n"
			+ "    },\n"
			+ "    \"profile\" : {\n"
			+ "      \"href\" : \"http://localhost:8082/profile/inventory\"\n"
			+ "    }\n"
			+ "  },\n"
			+ "  \"page\" : {\n"
			+ "    \"size\" : 20,\n"
			+ "    \"totalElements\" : 1,\n"
			+ "    \"totalPages\" : 1,\n"
			+ "    \"number\" : 0\n"
			+ "  }\n"
			+ "}";
}
