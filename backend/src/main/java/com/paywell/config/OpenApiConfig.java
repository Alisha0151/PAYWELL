package com.paywell.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paywellOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PAYWELL API")
                        .version("0.1.0")
                        .description("""
                                PAYWELL demonstration backend. Payments run through a SIMULATED provider: no real money moves, \
                                balances are demo-wallet funds, and nothing here is a bank account. \
                                Money amounts are decimal strings (never floats). Payment POSTs require an Idempotency-Key header."""))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
