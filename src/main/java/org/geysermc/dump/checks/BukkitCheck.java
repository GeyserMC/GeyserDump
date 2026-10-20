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
public class BukkitCheck extends AbstractCheck {

    @Override
    public boolean compatiblePlatform(String platform) {
        return platform.equals("SPIGOT");
    }

    @Override
    public List<String> checkIssues(JsonNode dump) {
        JsonNode bootstrapInfo = dump.required("bootstrapInfo");
        String platformName = bootstrapInfo.required("platformName").stringValue();

        if (platformName.equals("CraftBukkit") && !bootstrapInfo.required("platformVersion").stringValue().contains("Spigot")) {
            return Collections.singletonList("Your server is running on CraftBukkit; please switch over to Spigot or Paper (run `!!bukkit` in <#613194762249437245> for more information).");
        }

        return Collections.emptyList();
    }
}
