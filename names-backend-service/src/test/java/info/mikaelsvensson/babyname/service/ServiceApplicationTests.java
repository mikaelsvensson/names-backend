package info.mikaelsvensson.babyname.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"test", "email-smtp", "db"})
class ServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
