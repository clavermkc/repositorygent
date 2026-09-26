package com.example.repoanalyzer.analysis.python;

import com.example.repoanalyzer.analysis.ProjectAnalyzer;
import com.example.repoanalyzer.context.AnalysisContext;
import com.example.repoanalyzer.context.RepositoryData;
import com.example.repoanalyzer.context.RepositoryFile;
import com.example.repoanalyzer.context.RootFile;
import com.example.repoanalyzer.context.SourceFileEvidence;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PythonProjectAnalyzer implements ProjectAnalyzer {

    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "(?m)^\\s*(?:from\\s+([^\\s]+)\\s+import\\s+([^#\\n]+)|import\\s+([^#\\n]+))");

    private static final Pattern FUNCTION_PATTERN =
            Pattern.compile("(?m)^\\s*(?:async\\s+)?def\\s+(\\w+)\\s*\\(");

    private static final Set<String> ROOT_FILES = Set.of(
            "README.md",
            "README",
            "requirements.txt",
            "requirements-dev.txt",
            "pyproject.toml",
            "Pipfile",
            "setup.py",
            "Dockerfile",
            "docker-compose.yml",
            "docker-compose.yaml",
            "compose.yml",
            "compose.yaml"
    );

    @Override
    public boolean supports(String language) {
        return "python".equalsIgnoreCase(language);
    }

    @Override
    public AnalysisContext analyze(RepositoryData repository) {
        List<RepositoryFile> files = repository.files();

        List<String> structure = files.stream()
                .filter(file -> !file.directory())
                .map(RepositoryFile::path)
                .sorted()
                .toList();

        List<RootFile> rootFiles = files.stream()
                .filter(file -> !file.directory())
                .filter(this::isRelevantRootFile)
                .filter(this::hasContent)
                .map(file -> new RootFile(file.path(), file.content()))
                .toList();

        List<SourceFileEvidence> sourceEvidence = files.stream()
                .filter(file -> !file.directory())
                .filter(file -> file.path().endsWith(".py"))
                .map(file -> new SourceFileEvidence(
                        file.path(),
                        extractImports(file.content()),
                        extractFunctions(file.content())
                ))
                .toList();

        List<String> dependencyFiles = files.stream()
                .filter(file -> !file.directory())
                .map(RepositoryFile::path)
                .filter(path -> path.equals("requirements.txt")
                        || path.equals("pyproject.toml")
                        || path.equals("Pipfile")
                        || path.equals("setup.py"))
                .toList();

        return new AnalysisContext(
                repository.repository(),
                "Python",
                structure,
                rootFiles,
                sourceEvidence,
                dependencyFiles
        );
    }

    private boolean isRelevantRootFile(RepositoryFile file) {
        String name = file.path().substring(file.path().lastIndexOf('/') + 1);
        return !file.path().contains("/") && ROOT_FILES.contains(name);
    }

    private boolean hasContent(RepositoryFile file) {
        return file.content() != null && !file.content().isBlank();
    }

    private List<String> extractImports(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        Matcher matcher = IMPORT_PATTERN.matcher(content);
        List<String> imports = new ArrayList<>();

        while (matcher.find()) {
            String fromModule = matcher.group(1);
            String importedNames = matcher.group(2);
            String importedModules = matcher.group(3);

            if (fromModule != null) {
                String suffix = importedNames == null ? "" : " -> " + importedNames.trim();
                imports.add(fromModule.trim() + suffix);
            } else if (importedModules != null) {
                imports.add(importedModules.trim());
            }
        }

        return imports;
    }

    private List<String> extractFunctions(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        Matcher matcher = FUNCTION_PATTERN.matcher(content);
        List<String> functions = new ArrayList<>();

        while (matcher.find()) {
            functions.add(matcher.group(1));
        }

        return functions.stream().distinct().toList();
    }
}
