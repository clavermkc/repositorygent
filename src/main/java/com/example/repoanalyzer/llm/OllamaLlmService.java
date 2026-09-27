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
    //TODO Add real log
        System.out.println("[OLLAMA 1] Building prompt...");

        String userPrompt = promptBuilder.build(context);

        System.out.println("[OLLAMA 2] Prompt built.");
        System.out.println("[OLLAMA 2] Prompt characters: " + userPrompt.length());
        System.out.println("[OLLAMA 2] Estimated tokens: " + (userPrompt.length() / 4));

        OllamaChatRequest request = new OllamaChatRequest(
                model,
                List.of(
                        new OllamaMessage("system", agentInstructions),
                        new OllamaMessage("user", userPrompt)
                ),
                false,
                "json"
        );

        System.out.println("[OLLAMA 3] Sending request to Ollama...");

        JsonNode response = restClient.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        System.out.println("[OLLAMA 4] Ollama response received.");

        // reste du code...
        if (response == null) {
            throw new IllegalStateException("Ollama returned an empty response.");
        }

        JsonNode messageContent = response.path("message").path("content");

        if (!messageContent.isString()) {
            throw new IllegalStateException(
                    "Ollama response does not contain message.content."
            );
        }

        System.out.println("[OLLAMA 4] Ollama response received.");

        System.out.println("[OLLAMA 5] message.content textual: "
                + messageContent.asString());

        System.out.println("[OLLAMA 5] message.content length: "
                + messageContent.asString().length());

        System.out.println("[OLLAMA 5] message.content:");
        System.out.println(messageContent.asString());

        System.out.println("[OLLAMA 6] Parsing LLM result...");

        System.out.println("[OLLAMA 4] Ollama response received.");


        System.out.println("[OLLAMA 5] message.content textual: "
                + messageContent.asString());

        System.out.println("[OLLAMA 5] message.content length: "
                + messageContent.asString().length());

        System.out.println("[OLLAMA 5] message.content:");
        System.out.println(messageContent.asString());

        System.out.println("[OLLAMA 6] Parsing LLM result...");

        try {
            LlmAnalysisResult result = objectMapper.readValue(
                    messageContent.asString(),
                    LlmAnalysisResult.class
            );

            System.out.println("[OLLAMA 7] Parsing successful.");

            return result;

        } catch (Exception exception) {
            System.out.println("[OLLAMA 7] Parsing failed.");
            exception.printStackTrace();

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
