# Repository Analysis Agent

A smart assistant designed to analyze GitHub repositories and extract meaningful technical insights (technologies, skills, project summary) using Large Language Models (LLMs). The project aims to automate the creation of CVs and portfolios by extracting skills and project data directly from repositories (e.g., triggered after a push).

## Important Clarification: What is a "Repository"?

In this project, the term **Repository** refers to a **GitHub Software Project** (e.g., `https://github.com/user/project`). 


## How it Works

The application follows a specialized "Evidence-based Analysis" pipeline to minimize LLM context usage while maximizing accuracy:

1.  **Fetching**: The `GitHubRepositoryService` retrieves the file tree and content from a GitHub URL.
2.  **Detection**: The `ProjectLanguageDetector` identifies if the project is Java, Python, etc.
3.  **Extraction**: Specialized analyzers (e.g., `JavaProjectAnalyzer`) extract "Evidence" instead of raw code.
    *   **SourceFileEvidence**: Captures file paths, imports, and method signatures.
    *   **RootFiles**: Captures full content of critical files like `pom.xml`, `README.md`, or `Dockerfile`.
4.  **Orchestration**: The `AgentService` bundles everything into an `AnalysisContext`.
5.  **LLM Analysis**: The context is formatted by `AnalysisPromptBuilder` into a technical evidence prompt, then sent to a local LLM (via `OllamaLlmService`) to generate a summary and verify technologies.

## Advantages

*   **Inference Efficiency**: By sending structured "evidence" instead of full source code, the system drastically reduces token consumption, resulting in faster and cheaper analysis.
*   **Reduced Hallucinations**: Grounding the LLM in verified technical fingerprints (imports, method signatures, manifests) prevents it from inventing functionality or misinterpreting empty/irrelevant files.
*   **Precision over Noise**: The analyzer filters out noise and unused declarations, ensuring the LLM focuses on technologies that are actually utilized in the codebase.

## Project Structure

*   `com.example.repoanalyzer.agent`: The "Brain" of the system. Contains the orchestrator (`AgentService`) and instruction loaders.
*   `com.example.repoanalyzer.analysis`: Language-specific logic for extracting technical fingerprints (Java/Python).
*   `com.example.repoanalyzer.context`: Data transfer objects (Records) representing the evidence collected.
*   `com.example.repoanalyzer.github`: Integration with the GitHub API.
*   `com.example.repoanalyzer.llm`: Interface and implementations for connecting to AI models. It uses `AnalysisPromptBuilder` to translate technical evidence into LLM prompts (Ollama is currently supported).
*   `com.example.repoanalyzer.web`: REST API entry point.

## Demonstration

When you analyze a repository like [Caching-Proxy](https://github.com/clavermkc/Caching-Proxy), the agent produces a structured output containing both the technical evidence and the AI analysis:

![Terminal Analysis Output](docs/images/terminal-analysis.png)

```json
{
  "evidence": {
    "projectName": "Caching-Proxy",
    "language": "Java",
    "projectStructure": [
      "README.md",
      "pom.xml",
      "src/main/java/com/claver/cachingproxy/CachingProxyApplication.java",
      "src/main/java/com/claver/cachingproxy/controller/ProxyController.java",
      "src/main/java/com/claver/cachingproxy/service/ProxyService.java"
    ],
    "sourceEvidence": [
      {
        "path": "src/main/java/com/claver/cachingproxy/service/ProxyService.java",
        "imports": [
          "org.springframework.stereotype.Service",
          "org.springframework.web.client.RestClient"
        ],
        "methods": ["processRequest", "clear"]
      }
    ]
  },
  "llmAnalysis": {
    "projectSummary": "A caching proxy server built with Java 21 and Spring Boot that forwards GET requests to an origin server, stores each response in an in-memory cache, and returns cached responses for repeated requests.",
    "technologies": [
      { "name": "Java", "category": "Programming Language" },
      { "name": "Spring Boot", "category": "Framework" },
      { "name": "Caffeine", "category": "Library" }
    ],
    "skills": [
      "Implementing a caching proxy server using Java and Spring Boot",
      "Using Caffeine for caching",
      "Configuring a Spring Boot application"
    ]
  }
}
```

## Technical Stack

*   **Java 21**
*   **Spring Boot 4.1.1**
*   **Spring Web / RestClient**
*   **Jackson** (JSON Processing)
*   **Ollama** (Local LLM orchestration)
*   **Lombok**

## Getting Started

### Prerequisites
*   JDK 21
*   Maven
*   [Ollama](https://ollama.com/) installed and running.

### Configuration
Configure your Ollama instance in `src/main/resources/application.properties`:
```properties
ollama.base-url=http://localhost:11434
ollama.model=llama3.2
```

### Running the application
```bash
mvn spring-boot:run
```

### Analyzing a Repository
Send a `POST` request to `/api/analyze`:

```json
{
  "repositoryUrl": "https://github.com/owner/repository"
}
```

## Current Status
The pipeline is functional and integrated with **Ollama**. The agent successfully fetches repository trees, extracts technical evidence for Java and Python, and uses a local model to generate structured technical summaries.
