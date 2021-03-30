

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CartContent {
	@JsonProperty("content")
	private Map<String, Integer> content;
	

	public CartContent() {
		super();
		this.content = new HashMap<>();
	}

	@JsonCreator
	public CartContent(@JsonProperty("content") Map<String, Integer> content) {
		super();
		this.content = content;
		//this.foo = foo;
	}

	public void setContent(Map<String, Integer> content) {
		this.content = content;
	}
	
	public Collection<String> getContent(){
		return content.keySet();
	}
	
	public Integer getUnits(String productId) {
		if (!content.containsKey(productId)) {
			return 0;
		} 
		return content.get(productId);
	}
//
//	public String getFoo() {
//		return foo;
//	}
//
//	public void setFoo(String foo) {
//		this.foo = foo;
//	}
	
	
}
