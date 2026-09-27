#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.example.repoanalyzer"
BASE="src/main/java/${PACKAGE//.//}"
RESOURCE="src/main/resources"

echo "Applying Ollama LLM integration..."

mkdir -p "$BASE/llm" "$RESOURCE"

cat > "$BASE/llm/LlmService.java" <<'JAVA'
package com.example.repoanalyzer.llm;

import com.example.repoanalyzer.context.AnalysisContext;
import com.example.repoanalyzer.dto.LlmAnalysisResult;

public interface LlmService {

    LlmAnalysisResult analyze(
            AnalysisContext context,
            String agentInstructions
    );
}
JAVA

cat > "$BASE/llm/AnalysisPromptBuilder.java" <<'JAVA'
package com.example.repoanalyzer.llm;

import com.example.repoanalyzer.context.AnalysisContext;
import com.example.repoanalyzer.context.RootFile;
import com.example.repoanalyzer.context.SourceFileEvidence;
import org.springframework.stereotype.Component;

@Component
public class AnalysisPromptBuilder {

    public String build(AnalysisContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                Analyze the following software repository evidence.

                Identify:
                1. What the project appears to do.
                2. Technologies actually supported by the evidence.
                3. Technical skills demonstrated by the project.

                Be conservative:
                - Do not invent technologies.
                - Treat dependency declarations as evidence of availability,
                  not automatic proof of meaningful usage.
                - Use imports, methods/functions, filenames, project structure,
                  configuration files and README content as evidence.
                - Prefer concrete evidence over assumptions.
                - Return only the requested JSON.

                PROJECT NAME:
                """);

        prompt.append(context.projectName())
                .append("\n\nLANGUAGE:\n")
                .append(context.language())
                .append("\n\nPROJECT STRUCTURE:\n");

        for (String path : context.projectStructure()) {
            prompt.append("- ").append(path).append('\n');
        }

        prompt.append("\nROOT / CONFIGURATION FILES:\n");

        for (RootFile file : context.rootFiles()) {
            prompt.append("\n--- ")
                    .append(file.path())
                    .append(" ---\n")
                    .append(file.content())
                    .append('\n');
        }

        prompt.append("\nSOURCE EVIDENCE:\n");

        for (SourceFileEvidence file : context.sourceEvidence()) {
            prompt.append("\n--- ")
                    .append(file.path())
                    .append(" ---\n");

            prompt.append("imports:\n");
            for (String anImport : file.imports()) {
                prompt.append("- ").append(anImport).append('\n');
            }

            prompt.append("methods/functions:\n");
            for (String method : file.methods()) {
                prompt.append("- ").append(method).append('\n');
            }
        }

        prompt.append("\nDEPENDENCY MANIFESTS:\n");
        for (String dependencyFile : context.dependencyFiles()) {
            prompt.append("- ").append(dependencyFile).append('\n');
        }

        prompt.append("""

                Return JSON with exactly this shape:

                {
                  "projectSummary": "string",
                  "technologies": [
                    {
                      "name": "string",
                      "category": "string",
                      "evidence": ["string"]
                    }
                  ],
                  "skills": ["string"]
                }
                """);

        return prompt.toString();
    }
}
JAVA

cat > "$BASE/llm/OllamaLlmService.java" <<'JAVA'
package com.example.repoanalyzer.llm;

import com.example.repoanalyzer.context.AnalysisContext;
import com.example.repoanalyzer.dto.LlmAnalysisResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class OllamaLlmService implements LlmService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AnalysisPromptBuilder promptBuilder;
    private final String model;

    public OllamaLlmService(
            ObjectMapper objectMapper,
            AnalysisPromptBuilder promptBuilder,
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.model:llama3.2}") String model
    ) {
        this.objectMapper = objectMapper;
        this.promptBuilder = promptBuilder;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public LlmAnalysisResult analyze(
            AnalysisContext context,
            String agentInstructions
    ) {
        String userPrompt = promptBuilder.build(context);

        OllamaChatRequest request = new OllamaChatRequest(
                model,
                List.of(
                        new OllamaMessage("system", agentInstructions),
                        new OllamaMessage("user", userPrompt)
                ),
                false,
                "json"
        );

        JsonNode response = restClient.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("Ollama returned an empty response.");
        }

        JsonNode messageContent = response.path("message").path("content");

        if (!messageContent.isTextual()) {
            throw new IllegalStateException(
                    "Ollama response does not contain message.content."
            );
        }

        try {
            return objectMapper.readValue(
                    messageContent.asText(),
                    LlmAnalysisResult.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Ollama returned invalid analysis JSON: " + messageContent.asText(),
                    exception
            );
        }
    }

    private record OllamaChatRequest(
            String model,
            List<OllamaMessage> messages,
            boolean stream,
            String format
    ) {
    }

    private record OllamaMessage(
            String role,
            String content
    ) {
    }
}
JAVA

cat > "$BASE/agent/AgentService.java" <<'JAVA'
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

        String language =
                languageDetector.detect(repository);

        ProjectAnalyzer analyzer =
                analyzerFactory.getAnalyzer(language);

        AnalysisContext context =
                analyzer.analyze(repository);

        String instructions = instructionsLoader.load();

        return new AnalysisResponse(
                context,
                llmService.analyze(context, instructions)
        );
    }
}
JAVA

cat > "$RESOURCE/agent-instructions.md" <<'MD'
# Repository Analysis Agent

You analyze a software repository and infer its technologies,
tools and demonstrable technical skills from repository evidence.

## Available evidence

You may receive:

- project structure
- root files
- README content
- dependency manifests
- Java file paths
- Java imports
- Java methods
- Python file paths
- Python imports
- Python functions/methods

## Rules

1. Use only repository evidence.
2. Do not invent technologies.
3. A dependency being declared does not automatically mean
   the dependency is meaningfully used.
4. Prefer multiple concrete signals when available.
5. A project name or file name alone is weak evidence.
6. Distinguish "present" from "actually used" where possible.
7. Do not claim expertise level.
8. Return only valid JSON.

## Output

{
  "projectSummary": "What the project appears to do",
  "technologies": [
    {
      "name": "technology name",
      "category": "Programming Language | Framework | Library | Database | DevOps | Tool | Other",
      "evidence": [
        "concrete repository evidence"
      ]
    }
  ],
  "skills": [
    "demonstrable technical skill"
  ]
}
MD

if [[ -f "$BASE/llm/StubLlmService.java" ]]; then
  python3 - <<'PY'
from pathlib import Path
p = Path("src/main/java/com/example/repoanalyzer/llm/StubLlmService.java")
text = p.read_text(encoding="utf-8")
text = text.replace("import org.springframework.stereotype.Service;\n", "")
text = text.replace("@Service\npublic class StubLlmService", "public class StubLlmService")
p.write_text(text, encoding="utf-8")
PY
fi

if [[ -f "$RESOURCE/application.properties" ]]; then
  grep -q '^ollama.base-url=' "$RESOURCE/application.properties" || cat >> "$RESOURCE/application.properties" <<'PROP'

ollama.base-url=http://localhost:11434
ollama.model=llama3.2
PROP
else
  cat > "$RESOURCE/application.properties" <<'PROP'
ollama.base-url=http://localhost:11434
ollama.model=llama3.2
PROP
fi

echo
 echo "Done."
echo "Run: mvn test"
echo "Then make sure Ollama is running and the configured model is installed."
echo "API: POST http://localhost:8080/api/analyze"

