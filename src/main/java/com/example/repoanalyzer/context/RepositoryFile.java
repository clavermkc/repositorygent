package com.example.repoanalyzer.context;

public record RepositoryFile(
        String path,
        String content,
        boolean directory
) {
}
