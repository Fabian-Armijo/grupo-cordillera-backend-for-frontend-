package com.cordillera.bff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BffApplicationTests {

	@Test
	void contextLoads() {
		// Forzamos la ejecución del método main con un arreglo de strings vacío
		BffApplication.main(new String[] {});
	}

}