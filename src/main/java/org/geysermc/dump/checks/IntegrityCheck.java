/*
 * Copyright (c) 2020-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.dump.checks;

import com.google.auto.service.AutoService;
import org.geysermc.dump.util.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoService(AbstractCheck.class)
public class IntegrityCheck extends AbstractCheck {
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+\\.\\d+\\.\\d+)-b(\\d+) \\(.*\\)$");

    @Override
    public List<String> checkIssues(JsonNode dump) {
        List<String> issues = new ArrayList<>();

        String gitBuildNumber = dump.required("gitInfo").required("buildNumber").stringValue();

        // Make sure this has a build number
        if (gitBuildNumber.equals("-1")) {
            return issues;
        }

        String versionString = dump.required("versionInfo").required("version").stringValue();

        // Make sure the version string is valid
        Matcher matcher = VERSION_PATTERN.matcher(versionString);
        if (!matcher.matches()) {
            return issues;
        }

        // Get the version and build numbers
        String versionNumber = matcher.group(1);
        int buildNumber = Integer.parseInt(matcher.group(2));

        String sha256Hash = dump.required("hash").stringValue();

        // Get hashes for the current build number and one either side
        // this prevents a wrong hash being flagged when a race condition in the build system occurs

        List<String> hashes = new ArrayList<>();

        hashes.addAll(getHashesForBuild(versionNumber, buildNumber - 1));
        hashes.addAll(getHashesForBuild(versionNumber, buildNumber));
        hashes.addAll(getHashesForBuild(versionNumber, buildNumber + 1));

        // Couldnt find any hashes
        if (hashes.isEmpty()) {
            return issues;
        }

        for (String hash : hashes) {
            if (hash.equals(sha256Hash)) {
                return issues; // All good
            }
        }

        // We didnt match any hashes
        issues.add("Your Geyser jar is corrupt or has been tampered with. Please re-download it [from the website](https://geysermc.org/download/).");

        return issues;
    }

    private List<String> getHashesForBuild(String versionNumber, int buildNumber) {
        List<String> hashes = new ArrayList<>();

        JsonNode response = RestClient.get("https://download.geysermc.org/v2/projects/geyser/versions/" + versionNumber + "/builds/" + buildNumber).asJson();

        // Couldnt find the build
        if (response.has("error")) {
            return hashes;
        }

        for (JsonNode download : response.required("downloads").values()) {
            hashes.add(download.required("sha256").stringValue());
        }

        return hashes;
    }
}
