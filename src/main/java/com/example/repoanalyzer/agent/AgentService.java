package com.example.repoanalyzer.agent;

import com.example.repoanalyzer.analysis.ProjectAnalyzer;
import com.example.repoanalyzer.analysis.ProjectAnalyzerFactory;
import com.example.repoanalyzer.analysis.ProjectLanguageDetector;
import com.example.repoanalyzer.context.AnalysisContext;
import com.example.repoanalyzer.context.RepositoryData;
import com.example.repoanalyzer.dto.AnalysisResponse;
import com.example.repoanalyzer.github.GitHubRepositoryService;
import com.example.repoanalyzer.llm.LlmService;
import com.example.repoanalyzer.local.LocalRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final GitHubRepositoryService githubRepositoryService;
    private final LocalRepositoryService localRepositoryService;
    private final ProjectLanguageDetector languageDetector;
    private final ProjectAnalyzerFactory analyzerFactory;
    private final LlmService llmService;
    private final AgentInstructionsLoader instructionsLoader;

    public AgentService(
            GitHubRepositoryService githubRepositoryService,
            LocalRepositoryService localRepositoryService,
            ProjectLanguageDetector languageDetector,
            ProjectAnalyzerFactory analyzerFactory,
            LlmService llmService,
            AgentInstructionsLoader instructionsLoader
    ) {
        this.githubRepositoryService = githubRepositoryService;
        this.localRepositoryService = localRepositoryService;
        this.languageDetector = languageDetector;
        this.analyzerFactory = analyzerFactory;
        this.llmService = llmService;
        this.instructionsLoader = instructionsLoader;
    }

    /**
     * Analyzes a remote repository through the GitHub API (REST endpoint).
     */
    public AnalysisResponse analyze(String repositoryUrl) {
        log.info("Analysis started for {}", repositoryUrl);
        return analyze(githubRepositoryService.loadRepository(repositoryUrl));
    }

    /**
     * Analyzes a repository already present on disk (e.g. checked out by GitHub Actions).
     */
    public AnalysisResponse analyzeLocal(Path directory, String projectName) {
        log.info("Analysis started for local directory {}", directory);
        return analyze(localRepositoryService.loadRepository(directory, projectName));
    }

    private AnalysisResponse analyze(RepositoryData repository) {
        long startedAt = System.currentTimeMillis();
        log.debug("Repository loaded: {} files", repository.files().size());

        String language =
                languageDetector.detect(repository);

        ProjectAnalyzer analyzer =
                analyzerFactory.getAnalyzer(language);
        log.info("Language detected: {} (analyzer: {})",
                language, analyzer.getClass().getSimpleName());

        AnalysisContext context =
                analyzer.analyze(repository);
        log.debug("Evidence collected for {}: {} root files, {} source files, {} dependency files",
                context.projectName(),
                context.rootFiles().size(),
                context.sourceEvidence().size(),
                context.dependencyFiles().size());

        String instructions =
                instructionsLoader.load();

        AnalysisResponse response = new AnalysisResponse(
                context,
                llmService.analyze(context, instructions)
        );

        log.info("Analysis finished for {} in {} ms",
                repository.repository(), System.currentTimeMillis() - startedAt);

        return response;
    }
}
