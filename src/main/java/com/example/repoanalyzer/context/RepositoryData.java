package com.example.repoanalyzer.context;

import java.util.List;

public record RepositoryData(
        String owner,
        String repository,
        String defaultBranch,
        List<RepositoryFile> files
) {
}
