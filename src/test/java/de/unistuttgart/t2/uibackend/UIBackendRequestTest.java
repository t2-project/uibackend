package de.unistuttgart.t2.uibackend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.t2.common.domain.saga.SagaRequest;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;      
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * 
 * What i am doing here:
 *  - set up the mock server to expect a certain request
 *  - execute a service operation that calls some other service 
 *  - that call no ends up at the mock server
 *  - verify that mock server received the expected request
 * 
 * @author maumau
 *
 */
//@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(TestContext.class)
public class UIBackendRequestTest {

	String url = "http://localhost:8082"; 
	String foo = url + "/order";
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Autowired
	UIBackendService service;
	
    @Autowired
    private RestTemplate template;
	
	private MockRestServiceServer mockServer;
	
    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(template);
    }
    
    
	
	@Test
	public void testConfirmOrder() throws Exception {
		// this does not work....
		SagaRequest reqest = new SagaRequest("sessionId", "cardNumber", "cardOwner", "checksum", 242.2);
		System.out.println(mapper.writeValueAsString(reqest));
		
		
        mockServer.expect(ExpectedCount.once(), 
                requestTo(foo))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(mapper.writeValueAsString(reqest)))
                .andRespond(withStatus(HttpStatus.OK)); 
        
        
        service.confirmOrder(reqest.getCardNumber(), reqest.getCardOwner(), reqest.getChecksum(), reqest.getSessionId(), reqest.getTotal());
        mockServer.verify();
		
	}
}
