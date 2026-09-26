package com.example.repoanalyzer.context;

import java.util.List;

public record AnalysisContext(
        String projectName,
        String language,
        List<String> projectStructure,
        List<RootFile> rootFiles,
        List<SourceFileEvidence> sourceEvidence,
        List<String> dependencyFiles
) {
}
