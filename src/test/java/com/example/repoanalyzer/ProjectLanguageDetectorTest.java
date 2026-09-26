package com.example.repoanalyzer;

import com.example.repoanalyzer.analysis.ProjectLanguageDetector;
import com.example.repoanalyzer.context.RepositoryData;
import com.example.repoanalyzer.context.RepositoryFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectLanguageDetectorTest {

    @Test
    void detectsJava() {
        ProjectLanguageDetector detector = new ProjectLanguageDetector();

        RepositoryData repository = new RepositoryData(
                "owner",
                "repo",
                "main",
                List.of(
                        new RepositoryFile(
                                "src/main/java/App.java",
                                "class App {}",
                                false
                        )
                )
        );

        assertEquals("Java", detector.detect(repository));
    }

    @Test
    void detectsPython() {
        ProjectLanguageDetector detector = new ProjectLanguageDetector();

        RepositoryData repository = new RepositoryData(
                "owner",
                "repo",
                "main",
                List.of(
                        new RepositoryFile(
                                "app/main.py",
                                "print('hello')",
                                false
                        )
                )
        );

        assertEquals("Python", detector.detect(repository));
    }
}
