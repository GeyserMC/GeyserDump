package org.geysermc.dump;

import org.json.JSONObject;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHRepository;

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
    public static GitData from(JSONObject gitInfo) {
        String gitUrl = gitInfo.getString("git.remote.origin.url").replaceAll("\\.git$", "");

        GHRepository repo = GithubHandler.INSTANCE.getRepo();

        // Get the commit hash
        GHCommit latestCommit = GithubHandler.INSTANCE.getLatestCommit();

        // Compare latest and current
        GHCompare compare = null;
        try {
            compare = repo.getCompare(latestCommit, repo.getCommit(gitInfo.getString("git.commit.id")));
        } catch (IOException e) {
            // TODO Log the error somewhere
//            MessageHelper.errorResponse(event, "Failed to get latest commit", "There was an issue trying to get the latest commit!\n" + e.getMessage());
            return null;
        }

        return new GitData(
            gitInfo.getString("git.commit.id"),
            gitInfo.getString("git.branch"),
            compare != null ? compare.getAheadBy() : 0,
            compare != null ? compare.getBehindBy() : 0,
            compare != null && compare.getBehindBy() == 0 && compare.getAheadBy() == 0,
            !gitUrl.startsWith("https://github.com/GeyserMC/Geyser"),
            gitUrl
        );
    }
}
