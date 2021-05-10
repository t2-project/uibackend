package de.unistuttgart.t2.uibackend.supplicants;

/**
 * String representations of the JSONs used for the Tests. 
 *  
 * <p> 
 * TODO : this is a hell hole. do something like... create object and use json mapper??
 * 
 * @author maumau
 *
 */
public class JSONs {
	
	public static String productId = "foo";
	public static int units = 42;
	public static String anotherproductId = "foo2";
	public static int anotherunits = 42;
	public static String sessionId = "bar";
	public static String orchestratorUrl = "http://localhost:8083/order/";
	public static String cartUrl = "http://localhost:8080/cart/";
	public static String inventoryUrl = "http://localhost:8082/inventory/";
	public static String reservationEndpoint = "reservation/";
	public static String reservationUrl = inventoryUrl + reservationEndpoint;
    
	public static String updatedCartResponse =  "{\n"
	    	+ "  \"content\" : {\n"
	    	+ "    \"" + productId + "\" : 43\n"
	    	+ "  }"
	    	+ "}";
	
    public static String cartResponse =  "{\n"
    	    	+ "  \"content\" : {\n"
    	    	+ "    \"" + productId + "\" : " + units +"\n"
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
	    	+ "    \"" + productId + "\" : " + units +",\n"
	    	+ "    \"" + anotherproductId + "\" : " + anotherunits +"\n"
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
