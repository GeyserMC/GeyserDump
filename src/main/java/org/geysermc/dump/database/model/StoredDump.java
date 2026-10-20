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

package org.geysermc.dump.database.model;

import org.bson.types.ObjectId;
import org.geysermc.dump.checks.AbstractCheck;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

@Document("dumps")
public record StoredDump(
    @Id ObjectId _id,
    String id,
    List<String> problems,
    String raw,
    @CreatedDate Instant createdAt
) {
    private static final List<AbstractCheck> CHECKS;

    static {
        CHECKS = ServiceLoader.load(AbstractCheck.class).stream()
            .map(ServiceLoader.Provider::get)
            .toList();
    }

    public static StoredDump from(String id, String dumpData) {
        JsonNode bootstrapInfo;
        JsonNode dump;
        try {
            // Get json data from dump
            dump = JsonMapper.shared().readTree(dumpData);
            if (!dump.isObject()) {
                throw new IllegalArgumentException("Invalid dump");
            }

            // Check the dump isn't empty or an error message
            if (dump.isEmpty() || (dump.size() == 1 && dump.has("message"))) {
                throw new IllegalArgumentException("Empty dump");
            }

            // Setup some helper vars for quicker access
            bootstrapInfo = dump.required("bootstrapInfo");
        } catch (JacksonException ignored) {
            throw new IllegalArgumentException("Invalid dump");
        }

        JsonNode platformNode = bootstrapInfo.required("platform");
        String platform = "";
        if (platformNode.isString()) {
            platform = platformNode.stringValue();
        } else {
            platform = platformNode.required("platformName").stringValue().toUpperCase();
        }
        List<String> problems = new ArrayList<>();

        // Check plugins and stuff for potential issues
        for (AbstractCheck issueCheck : CHECKS) {
            if (issueCheck.compatiblePlatform(platform)) {
                try {
                    problems.addAll(issueCheck.checkIssues(dump));
                } catch (JacksonException ignored) {
                }
            }
        }

        return new StoredDump(null, id, problems, dump.toString(), Instant.now());
    }
}
