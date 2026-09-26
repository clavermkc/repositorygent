package com.example.repoanalyzer.agent;

import com.example.repoanalyzer.analysis.ProjectAnalyzer;
import com.example.repoanalyzer.analysis.ProjectAnalyzerFactory;
import com.example.repoanalyzer.analysis.ProjectLanguageDetector;
import com.example.repoanalyzer.context.AnalysisContext;
import com.example.repoanalyzer.context.RepositoryData;
import com.example.repoanalyzer.dto.AnalysisResponse;
import com.example.repoanalyzer.github.GitHubRepositoryService;
import com.example.repoanalyzer.llm.LlmService;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private final GitHubRepositoryService githubRepositoryService;
    private final ProjectLanguageDetector languageDetector;
    private final ProjectAnalyzerFactory analyzerFactory;
    private final LlmService llmService;
    private final AgentInstructionsLoader instructionsLoader;

    public AgentService(
            GitHubRepositoryService githubRepositoryService,
            ProjectLanguageDetector languageDetector,
            ProjectAnalyzerFactory analyzerFactory,
            LlmService llmService,
            AgentInstructionsLoader instructionsLoader
    ) {
        this.githubRepositoryService = githubRepositoryService;
        this.languageDetector = languageDetector;
        this.analyzerFactory = analyzerFactory;
        this.llmService = llmService;
        this.instructionsLoader = instructionsLoader;
    }

    public AnalysisResponse analyze(String repositoryUrl) {
        RepositoryData repository =
                githubRepositoryService.loadRepository(repositoryUrl);

        String language = languageDetector.detect(repository);

        ProjectAnalyzer analyzer =
                analyzerFactory.getAnalyzer(language);

        AnalysisContext context = analyzer.analyze(repository);

        /*
         * The instructions are loaded now so the LLM adapter can use them.
         * The current stub does not call a provider yet.
         */
        instructionsLoader.load();

        return new AnalysisResponse(
                context,
                llmService.analyze(context)
        );
    }
}
