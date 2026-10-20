/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/GeyserDump
 */

package org.geysermc.dump.database;

import org.geysermc.dump.database.model.StoredDump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Imports dumps from the GeyserDump-js file store, where each dump is a file named by the md5 hash of its key.
 * <p>
 * Existing dumps are skipped, so it is safe to re-run to pick up dumps added since the last import.
 * Run with: {@code java -jar GeyserDump.jar --dump.legacy-import=/path/to/data --spring.main.web-application-type=none --dump.expiry.enabled=false}
 */
@Component
@ConditionalOnProperty("dump.legacy-import")
public class LegacyImporter implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyImporter.class);
    private static final int BATCH_SIZE = 500;
    private static final Pattern FILE_NAME = Pattern.compile("[a-f0-9]{32}");

    private static final String INSERT = "INSERT INTO dumps (id, problems, raw, created_at, last_accessed, legacy) VALUES (?, ?, ?, ?, ?, true) ON CONFLICT (id) DO NOTHING";

    private final JdbcTemplate jdbcTemplate;
    private final Path directory;

    public LegacyImporter(JdbcTemplate jdbcTemplate, @Value("${dump.legacy-import}") Path directory) {
        this.jdbcTemplate = jdbcTemplate;
        this.directory = directory;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        importAll();
    }

    public Result importAll() throws IOException {
        LOGGER.info("Importing legacy dumps from {}", directory);

        int imported = 0;
        int existing = 0;
        int skipped = 0;
        List<StoredDump> batch = new ArrayList<>(BATCH_SIZE);

        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
            for (Path file : files) {
                String id = file.getFileName().toString();
                if (!Files.isRegularFile(file) || !FILE_NAME.matcher(id).matches()) {
                    continue;
                }

                try {
                    batch.add(StoredDump.legacy(id, Files.readString(file), Files.getLastModifiedTime(file).toInstant()));
                } catch (Exception e) {
                    LOGGER.warn("Skipping {}: {}", id, e.getMessage());
                    skipped++;
                    continue;
                }

                if (batch.size() == BATCH_SIZE) {
                    int inserted = insert(batch);
                    imported += inserted;
                    existing += batch.size() - inserted;
                    batch.clear();
                    LOGGER.info("Imported {} legacy dumps so far ({} already present, {} skipped)", imported, existing, skipped);
                }
            }
        }

        int inserted = insert(batch);
        imported += inserted;
        existing += batch.size() - inserted;

        LOGGER.info("Finished importing legacy dumps: {} imported, {} already present, {} skipped", imported, existing, skipped);
        return new Result(imported, existing, skipped);
    }

    private int insert(List<StoredDump> dumps) {
        if (dumps.isEmpty()) {
            return 0;
        }

        int[][] counts = jdbcTemplate.batchUpdate(INSERT, dumps, dumps.size(), (statement, dump) -> {
            statement.setString(1, dump.id());
            statement.setArray(2, statement.getConnection().createArrayOf("text", dump.problems().toArray()));
            statement.setString(3, dump.raw());
            statement.setTimestamp(4, Timestamp.from(dump.createdAt()));
            statement.setTimestamp(5, Timestamp.from(dump.lastAccessed()));
        });

        int inserted = 0;
        for (int[] batchCounts : counts) {
            for (int count : batchCounts) {
                inserted += count;
            }
        }
        return inserted;
    }

    public record Result(int imported, int existing, int skipped) {
    }
}
