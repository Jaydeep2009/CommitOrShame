package com.commitOrShame.backend.controller;

import com.commitOrShame.backend.dto.ContributionGraphResponse;
import com.commitOrShame.backend.dto.StreakResponse;
import com.commitOrShame.backend.entity.User;
import com.commitOrShame.backend.repository.UserRepository;
import com.commitOrShame.backend.service.GitHubGraphQLService;
import com.commitOrShame.backend.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/streak")
@RequiredArgsConstructor
public class StreakController {

    private final GitHubGraphQLService gitHubGraphQLService;
    private final StreakService streakService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getStreak(Authentication authentication) {
        try {
            String githubId = authentication.getName();

            User user = userRepository.findByGithubId(githubId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ContributionGraphResponse graph = gitHubGraphQLService
                    .fetchContributionGraph(user.getUsername(), user.getAccessToken());

            StreakResponse streak = streakService.calculateStreak(graph);
            return ResponseEntity.ok(streak);

        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}