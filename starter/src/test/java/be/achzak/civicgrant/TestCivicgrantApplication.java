package be.achzak.civicgrant;

import org.springframework.boot.SpringApplication;

public class TestCivicgrantApplication {

	public static void main(String[] args) {
		SpringApplication.from(CivicgrantApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
