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
