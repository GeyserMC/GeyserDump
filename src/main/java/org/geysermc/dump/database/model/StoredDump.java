package org.geysermc.dump.database.model;

import org.bson.types.ObjectId;
import org.geysermc.dump.checks.AbstractCheck;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
        JSONObject bootstrapInfo;
        JSONObject dump;
        try {
            // Get json data from dump
            dump = new JSONObject(dumpData);

            // Check the dump isn't empty or an error message
            if (dump.isEmpty() || (dump.length() == 1 && dump.has("message"))) {
                throw new IllegalArgumentException("Empty dump");
            }

            // Setup some helper vars for quicker access
            bootstrapInfo = dump.getJSONObject("bootstrapInfo");
        } catch (JSONException ignored) {
            throw new IllegalArgumentException("Invalid dump");
        }

        Object platformObj = bootstrapInfo.get("platform");
        String platform = "";
        if (platformObj instanceof String) {
            platform = (String) platformObj;
        } else {
            platform = ((JSONObject) platformObj).getString("platformName").toUpperCase();
        }
        List<String> problems = new ArrayList<>();

        // Check plugins and stuff for potential issues
        for (AbstractCheck issueCheck : CHECKS) {
            if (issueCheck.compatiblePlatform(platform)) {
                try {
                    problems.addAll(issueCheck.checkIssues(dump));
                } catch (JSONException ignored) {
                }
            }
        }

        return new StoredDump(null, id, problems, dump.toString(), Instant.now());
    }
}
