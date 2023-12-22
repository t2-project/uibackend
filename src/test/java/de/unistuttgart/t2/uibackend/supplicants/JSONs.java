package de.unistuttgart.t2.uibackend.supplicants;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Provides JSONs for the Tests.
 * 
 * @author maumau
 */
public class JSONs {

    public static String productId = "foo";
    public static int units = 42;
    public static String anotherproductId = "foo2";
    public static int anotherunits = 42;
    public static String sessionId = "bar";
    public static String orchestratorUrl = "http://localhost:8085/order";
    public static String cartUrl = "http://localhost:8080/cart";
    public static String inventoryUrl = "http://localhost:8082/inventory";
    public static String reservationEndpoint = "reservation";
    public static String reservationUrl = inventoryUrl + "/" + reservationEndpoint;

    static JsonNodeFactory factory = JsonNodeFactory.instance;

    public static String updatedCartResponse() {
        JsonNode cart = factory.objectNode().set("content", makeCartContent(productId, 43));
        return cart.toString();
    }

    public static JsonNode makeCartContent(String productId, int units) {
        JsonNode content = factory.objectNode().set(productId, factory.numberNode(units));
        return content;
    }

    public static JsonNode makeLinks(String url, String fieldname) {
        JsonNode href = factory.objectNode().set("href", factory.textNode(url));
        ObjectNode links = (ObjectNode) factory.objectNode();
        links.set("self", href);
        links.set(fieldname, href);

        return links;
    }

    public static String cartResponse() {
        JsonNode content = makeCartContent(productId, units);
        JsonNode links = makeLinks(cartUrl + "/" + sessionId, "cart");
        JsonNode response = ((ObjectNode) factory.objectNode().set("content", content)).set("_links", links);

        return response.toString();
    }

    public static String cartResponseMulti() {
        JsonNode content = makeCartContent(productId, units);
        content = ((ObjectNode) content).set(anotherproductId, factory.numberNode(anotherunits));
        JsonNode links = makeLinks(cartUrl + "/" + sessionId, "cart");
        JsonNode response = ((ObjectNode) factory.objectNode().set("content", content)).set("_links", links);

        return response.toString();
    }

    public static String inventoryResponse() {
        ObjectNode base = inventoryBase("name", "description");
        JsonNode links = makeLinks(inventoryUrl + "/" + productId, "inventory");

        base.set("_links", links);

        return base.toString();
    }

    public static ObjectNode inventoryBase(String name, String description) {
        ObjectNode base = factory.objectNode();
        base.set("name", factory.textNode(name));
        base.set("description", factory.textNode(description));
        base.set("units", factory.numberNode(5));
        base.set("price", factory.numberNode(1.0));

        return base;
    }

    public static String anotherInventoryResponse() {
        ObjectNode base = inventoryBase("name2", "description2");
        JsonNode links = makeLinks(inventoryUrl + "/" + anotherproductId, "inventory");

        base.set("_links", links);

        return base.toString();
    }

    public static String inventoryResponseAllProducts() {
        JsonNode links1 = makeLinks(inventoryUrl + "/" + productId, "inventory");
        JsonNode links2 = makeLinks(inventoryUrl + "/" + anotherproductId, "inventory");
        ObjectNode base1 = inventoryBase("name", "description");
        ObjectNode base2 = inventoryBase("name2", "description2");

        base1.set("_links", links1);
        base2.set("_links", links2);

        JsonNode inventory = factory.arrayNode().add(base1).add(base2);
        ObjectNode all = factory.objectNode().set("inventory", inventory);
        ObjectNode embedded = factory.objectNode().set("_embedded", all);

        return embedded.toString();
    }
}
