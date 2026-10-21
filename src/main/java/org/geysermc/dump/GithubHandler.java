package org.geysermc.dump;

import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.geysermc.dump.config.MainConfig;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
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

    // Cache compares by the latest and dump commit, so a new latest commit stops matching old entries
    private final Cache<String, GHCompare> cachedCompares = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.DAYS)
        .build();

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

    public GHCompare getCompare(String commit) throws IOException {
        String latest = getLatestCommit().getSHA1();
        try {
            return cachedCompares.get(latest + ":" + commit, () -> getRepo().getCompare(latest, commit));
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        }
    }
}
