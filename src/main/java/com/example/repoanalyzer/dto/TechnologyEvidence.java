package com.example.repoanalyzer.dto;

import java.util.List;

public record TechnologyEvidence(
        String name,
        String category,
        List<String> evidence
) {
}
