package com.cordillera.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

// 1. Excluimos la BD (lo que hicimos en el paso anterior)
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
// 2. ACTIVAMOS FEIGN y le decimos exactamente dónde están las interfaces
@EnableFeignClients(basePackages = "com.cordillera.bff.client")
public class BffApplication {

	public static void main(String[] args) {
		SpringApplication.run(BffApplication.class, args);
	}
}
