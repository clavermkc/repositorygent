package com.example.repoanalyzer.analysis;

import com.example.repoanalyzer.context.AnalysisContext;
import com.example.repoanalyzer.context.RepositoryData;

public interface ProjectAnalyzer {

    boolean supports(String language);

    AnalysisContext analyze(RepositoryData repository);
}
