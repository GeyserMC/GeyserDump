package org.geysermc.dump;

import org.geysermc.dump.database.DumpExpiry;
import org.geysermc.dump.database.model.StoredDump;
import org.geysermc.dump.database.repository.DumpCollection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class GeyserDumpApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

	@Autowired
	DumpCollection dumpCollection;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	DumpExpiry dumpExpiry;

	private static StoredDump newDump(Set<String> problems, String raw) {
		Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
		return new StoredDump(null, problems, raw, now, now, false);
	}

	private void setLastAccessed(String id, Instant lastAccessed) {
		jdbcTemplate.update("UPDATE dumps SET last_accessed = ? WHERE id = ?", Timestamp.from(lastAccessed), id);
	}

	@Test
	void contextLoads() {
	}

	@Test
	void saveGeneratesIdAndFindsDump() {
		StoredDump dump = newDump(Set.of("problem one", "problem two"),"{\"key\":\"value\"}");
		StoredDump saved = dumpCollection.save(dump);

		assertTrue(saved.id().matches("[a-f0-9]{32}"));
		assertFalse(StoredDump.isLegacyKey(saved.id()));
		assertEquals(Optional.of(saved), dumpCollection.findByKey(saved.id()));
		assertEquals(Optional.empty(), dumpCollection.findByKey("b".repeat(32)));
	}

	@Test
	void saveDumpWithNoProblems() {
		StoredDump saved = dumpCollection.save(newDump(Set.of(), "{}"));

		assertEquals(Set.of(), dumpCollection.findByKey(saved.id()).orElseThrow().problems());
	}

	@Test
	void markAccessedOnlyBumpsStaleDumps() {
		StoredDump fresh = dumpCollection.save(newDump(Set.of(), "{}"));
		StoredDump stale = dumpCollection.save(newDump(Set.of(), "{}"));
		Instant twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
		setLastAccessed(stale.id(), twoDaysAgo);

		dumpCollection.markAccessed(fresh.id());
		dumpCollection.markAccessed(stale.id());

		assertEquals(fresh.lastAccessed(), dumpCollection.findByKey(fresh.id()).orElseThrow().lastAccessed());
		assertTrue(dumpCollection.findByKey(stale.id()).orElseThrow().lastAccessed().isAfter(twoDaysAgo.plus(1, ChronoUnit.DAYS)));
	}

	@Test
	void expiryDeletesDumpsNotAccessedRecently() {
		StoredDump recent = dumpCollection.save(newDump(Set.of(), "{}"));
		StoredDump expired = dumpCollection.save(newDump(Set.of(), "{}"));
		setLastAccessed(expired.id(), Instant.now().minus(366, ChronoUnit.DAYS));

		assertTrue(dumpExpiry.deleteExpired() >= 1);

		assertEquals(Optional.empty(), dumpCollection.findByKey(expired.id()));
		assertTrue(dumpCollection.findByKey(recent.id()).isPresent());
	}

}
