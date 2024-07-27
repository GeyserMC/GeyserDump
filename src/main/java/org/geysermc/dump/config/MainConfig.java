package org.geysermc.dump.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "dump")
public record MainConfig(
    String githubToken
) {
    @ConstructorBinding
    public MainConfig {
    }
}
