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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@AutoService(AbstractCheck.class)
public class OfflineCheck extends AbstractCheck {

    private static final Set<String> POPULAR_AUTH_PLUGINS_MODS = Set.of(
        "easyauth",
        "authmc",
        "sessionguard",
        "authme",
        "authmereloaded",
        "nlogin",
        "loginsecurity",
        "oauthenticator",
        "ultimatelogin",
        "locklogin",
        "fastlogin"
    );

    @Override
    public List<String> checkIssues(JsonNode dump) throws JacksonException {
        List<String> problems = new ArrayList<>();
        JsonNode bootstrapInfo = dump.required("bootstrapInfo");
        boolean isOffline = !bootstrapInfo.required("onlineMode").asBoolean();

        if (isOffline) {
            problems.add("We do not support offline mode servers, please see `!!offline`.");
        } else {
            String foundAuthAddon = null;
            if (bootstrapInfo.has("mods")) {
                foundAuthAddon = findAuthAddon(bootstrapInfo.required("mods"));
            }
            if (foundAuthAddon == null && bootstrapInfo.has("plugins")) {
                foundAuthAddon = findAuthAddon(bootstrapInfo.required("plugins"));
            }

            if (foundAuthAddon != null) {
                problems.add("Server is in online mode, but authentication plugin/mod `" + foundAuthAddon + "` was found. This may interfere with authentication.");
            }
        }

        return problems;
    }

    private String findAuthAddon(JsonNode addons) {
        for (JsonNode addon : addons) {
            if (addon.path("enabled").asBoolean(true) && addon.has("name")) {
                String name = addon.required("name").stringValue();
                if (POPULAR_AUTH_PLUGINS_MODS.contains(name.toLowerCase())) {
                    return name;
                }
            }
        }
        return null;
    }
}
