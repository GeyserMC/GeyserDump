/*
 * Copyright (c) 2020-2022 GeyserMC. http://geysermc.org
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
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@AutoService(AbstractCheck.class)
public class FloodgateCheck extends AbstractCheck {

    @Override
    public boolean compatiblePlatform(String platform) {
        return !platform.equals("STANDALONE");
    }

    @Override
    public List<String> checkIssues(JsonNode dump) {
        JsonNode bootstrapInfo = dump.required("bootstrapInfo");
        JsonNode configRemote = dump.required("config").required("remote");

        List<String> problems = new ArrayList<>();

        // Check plugins
        if (bootstrapInfo.has("plugins")) {
            String authType = configRemote.required("auth-type").stringValue();
            boolean needsFloodgate = authType.equalsIgnoreCase("floodgate");
            boolean needsFloodgateAuthType = false;

            for (JsonNode plugin : bootstrapInfo.required("plugins")) {
                // Check if floodgate is installed
                if (plugin.required("name").stringValue().toLowerCase().contains("floodgate")) {
                    // Check if its enabled
                    if (plugin.required("enabled").asBoolean()) {
                        needsFloodgate = false;
                        needsFloodgateAuthType = true;
                    }

                    // Check we aren't on an old version of 1.8
                    if (bootstrapInfo.has("platformAPIVersion") && bootstrapInfo.required("platformAPIVersion").stringValue().startsWith("1.8-R0.1")) {
                        problems.add("You run on an outdated and unsupported version of 1.8, you can download the latest Paper build (1.8.8) [here](https://papermc.io/api/v1/paper/1.8.8/443/download).");
                    }
                    break;
                }
            }

            // Add any problem messages relates to floodgate
            if (needsFloodgate) {
                problems.add("`auth-type` is set to `floodgate`, but you don't have Floodgate installed! Download it [here](https://geysermc.org/download#floodgate).");
            } else if (needsFloodgateAuthType && !authType.equalsIgnoreCase("floodgate")) {
                problems.add("You have Floodgate installed, but `auth-type` is set to `" + authType + "`! Set it to `floodgate` if you want to use Floodgate.");
            }
        }

        return problems;
    }
}
