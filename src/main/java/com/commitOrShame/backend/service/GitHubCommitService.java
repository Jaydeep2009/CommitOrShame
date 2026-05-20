package com.commitOrShame.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GitHubCommitService {

    private final WebClient webClient = WebClient.create("https://api.github.com");

    @SuppressWarnings("unchecked")
    public CommitSummary fetchTodaysLatestCommit(String username, String accessToken) {
        try {
            // Step 1 — get all repos the user pushed to today
            String today = LocalDate.now().toString();

            List<Map> events = webClient.get()
                    .uri("/users/{username}/events", username)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .collectList()
                    .block();

            if (events == null || events.isEmpty()) {
                return CommitSummary.empty();
            }

            // Step 2 — find the most recent PushEvent from today
            for (Map event : events)  {
                if (!"PushEvent".equals(event.get("type"))) continue;

                String createdAt = (String) event.get("created_at");
                if (createdAt == null || !createdAt.startsWith(today)) continue;

                // Repo info
                Map<String, Object> repo = (Map<String, Object>) event.get("repo");
                String repoName = (String) repo.get("name"); // "Jaydeep2009/CommitOrShame"
                String repoShortName = repoName.contains("/")
                        ? repoName.split("/")[1]
                        : repoName;

                // Commits in this push
                Map<String, Object> payload = (Map<String, Object>) event.get("payload");
                List<Map<String, Object>> commits =
                        (List<Map<String, Object>>) payload.get("commits");

                if (commits == null || commits.isEmpty()) continue;

                // Most recent commit message
                Map<String, Object> latestCommit = commits.get(commits.size() - 1);
                String message = (String) latestCommit.get("message");

                // Step 3 — fetch line stats for this commit
                String sha = (String) latestCommit.get("sha");
                int[] lines = fetchLineStats(repoName, sha, accessToken);

                return new CommitSummary(
                        message,
                        repoShortName,
                        lines[0],   // added
                        lines[1],   // deleted
                        false       // public repo
                );
            }

            // No public push today — could be private repo contributions
            return CommitSummary.privateOrEmpty();

        } catch (Exception e) {
            log.error("Failed to fetch commits for {}: {}", username, e.getMessage());
            return CommitSummary.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private int[] fetchLineStats(String repoFullName, String sha, String accessToken) {
        try {
            Map<String, Object> commitDetail = webClient.get()
                    .uri("/repos/{repo}/commits/{sha}", repoFullName, sha)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (commitDetail == null) return new int[]{0, 0};

            Map<String, Object> stats = (Map<String, Object>) commitDetail.get("stats");
            if (stats == null) return new int[]{0, 0};

            int additions = (Integer) stats.getOrDefault("additions", 0);
            int deletions = (Integer) stats.getOrDefault("deletions", 0);
            return new int[]{additions, deletions};

        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }

    // Inner record to carry the result cleanly
    public record CommitSummary(
            String message,
            String repoName,
            int linesAdded,
            int linesDeleted,
            boolean privateRepo
    ) {
        public static CommitSummary empty() {
            return new CommitSummary(null, null, 0, 0, false);
        }

        public static CommitSummary privateOrEmpty() {
            return new CommitSummary(
                    "Contributed to a private repo",
                    "private",
                    0, 0, true
            );
        }
    }
}