package com.paymybuddy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.security.oauth2.client.registration.google.client-id=test",
		"spring.security.oauth2.client.registration.google.client-secret=test"
})
class PayMyBuddyApplicationTests {

	@Test
	void contextLoads() {
	}

}
