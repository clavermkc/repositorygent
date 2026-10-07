package com.example.repoanalyzer.cli;

import com.example.repoanalyzer.agent.AgentService;
import com.example.repoanalyzer.dto.AnalysisResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Batch mode used by GitHub Actions: analyzes a local directory, writes the
 * result to a JSON file, then the application exits.
 *
 * Only active when --analysis.path is given, so the REST mode is unchanged.
 * Run with --spring.main.web-application-type=none to avoid starting Tomcat.
 */
@Component
@ConditionalOnProperty(name = "analysis.path")
public class CliAnalysisRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CliAnalysisRunner.class);

    private final AgentService agentService;
    private final ObjectMapper objectMapper;
    private final Path repositoryPath;
    private final Path outputPath;
    private final String projectName;

    public CliAnalysisRunner(
            AgentService agentService,
            ObjectMapper objectMapper,
            @Value("${analysis.path}") Path repositoryPath,
            @Value("${analysis.output:analysis.json}") Path outputPath,
            @Value("${analysis.name:}") String projectName
    ) {
        this.agentService = agentService;
        this.objectMapper = objectMapper;
        this.repositoryPath = repositoryPath;
        this.outputPath = outputPath;
        this.projectName = projectName;
    }

    @Override
    public void run(String... args) throws Exception {
        String name = projectName.isBlank()
                ? repositoryPath.toAbsolutePath().normalize().getFileName().toString()
                : projectName;

        AnalysisResponse response = agentService.analyzeLocal(repositoryPath, name);

        Files.writeString(
                outputPath,
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response)
        );

        log.info("Analysis written to {}", outputPath.toAbsolutePath());
    }
}
