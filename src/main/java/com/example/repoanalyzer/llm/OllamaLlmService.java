package com.example.repoanalyzer.llm;

import com.example.repoanalyzer.context.AnalysisContext;
import com.example.repoanalyzer.dto.LlmAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class OllamaLlmService implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmService.class);

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

        // Rough estimate (~4 chars per token), useful to spot prompts larger than the model context.
        log.debug("Prompt built: {} chars, ~{} tokens",
                userPrompt.length(), userPrompt.length() / 4);

        OllamaChatRequest request = new OllamaChatRequest(
                model,
                List.of(
                        new OllamaMessage("system", agentInstructions),
                        new OllamaMessage("user", userPrompt)
                ),
                false,
                "json"
        );

        log.info("Sending analysis request to Ollama (model: {})", model);
        long startedAt = System.currentTimeMillis();

        JsonNode response = restClient.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        log.info("Ollama responded in {} ms", System.currentTimeMillis() - startedAt);

        if (response == null) {
            throw new IllegalStateException("Ollama returned an empty response.");
        }

        JsonNode messageContent = response.path("message").path("content");

        if (!messageContent.isString()) {
            throw new IllegalStateException(
                    "Ollama response does not contain message.content."
            );
        }

        log.debug("Ollama raw content ({} chars): {}",
                messageContent.asString().length(), messageContent.asString());

        try {
            return objectMapper.readValue(
                    messageContent.asString(),
                    LlmAnalysisResult.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Ollama returned invalid analysis JSON: "
                            + messageContent.asString(),
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
