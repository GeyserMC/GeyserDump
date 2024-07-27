/*
 * Copyright (c) 2020-2024 GeyserMC. http://geysermc.org
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
 * @link https://github.com/GeyserMC/GeyserDiscordBot
 */

package org.geysermc.dump.checks;

import com.google.auto.service.AutoService;
import org.json.JSONObject;
import pw.chew.chewbotcca.util.RestClient;

import java.util.ArrayList;
import java.util.List;

@AutoService(AbstractCheck.class)
public class IntegrityCheck extends AbstractCheck {

    @Override
    public List<String> checkIssues(JSONObject dump) {
        List<String> issues = new ArrayList<>();

        try {
            String versionNumber = dump.getJSONObject("versionInfo").getString("version").split("-")[0];
            String buildNumber = dump.getJSONObject("gitInfo").getString("buildNumber");

            JSONObject buildInfo = RestClient.get("https://download.geysermc.org/v2/projects/geyser/versions/" + versionNumber + "/builds/" + buildNumber).asJSONObject();

            if (buildInfo.has("error")) {
                return issues;
            }

            String buildHash = dump.getJSONObject("hashInfo").getString("sha256Hash");

            boolean found = false;
            JSONObject downloads = buildInfo.getJSONObject("downloads");
            for (String downloadKey : downloads.keySet()) {
                String sha256 = downloads.getJSONObject(downloadKey).getString("sha256");

                if (sha256.equalsIgnoreCase(buildHash)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                issues.add("Your Geyser jar is corrupt or has been tampered with. Please re-download it [from the website](https://geysermc.org/download).");
            }
        } catch (Exception e) {
        }

        // Make sure this is an official build
//        if (!dump.getJSONObject("gitInfo").getString("git.build.host").equals("nukkitx.com")) {
//            return issues;
//        }

//        String md5Hash = dump.getJSONObject("hashInfo").getString("md5Hash");
//        String response = RestClient.get("https://ci.opencollab.dev/fingerprint/" + md5Hash + "/api/json").asString();
//
//        // Check if 404
//        if (response.startsWith("<html>")) {
//            issues.add("Your Geyser jar is corrupt or has been tampered with. Please re-download it [from the CI](https://ci.opencollab.dev/job/GeyserMC/job/Geyser/job/master/).");
//        }

        // TODO Re-implement using the new API

        return issues;
    }
}
