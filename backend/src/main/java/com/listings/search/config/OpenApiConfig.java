package com.listings.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** API metadata shown in Swagger UI (/swagger-ui.html) and /v3/api-docs. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI listingSearchOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Listing Search API")
                .version("v1")
                .description("Search, rank and paginate property listings ingested from multiple MLS feeds."));
    }
}
