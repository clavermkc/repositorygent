package com.example.repoanalyzer.llm;

import com.example.repoanalyzer.context.AnalysisContext;
import com.example.repoanalyzer.dto.LlmAnalysisResult;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 * Temporary implementation so the project is executable before an actual
 * LLM provider is connected.
 *
 * Replace this class later with OpenAI/Ollama/another provider.
 */
@Service
public class StubLlmService implements LlmService {

    @Override
    public LlmAnalysisResult analyze(AnalysisContext context) {
        return new LlmAnalysisResult(
                "LLM integration not configured yet.",
                List.of(),
                List.of()
        );
    }
}
