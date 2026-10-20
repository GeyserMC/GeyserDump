package org.geysermc.dump;

import org.geysermc.dump.database.model.StoredDump;
import org.geysermc.dump.database.repository.DumpCollection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class GeyserDumpApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

	@Autowired
	DumpCollection dumpCollection;

	@Test
	void contextLoads() {
	}

	@Test
	void saveGeneratesIdAndFindsDump() {
		StoredDump dump = new StoredDump(null, List.of("problem one", "problem two"), "{\"key\":\"value\"}", Instant.now().truncatedTo(ChronoUnit.MICROS));
		StoredDump saved = dumpCollection.save(dump);

		assertTrue(saved.id().matches("[a-f0-9]{32}"));
		assertEquals(Optional.of(saved), dumpCollection.findById(saved.id()));
		assertEquals(Optional.empty(), dumpCollection.findById("b".repeat(32)));
	}

	@Test
	void saveDumpWithNoProblems() {
		StoredDump saved = dumpCollection.save(new StoredDump(null, List.of(), "{}", Instant.now().truncatedTo(ChronoUnit.MICROS)));

		assertEquals(List.of(), dumpCollection.findById(saved.id()).orElseThrow().problems());
	}

}
