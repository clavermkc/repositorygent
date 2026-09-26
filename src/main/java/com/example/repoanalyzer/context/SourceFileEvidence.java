package com.example.repoanalyzer.context;

import java.util.List;

public record SourceFileEvidence(
        String path,
        List<String> imports,
        List<String> methods
) {
}
