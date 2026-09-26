package com.example.repoanalyzer.web;

import com.example.repoanalyzer.agent.AgentService;
import com.example.repoanalyzer.dto.AnalysisResponse;
import com.example.repoanalyzer.dto.AnalyzeRepositoryRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analyze")
public class AnalysisController {

    private final AgentService agentService;

    public AnalysisController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public ResponseEntity<AnalysisResponse> analyze(
            @RequestBody AnalyzeRepositoryRequest request
    ) {
        if (request == null
                || request.repositoryUrl() == null
                || request.repositoryUrl().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                agentService.analyze(request.repositoryUrl())
        );
    }
}
