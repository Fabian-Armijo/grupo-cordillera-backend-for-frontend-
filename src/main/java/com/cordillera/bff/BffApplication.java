package com.cordillera.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableFeignClients(basePackages = {"com.cordillera.bff.client", "com.cordillera.bff"})
public class BffApplication {

	public static void main(String[] args) {
		SpringApplication.run(BffApplication.class, args);
	}
}
