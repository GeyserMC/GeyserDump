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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoService(AbstractCheck.class)
public class PlatformVersionCheck extends AbstractCheck {
    private static final Pattern PAPER_OLD_VERSION = Pattern.compile("git-Paper-(\\d+) \\(MC: (\\d+\\.\\d+(?:\\.\\d+)?)\\)");

    @Override
    public boolean compatiblePlatform(String platform) {
        return !platform.equals("STANDALONE");
    }

    @Override
    public List<String> checkIssues(JsonNode dump) throws JacksonException {
        List<String> problems = new ArrayList<>();

        JsonNode bootstrapInfo = dump.required("bootstrapInfo");
        String platformName = bootstrapInfo.required("platformName").stringValue();
        String platformVersion = bootstrapInfo.required("platformVersion").stringValue();
        String softwareName = platformName;

        boolean isLatest = switch (platformName) {
            case "Paper" -> {
                if (platformVersion.startsWith("git-Paper")) {
                    // Parse the old version format, e.g. "git-Paper-318 (MC: 1.20.2)"
                    Matcher matcher = PAPER_OLD_VERSION.matcher(platformVersion);
                    if (matcher.matches()) {
                        yield isLatestFillBuild("paper", matcher.group(2), Integer.parseInt(matcher.group(1)));
                    } else {
                        yield true; // Assume all is fine if we can't parse the version
                    }
                } else {
                    String[] versionParts = platformVersion.split("-");
                    yield isLatestFillBuild("paper", versionParts[0], Integer.parseInt(versionParts[1]));
                }
            }
            case "Purpur" -> {
                String[] versionParts = platformVersion.split("-");
                yield isLatestPurpurBuild(versionParts[0], Integer.parseInt(versionParts[1]));
            }
            case "Velocity" -> {
                String[] versionParts = platformVersion.split("-b");
                String build = versionParts[versionParts.length-1];

                yield isLatestFillBuild(
                        "velocity", platformVersion.split(" ")[0],
                        Integer.parseInt(build.replace(")", ""))
                );
            }
//            case "fabric" -> {
//                softwareName = "Fabric Loader";
//                yield isLatestFabricLoader(platformVersion);
//            }
            default -> true;
        };

        if (!isLatest) {
            problems.add("Your version of %s is out of date, please consider updating your server software.".formatted(softwareName));
        }

        return problems;
    }

    private static boolean isLatestFillBuild(String project, String projectVersion, int build) {
        RestClient.Response response = RestClient.get("https://fill.papermc.io/v3/projects/%s/versions/%s".formatted(project, projectVersion));

        if (!response.success()) return true; // Assume all is fine

        JsonNode fillData = response.asJson();

        JsonNode buildsArray = fillData.required("builds");

        int latestBuild = buildsArray.required(0).intValue();

        return latestBuild <= build;
    }

    private static boolean isLatestPurpurBuild(String minecraftVersion, int build) {
        RestClient.Response response = RestClient.get("https://api.purpurmc.org/v2/purpur/%s".formatted(minecraftVersion));

        if (!response.success()) return true; // Assume all is fine

        JsonNode responseData = response.asJson();
        JsonNode builds = responseData.required("builds");

        int latestBuild = Integer.parseInt(builds.required("latest").stringValue());

        return latestBuild <= build;
    }

    private static boolean isLatestFabricLoader(String currentFabricLoader) {
        RestClient.Response response = RestClient.get("https://meta.fabricmc.net/v2/versions/loader");

        if (!response.success()) return true; // Assume all is fine

        JsonNode buildsArray = response.asJson();

        JsonNode latestData = buildsArray.required(0);

        String separator = latestData.required("separator").stringValue();
        if (!separator.equals(".")) return false;

        List<Integer> latestVersionParts = Arrays.stream(latestData.required("version").stringValue().split("\\."))
                .map(Integer::parseInt).toList();
        List<Integer> currentVersionParts = Arrays.stream(currentFabricLoader.split("\\."))
                .map(Integer::parseInt).toList();

        if (currentVersionParts.get(0) < latestVersionParts.get(0)) return false;
        if (currentVersionParts.get(1) < latestVersionParts.get(1)) return false;
        return currentVersionParts.get(2) >= latestVersionParts.get(2);
    }
}
