package org.geysermc.dump;

import org.geysermc.dump.database.DumpExpiry;
import org.geysermc.dump.database.LegacyImporter;
import org.geysermc.dump.database.model.StoredDump;
import org.geysermc.dump.database.repository.DumpCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

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

	private static StoredDump newDump(List<String> problems, String raw) {
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
		StoredDump dump = newDump(List.of("problem one", "problem two"), "{\"key\":\"value\"}");
		StoredDump saved = dumpCollection.save(dump);

		assertTrue(saved.id().matches("[a-f0-9]{32}"));
		assertFalse(StoredDump.isLegacyKey(saved.id()));
		assertEquals(Optional.of(saved), dumpCollection.findByKey(saved.id()));
		assertEquals(Optional.empty(), dumpCollection.findByKey("b".repeat(32)));
	}

	@Test
	void saveDumpWithNoProblems() {
		StoredDump saved = dumpCollection.save(newDump(List.of(), "{}"));

		assertEquals(List.of(), dumpCollection.findByKey(saved.id()).orElseThrow().problems());
	}

	@Test
	void markAccessedOnlyBumpsStaleDumps() {
		StoredDump fresh = dumpCollection.save(newDump(List.of(), "{}"));
		StoredDump stale = dumpCollection.save(newDump(List.of(), "{}"));
		Instant twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
		setLastAccessed(stale.id(), twoDaysAgo);

		dumpCollection.markAccessed(fresh.id());
		dumpCollection.markAccessed(stale.id());

		assertEquals(fresh.lastAccessed(), dumpCollection.findByKey(fresh.id()).orElseThrow().lastAccessed());
		assertTrue(dumpCollection.findByKey(stale.id()).orElseThrow().lastAccessed().isAfter(twoDaysAgo.plus(1, ChronoUnit.DAYS)));
	}

	@Test
	void expiryDeletesDumpsNotAccessedRecently() {
		StoredDump recent = dumpCollection.save(newDump(List.of(), "{}"));
		StoredDump expired = dumpCollection.save(newDump(List.of(), "{}"));
		setLastAccessed(expired.id(), Instant.now().minus(366, ChronoUnit.DAYS));

		assertTrue(dumpExpiry.deleteExpired() >= 1);

		assertEquals(Optional.empty(), dumpCollection.findByKey(expired.id()));
		assertTrue(dumpCollection.findByKey(recent.id()).isPresent());
	}

	@Test
	void legacyIdMatchesNodeMd5() {
		// md5 of the UTF-8 bytes, as computed by the old GeyserDump (values from Node's crypto module)
		assertEquals("46b3931b9959c927df4fc65fdee94b07", StoredDump.legacyId("about"));
		assertEquals("5f74832efc018589d682836c56271cba", StoredDump.legacyId("AbCdEfGhIjKlMnOpQrStUvWxYz012345"));
		assertTrue(StoredDump.isLegacyKey("AbCdEfGhIjKlMnOpQrStUvWxYz012345"));
	}

	@Test
	void importLegacyFileStore(@TempDir Path data) throws IOException {
		String dockerKey = "AbCdEfGhIjKlMnOpQrStUvWxYz012345";
		String otherJsonKey = "ZyXwVuTsRqPoNmLkJiHgFeDcBa987654";
		Instant modified = Instant.parse("2023-05-01T12:00:00Z");

		String dockerDump = "{\"bootstrapInfo\":{\"platform\":\"SPIGOT\"},\"versionInfo\":{\"network\":{\"dockerCheck\":true}}}";
		Files.writeString(data.resolve(StoredDump.legacyId(dockerKey)), dockerDump);
		Files.setLastModifiedTime(data.resolve(StoredDump.legacyId(dockerKey)), FileTime.from(modified));
		Files.writeString(data.resolve(StoredDump.legacyId(otherJsonKey)), "[1, 2, 3]");
		Files.writeString(data.resolve(StoredDump.legacyId("invalid")), "not json");
		Files.writeString(data.resolve("notes.txt"), "not a dump");

		LegacyImporter importer = new LegacyImporter(jdbcTemplate, data);
		assertEquals(new LegacyImporter.Result(2, 0, 1), importer.importAll());

		// Re-running only skips what is already there
		assertEquals(new LegacyImporter.Result(0, 2, 1), importer.importAll());

		StoredDump docker = dumpCollection.findByKey(dockerKey).orElseThrow();
		assertTrue(docker.legacy());
		assertEquals(dockerDump, docker.raw());
		assertEquals(modified, docker.createdAt());
		// Legacy dumps start their expiry from the import, not the original upload
		assertTrue(docker.lastAccessed().isAfter(Instant.now().minus(1, ChronoUnit.HOURS)));
		// Only DockerCheck applies, the rest are missing the fields they need
		assertEquals(1, docker.problems().size());
		assertTrue(docker.problems().getFirst().contains("docker"));

		assertEquals(List.of(), dumpCollection.findByKey(otherJsonKey).orElseThrow().problems());

		// Legacy dumps can only be found by their original key
		assertEquals(Optional.empty(), dumpCollection.findByKey(StoredDump.legacyId(dockerKey)));
	}

}
