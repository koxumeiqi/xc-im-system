package org.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.app.dao")
public class AppServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppServiceApplication.class, args);
	}

}
