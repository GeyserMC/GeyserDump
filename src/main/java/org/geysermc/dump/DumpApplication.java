package org.geysermc.dump;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MainConfig.class)
public class DumpApplication {

	public static void main(String[] args) {
		SpringApplication.run(DumpApplication.class, args);
	}

}
