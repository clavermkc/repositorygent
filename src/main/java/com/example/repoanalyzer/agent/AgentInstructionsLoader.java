package com.example.repoanalyzer.agent;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class AgentInstructionsLoader {

    public String load() {
        try {
            ClassPathResource resource =
                    new ClassPathResource("agent-instructions.md");

            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load agent-instructions.md",
                    exception
            );
        }
    }
}
