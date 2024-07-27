package org.geysermc.dump;

import com.google.common.base.Suppliers;
import org.geysermc.dump.config.MainConfig;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@Scope
public class GithubHandler {
    public static GithubHandler INSTANCE;

    private GitHub github;
    private MainConfig mainConfig;

    // Cache the repo and latest commit for 5 minutes
    private final Supplier<GHRepository> cachedRepo = Suppliers.memoizeWithExpiration(() -> {
        try {
            return github.getRepository("GeyserMC/Geyser");
        } catch (IOException e) {
            return null;
        }
    }, 5, TimeUnit.MINUTES);
    private final Supplier<GHCommit> cachedLatestCommit = Suppliers.memoizeWithExpiration(() -> cachedRepo.get().listCommits()._iterator(1).next(), 5, TimeUnit.MINUTES);

    @Autowired
    public GithubHandler(MainConfig mainConfig) throws IOException {
        this.mainConfig = mainConfig;
        this.github = new GitHubBuilder().withOAuthToken(mainConfig.githubToken()).build();

        INSTANCE = this;
    }

    public GHRepository getRepo() {
        return cachedRepo.get();
    }

    public GHCommit getLatestCommit() {
        return cachedLatestCommit.get();
    }
}
