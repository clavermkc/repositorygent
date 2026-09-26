package com.example.repoanalyzer.github;

import com.example.repoanalyzer.context.RepositoryData;
import com.example.repoanalyzer.context.RepositoryFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service

public class GitHubRepositoryService {

    private final RestClient restClient;

    public GitHubRepositoryService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
    }

    public RepositoryData loadRepository(String repositoryUrl) {
        RepositoryCoordinates coordinates = parseRepositoryUrl(repositoryUrl);

        JsonNode repository = restClient.get()
                .uri("/repos/{owner}/{repo}", coordinates.owner(), coordinates.repository())
                .retrieve()
                .body(JsonNode.class);

        if (repository == null) {
            throw new IllegalStateException("GitHub repository metadata could not be loaded.");
        }

        String defaultBranch = repository.path("default_branch").asText("main");

        String treeUrl =
                "/repos/" + coordinates.owner() + "/" + coordinates.repository()
                        + "/git/trees/" + defaultBranch + "?recursive=1";

        JsonNode tree = restClient.get()
                .uri(treeUrl)
                .retrieve()
                .body(JsonNode.class);

        if (tree == null || !tree.has("tree")) {
            throw new IllegalStateException("GitHub repository tree could not be loaded.");
        }

        List<RepositoryFile> files = new ArrayList<>();

        for (JsonNode node : tree.path("tree")) {
            String path = node.path("path").asText();
            String type = node.path("type").asText();

            if ("tree".equals(type)) {
                files.add(new RepositoryFile(path, "", true));
                continue;
            }

            if (!"blob".equals(type)) {
                continue;
            }

            String content = readRawFile(
                    coordinates.owner(),
                    coordinates.repository(),
                    defaultBranch,
                    path
            );

            files.add(new RepositoryFile(path, content, false));
        }

        return new RepositoryData(
                coordinates.owner(),
                coordinates.repository(),
                defaultBranch,
                files
        );
    }

    private String readRawFile(
            String owner,
            String repository,
            String branch,
            String path
    ) {
        String rawUrl = "https://raw.githubusercontent.com/"
                + owner + "/"
                + repository + "/"
                + branch + "/"
                + path;

        try {
            return RestClient.builder()
                    .build()
                    .get()
                    .uri(rawUrl)
                    .accept(MediaType.TEXT_PLAIN)
                    .retrieve()
                    .body(String.class);
        } catch (Exception ignored) {
            /*
             * Binary/unsupported/unavailable files are intentionally omitted
             * from textual analysis rather than failing the entire repository.
             */
            return "";
        }
    }

    private RepositoryCoordinates parseRepositoryUrl(String repositoryUrl) {
        String normalized = repositoryUrl
                .trim()
                .replace("https://github.com/", "")
                .replace("http://github.com/", "")
                .replaceAll("/+$", "")
                .replaceFirst("\\.git$", "");

        String[] parts = normalized.split("/");

        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid GitHub repository URL. Expected https://github.com/{owner}/{repo}"
            );
        }

        return new RepositoryCoordinates(parts[0], parts[1]);
    }

    private record RepositoryCoordinates(
            String owner,
            String repository
    ) {
    }
}
