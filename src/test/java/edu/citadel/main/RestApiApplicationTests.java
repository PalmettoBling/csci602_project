package edu.citadel.main;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:jwt-test.properties")
public class RestApiApplicationTests {

	@Test
	public void contextLoads() {}

}
