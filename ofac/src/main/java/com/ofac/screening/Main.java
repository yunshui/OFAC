package com.ofac.screening;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the OFAC blacklist REST API.
 *
 * Endpoint:
 *   GET /search?name=xxx
 */
@SpringBootApplication(scanBasePackages = "com.ofac")
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
