package com.example.repoanalyzer.analysis;

import com.example.repoanalyzer.context.RepositoryData;
import org.springframework.stereotype.Component;

@Component
public class ProjectLanguageDetector {

    public String detect(RepositoryData repository) {
        boolean hasJava = repository.files().stream()
                .filter(file -> !file.directory())
                .anyMatch(file -> file.path().endsWith(".java"));

        if (hasJava) {
            return "Java";
        }

        boolean hasPython = repository.files().stream()
                .filter(file -> !file.directory())
                .anyMatch(file -> file.path().endsWith(".py"));

        if (hasPython) {
            return "Python";
        }

        throw new IllegalArgumentException(
                "Unsupported or undetected project language. V1 supports Java and Python.");
    }
}
