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
import tools.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;

@AutoService(AbstractCheck.class)
public class VersionCheck extends AbstractCheck {

    @Override
    public boolean compatiblePlatform(String platform) {
        return !platform.equals("STANDALONE");
    }

    @Override
    public List<String> checkIssues(JsonNode dump) {
        JsonNode bootstrapInfo = dump.required("bootstrapInfo");
        String platform = bootstrapInfo.required("platform").required("platformName").stringValue().toUpperCase();
        JsonNode jsonSupportedMinecraft = dump.required("versionInfo").required("mcInfo");
        List<String> supportedMinecraft;
        if (jsonSupportedMinecraft.get("javaVersions") instanceof JsonNode array && array.isArray()) {
            supportedMinecraft = array.valueStream().map(JsonNode::stringValue).toList();
        } else {
            supportedMinecraft = Collections.singletonList(jsonSupportedMinecraft.required("javaVersion").stringValue());
        }

        boolean isOldVersion = false;
        if (!(platform.equals("BUNGEECORD") || platform.equals("VELOCITY") || platform.equals("FABRIC"))) {
            for (String version : supportedMinecraft) {
                isOldVersion = !bootstrapInfo.required("platformVersion").stringValue().contains(version);
                if (!isOldVersion) {
                    break;
                }
            }
        }

        // Check if we are running an old server version

        // Check plugins
        if (bootstrapInfo.has("plugins")) {
            JsonNode plugins = bootstrapInfo.required("plugins");
            for (JsonNode plugin : plugins) {
                // Check if VV is installed
                if (plugin.has("name") && plugin.required("name").stringValue().equals("ViaVersion") && plugin.required("enabled").asBoolean()) {
                    isOldVersion = false;
                    break;
                }
            }
        }

        if (isOldVersion) {
            if (supportedMinecraft.size() > 1) {
                return Collections.singletonList("Your server is not on one the following Minecraft versions: " + supportedMinecraft + "! If you're on an old version you can use [ViaVersion](https://viaversion.com/viaversion).");
            } else {
                return Collections.singletonList("Your server is not on Minecraft " + supportedMinecraft.get(0) + "! If you're on an old version you can use [ViaVersion](https://viaversion.com/viaversion).");
            }
        }

        return Collections.emptyList();
    }
}
