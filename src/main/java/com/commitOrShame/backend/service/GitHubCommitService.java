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
            String today = LocalDate.now(java.time.ZoneOffset.UTC).toString();

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

            log.info("Fetched {} events for {}", events.size(), username);
            for (Map event : events) {
                String type = (String) event.get("type");
                String createdAt = (String) event.get("created_at");
                log.info("Event: type={} createdAt={}", type, createdAt);
                if ("PushEvent".equals(type)) break;
            }

            // Step 2 — find the most recent PushEvent from today
            for (Map event : events) {
                if (!"PushEvent".equals(event.get("type"))) continue;

                String createdAt = (String) event.get("created_at");
                if (createdAt == null || !createdAt.startsWith(today)) continue;

                // Repo info
                Map<String, Object> repo = (Map<String, Object>) event.get("repo");
                String repoName = (String) repo.get("name");
                String repoShortName = repoName.contains("/")
                        ? repoName.split("/")[1]
                        : repoName;

                // Get branch from payload ref
                Map<String, Object> payload = (Map<String, Object>) event.get("payload");
                String ref = (String) payload.get("ref");
                String branch = ref != null && ref.contains("/")
                        ? ref.substring(ref.lastIndexOf("/") + 1)
                        : "main";

                log.info("Fetching commits for repo={} branch={}", repoName, branch);

                // Step 3 — directly fetch commits on that branch from today
                try {
                    List<Map> commits = webClient.get()
                            .uri("/repos/" + repoName + "/commits"
                                    + "?sha=" + branch
                                    + "&author=" + username
                                    + "&since=" + today + "T00:00:00Z")
                            .header("Authorization", "Bearer " + accessToken)
                            .header("Accept", "application/vnd.github+json")
                            .retrieve()
                            .bodyToFlux(Map.class)
                            .collectList()
                            .block();
                    if (commits == null || commits.isEmpty()) continue;

                    log.info("Found {} commits in repo {}", commits.size(), repoName);

                    // Most recent commit
                    Map<String, Object> latestCommit = (Map<String, Object>) commits.get(0);
                    Map<String, Object> commitData = (Map<String, Object>) latestCommit.get("commit");
                    String message = (String) commitData.get("message");
                    String sha = (String) latestCommit.get("sha");

                    log.info("Latest commit message: {}", message);

                    // Step 4 — fetch line stats
                    int[] lines = fetchLineStats(repoName, sha, accessToken);

                    return new CommitSummary(
                            message,
                            repoShortName,
                            lines[0],
                            lines[1],
                            false
                    );

                } catch (Exception e) {
                    log.error("Failed to fetch commits for repo {}: {}", repoName, e.getMessage());
                    continue;
                }
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
            // In fetchLineStats — replace the WebClient call with:
            Map commitDetail = webClient.get()
                    .uri("https://api.github.com/repos/" + repoFullName + "/commits/" + sha)
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