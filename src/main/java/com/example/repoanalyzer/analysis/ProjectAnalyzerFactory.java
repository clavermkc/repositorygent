package com.example.repoanalyzer.analysis;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProjectAnalyzerFactory {

    private final List<ProjectAnalyzer> analyzers;

    public ProjectAnalyzerFactory(List<ProjectAnalyzer> analyzers) {
        this.analyzers = analyzers;
    }

    public ProjectAnalyzer getAnalyzer(String language) {
        return analyzers.stream()
                .filter(analyzer -> analyzer.supports(language))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("No analyzer available for language: " + language));
    }
}
