package com.example.repoanalyzer.llm;

import com.example.repoanalyzer.context.AnalysisContext;
import com.example.repoanalyzer.dto.LlmAnalysisResult;

public interface LlmService {


    LlmAnalysisResult analyze(
            AnalysisContext context,
            String agentInstructions
    );
}
