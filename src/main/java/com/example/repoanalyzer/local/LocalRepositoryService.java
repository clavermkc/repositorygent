package com.example.repoanalyzer.local;

import com.example.repoanalyzer.context.RepositoryData;
import com.example.repoanalyzer.context.RepositoryFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Loads a repository from the local file system instead of the GitHub API.
 * Used in CI, where the repository has already been cloned by actions/checkout.
 */
@Service
public class LocalRepositoryService {

    private static final Logger log = LoggerFactory.getLogger(LocalRepositoryService.class);

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git", ".idea", ".vscode", "target", "build", "node_modules",
            "__pycache__", ".venv", "venv", ".gradle"
    );

    // Bigger files are almost never useful evidence (generated code, data dumps...).
    private static final long MAX_FILE_SIZE_BYTES = 512 * 1024;

    public RepositoryData loadRepository(Path directory, String projectName) {
        Path root = directory.toAbsolutePath().normalize();

        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Not a directory: " + root);
        }

        try (Stream<Path> paths = Files.walk(root)) {
            List<RepositoryFile> files = paths
                    .filter(path -> !path.equals(root))
                    .filter(path -> !isIgnored(root.relativize(path)))
                    .map(path -> toRepositoryFile(root, path))
                    .toList();

            return new RepositoryData(null, projectName, null, files);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read repository at " + root, exception);
        }
    }

    private boolean isIgnored(Path relativePath) {
        for (Path segment : relativePath) {
            if (IGNORED_DIRECTORIES.contains(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    private RepositoryFile toRepositoryFile(Path root, Path path) {
        // Same format as the GitHub tree API: relative path with '/' separators.
        String relativePath = root.relativize(path).toString().replace('\\', '/');

        if (Files.isDirectory(path)) {
            return new RepositoryFile(relativePath, "", true);
        }

        return new RepositoryFile(relativePath, readText(path), false);
    }

    private String readText(Path path) {
        try {
            if (Files.size(path) > MAX_FILE_SIZE_BYTES) {
                log.debug("Skipping large file {}", path);
                return "";
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (CharacterCodingException binaryFile) {
            /*
             * Binary files (images, jars...) are not valid UTF-8: they are kept
             * in the project structure but excluded from textual analysis.
             */
            return "";
        } catch (IOException exception) {
            log.warn("Could not read {}: {}", path, exception.getMessage());
            return "";
        }
    }
}
