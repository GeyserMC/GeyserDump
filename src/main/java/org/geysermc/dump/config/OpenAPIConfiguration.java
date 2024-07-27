package org.geysermc.dump.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Configuration
public class OpenAPIConfiguration {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("GeyserMC Dump API")
                .description("API for analyzing and fetching dumps produced by Geyser")
                .version("v1")
            )
            .servers(List.of(
                new Server().url("https://dump.geysermc.org")
            ));
    }

    @Bean
    @SuppressWarnings("rawtypes") // nothing we can do, the API exposes it raw
    public OpenApiCustomizer sortSchemasAlphabetically() {
        return openApi -> {
            final Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            openApi.getComponents().setSchemas(new TreeMap<>(schemas));
        };
    }
}
