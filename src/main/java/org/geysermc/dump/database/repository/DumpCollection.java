/*
 * Copyright (c) 2024-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.dump.database.repository;

import org.geysermc.dump.database.model.StoredDump;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.Optional;

public interface DumpCollection extends Repository<StoredDump, String> {
    StoredDump save(StoredDump dump);

    Optional<StoredDump> findByIdAndLegacy(String id, boolean legacy);

    /**
     * Mark a dump as accessed, at most once a day to avoid a write on every view
     *
     * @param id the id of the dump
     */
    @Modifying
    @Query("UPDATE dumps SET last_accessed = now() WHERE id = :id AND last_accessed < now() - interval '1 day'")
    void markAccessed(String id);

    @Modifying
    @Query("DELETE FROM dumps WHERE last_accessed < :cutoff")
    int deleteNotAccessedSince(Instant cutoff);

    /**
     * Find a dump by the key from its url, looking up legacy keys by the hash they were stored under
     *
     * @param key the key from the url
     * @return the dump if found
     */
    default Optional<StoredDump> findByKey(String key) {
        if (StoredDump.isLegacyKey(key)) {
            return findByIdAndLegacy(StoredDump.legacyId(key), true);
        }

        return findByIdAndLegacy(key, false);
    }
}
