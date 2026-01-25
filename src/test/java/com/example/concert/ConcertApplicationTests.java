package com.example.concert;

import com.example.concert.config.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ConcertApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}
