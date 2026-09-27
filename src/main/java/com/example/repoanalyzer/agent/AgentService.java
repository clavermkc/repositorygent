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

        System.out.println();
        System.out.println("========== AGENT START ==========");
        System.out.println("Repository URL: " + repositoryUrl);

        System.out.println("[1] Loading GitHub repository...");

        RepositoryData repository =
                githubRepositoryService.loadRepository(repositoryUrl);

        System.out.println("[2] GitHub repository loaded.");
        System.out.println("Repository files: " + repository.files().size());

        System.out.println("[3] Detecting language...");

        String language =
                languageDetector.detect(repository);

        System.out.println("[4] Language detected: " + language);

        System.out.println("[5] Selecting analyzer...");

        ProjectAnalyzer analyzer =
                analyzerFactory.getAnalyzer(language);

        System.out.println("[6] Analyzer selected: "
                + analyzer.getClass().getSimpleName());

        System.out.println("[7] Analyzing repository...");

        AnalysisContext context =
                analyzer.analyze(repository);

        System.out.println("[8] Repository analyzed.");

        System.out.println("Project: " + context.projectName());
        System.out.println("Root files: " + context.rootFiles().size());
        System.out.println("Source evidence: " + context.sourceEvidence().size());
        System.out.println("Dependency files: " + context.dependencyFiles().size());

        System.out.println("[9] Loading agent instructions...");

        String instructions =
                instructionsLoader.load();

        System.out.println("[10] Agent instructions loaded.");

        System.out.println("[11] Calling Ollama...");

        AnalysisResponse response = new AnalysisResponse(
                context,
                llmService.analyze(context, instructions)
        );

        System.out.println("[12] Ollama response received.");

        System.out.println("========== AGENT END ==========");
        System.out.println();

        return response;
    }
}
