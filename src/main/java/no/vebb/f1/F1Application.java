package no.vebb.f1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class F1Application {

	public static void main(String[] args) {
		SpringApplication.run(F1Application.class, args);
	}

}
