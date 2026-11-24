package com.paywell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * PAYWELL backend. Demonstration build: payments are executed by a simulated provider and no real money moves.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PaywellApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaywellApplication.class, args);
    }
}
