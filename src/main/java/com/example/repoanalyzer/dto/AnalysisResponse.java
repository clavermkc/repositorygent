package com.example.repoanalyzer.dto;

import com.example.repoanalyzer.context.AnalysisContext;

public record AnalysisResponse(
        AnalysisContext evidence,
        LlmAnalysisResult llmAnalysis
) {
}
