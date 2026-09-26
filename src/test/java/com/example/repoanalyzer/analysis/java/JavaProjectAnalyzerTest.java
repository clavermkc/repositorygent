package com.example.repoanalyzer.analysis.java;

import com.example.repoanalyzer.context.AnalysisContext;
import com.example.repoanalyzer.context.RepositoryData;
import com.example.repoanalyzer.context.RepositoryFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaProjectAnalyzerTest {

    @Test
    void extractsImportsAndMethods() {
        JavaProjectAnalyzer analyzer = new JavaProjectAnalyzer();

        String source = """
                import org.springframework.stereotype.Service;
                import org.springframework.web.bind.annotation.RestController;

                public class UserService {

                    public void createUser() {
                    }

                    private String findUser() {
                        return "user";
                    }
                }
                """;

        RepositoryData repository = new RepositoryData(
                "owner",
                "repo",
                "main",
                List.of(
                        new RepositoryFile(
                                "src/main/java/UserService.java",
                                source,
                                false
                        )
                )
        );

        AnalysisContext context = analyzer.analyze(repository);

        assertEquals("Java", context.language());
        assertEquals(1, context.sourceEvidence().size());
        assertTrue(context.sourceEvidence().get(0).imports().contains(
                "org.springframework.stereotype.Service"
        ));
        assertTrue(context.sourceEvidence().get(0).methods().contains(
                "createUser"
        ));
    }
}
