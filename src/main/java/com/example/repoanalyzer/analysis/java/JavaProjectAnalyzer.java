package com.example.repoanalyzer.analysis.java;

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
import java.util.stream.Collectors;

@Component
public class JavaProjectAnalyzer implements ProjectAnalyzer {

    private static final Pattern IMPORT_PATTERN =
            Pattern.compile("(?m)^\\s*import\\s+(?:static\\s+)?([^;]+);");

    /*
     * V1 deliberately keeps method extraction simple.
     * It is not intended to replace a full Java parser.
     */
    private static final Pattern METHOD_PATTERN =
            Pattern.compile(
                    "(?m)^\\s*(?:(?:public|protected|private|static|final|synchronized|abstract|native)\\s+)*" +
                    "[\\w<>\\[\\], ?.@]+\\s+(\\w+)\\s*\\([^;{}]*\\)\\s*(?:throws[^\\{]+)?\\{");

    private static final Set<String> ROOT_FILES = Set.of(
            "README.md",
            "README",
            "pom.xml",
            "build.gradle",
            "build.gradle.kts",
            "Dockerfile",
            "docker-compose.yml",
            "docker-compose.yaml",
            "compose.yml",
            "compose.yaml",
            "application.yml",
            "application.yaml",
            "application.properties"
    );

    @Override
    public boolean supports(String language) {
        return "java".equalsIgnoreCase(language);
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
                .filter(file -> file.path().endsWith(".java"))
                .map(file -> new SourceFileEvidence(
                        file.path(),
                        extractImports(file.content()),
                        extractMethods(file.content())
                ))
                .toList();

        List<String> dependencyFiles = files.stream()
                .filter(file -> !file.directory())
                .map(RepositoryFile::path)
                .filter(path -> path.equals("pom.xml")
                        || path.equals("build.gradle")
                        || path.equals("build.gradle.kts"))
                .toList();

        return new AnalysisContext(
                repository.repository(),
                "Java",
                structure,
                rootFiles,
                sourceEvidence,
                dependencyFiles
        );
    }

    private boolean isRelevantRootFile(RepositoryFile file) {
        String name = file.path().substring(file.path().lastIndexOf('/') + 1);
        return !file.path().contains("/")
                && (ROOT_FILES.contains(name) || name.startsWith(".github"));
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
            imports.add(matcher.group(1).trim());
        }

        return imports;
    }

    private List<String> extractMethods(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        Matcher matcher = METHOD_PATTERN.matcher(content);
        List<String> methods = new ArrayList<>();

        while (matcher.find()) {
            methods.add(matcher.group(1));
        }

        return methods.stream().distinct().toList();
    }
}
