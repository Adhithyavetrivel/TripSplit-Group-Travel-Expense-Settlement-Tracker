package com.tripsplit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tripSplitOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TripSplit API")
                        .description("Group Travel Expense Settlement Tracker API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TripSplit")
                                .email("support@tripsplit.com")));
    }
}
