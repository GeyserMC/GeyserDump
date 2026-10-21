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

package org.geysermc.dump;

import org.kohsuke.github.GHCompare;
import tools.jackson.databind.JsonNode;

import java.io.IOException;

public record GitData(
    String commit,
    String branch,
    int ahead,
    int behind,
    boolean latest,
    boolean fork,
    String url
) {
    public static GitData from(JsonNode gitInfo) {
        String gitUrl = gitInfo.required("git.remote.origin.url").stringValue().replaceAll("\\.git$", "");

        // Compare latest and current
        GHCompare compare = null;
        try {
            compare = GithubHandler.INSTANCE.getCompare(gitInfo.required("git.commit.id").stringValue());
        } catch (IOException e) {
            // TODO Log the error somewhere
//            MessageHelper.errorResponse(event, "Failed to get latest commit", "There was an issue trying to get the latest commit!\n" + e.getMessage());
            return null;
        }

        return new GitData(
            gitInfo.required("git.commit.id").stringValue(),
            gitInfo.required("git.branch").stringValue(),
            compare != null ? compare.getAheadBy() : 0,
            compare != null ? compare.getBehindBy() : 0,
            compare != null && compare.getBehindBy() == 0 && compare.getAheadBy() == 0,
            !gitUrl.startsWith("https://github.com/GeyserMC/Geyser"),
            gitUrl
        );
    }
}
