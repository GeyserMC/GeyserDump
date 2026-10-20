package org.geysermc.dump;

import org.geysermc.dump.database.model.StoredDump;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record AnalysedDump(
    GitData gitData,
    List<String> problems,
    String raw,
    Instant createdAt
) {
    public static AnalysedDump from(StoredDump storedDump) {
        JsonNode gitInfo;
        JsonNode dump;
        try {
            // Get json data from dump
            dump = JsonMapper.shared().readTree(storedDump.raw());
            if (!dump.isObject()) {
                throw new IllegalArgumentException("Invalid dump");
            }

            // Check the dump isn't empty or an error message
            if (dump.isEmpty() || (dump.size() == 1 && dump.has("message"))) {
                throw new IllegalArgumentException("Empty dump");
            }

            // Setup some helper vars for quicker access
            gitInfo = dump.required("gitInfo");
        } catch (JacksonException ignored) {
            throw new IllegalArgumentException("Invalid dump");
        }

        GitData gitData = GitData.from(gitInfo);

        List<String> problems = new ArrayList<>(storedDump.problems());
        if (!gitData.latest()) {
            problems.add("You aren't on the latest Geyser version! Please [download](https://geysermc.org/download) the latest version.");
        }

        return new AnalysedDump(gitData, problems, storedDump.raw(), storedDump.createdAt());
    }
}
