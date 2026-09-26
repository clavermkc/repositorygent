package com.example.repoanalyzer.dto;

import java.util.List;

public record LlmAnalysisResult(
        String projectSummary,
        List<TechnologyEvidence> technologies,
        List<String> skills
) {
}
