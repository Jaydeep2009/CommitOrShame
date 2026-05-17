package com.commitOrShame.backend.service;

import com.commitOrShame.backend.dto.ContributionDay;
import com.commitOrShame.backend.dto.ContributionGraphResponse;
import com.commitOrShame.backend.dto.ContributionWeek;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GitHubGraphQLService {

    private final WebClient webClient = WebClient.create("https://api.github.com");

    private static final String CONTRIBUTION_QUERY = """
        query($username: String!) {
          user(login: $username) {
            contributionsCollection {
              contributionCalendar {
                totalContributions
                weeks {
                  contributionDays {
                    date
                    contributionCount
                    color
                  }
                }
              }
            }
          }
        }
        """;

    @SuppressWarnings("unchecked")
    public ContributionGraphResponse fetchContributionGraph(String username, String accessToken) {
        Map<String, Object> requestBody = Map.of(
                "query", CONTRIBUTION_QUERY,
                "variables", Map.of("username", username)
        );

        Map<String, Object> response = webClient.post()
                .uri("/graphql")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || response.containsKey("errors")) {
            throw new RuntimeException("GitHub GraphQL query failed: " +
                    (response != null ? response.get("errors") : "null response"));
        }

        // Drill into: data → user → contributionsCollection → contributionCalendar
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        Map<String, Object> user = (Map<String, Object>) data.get("user");

        if (user == null) {
            throw new RuntimeException("GitHub user not found: " + username);
        }

        Map<String, Object> collection = (Map<String, Object>)
                user.get("contributionsCollection");
        Map<String, Object> calendar = (Map<String, Object>)
                collection.get("contributionCalendar");

        int totalContributions = (Integer) calendar.get("totalContributions");

        List<Map<String, Object>> rawWeeks =
                (List<Map<String, Object>>) calendar.get("weeks");

        List<ContributionWeek> weeks = new ArrayList<>();
        for (Map<String, Object> rawWeek : rawWeeks) {
            List<Map<String, Object>> rawDays =
                    (List<Map<String, Object>>) rawWeek.get("contributionDays");

            List<ContributionDay> days = new ArrayList<>();
            for (Map<String, Object> rawDay : rawDays) {
                days.add(new ContributionDay(
                        (String) rawDay.get("date"),
                        (Integer) rawDay.get("contributionCount"),
                        (String) rawDay.get("color")
                ));
            }
            weeks.add(new ContributionWeek(days));
        }

        return new ContributionGraphResponse(username, totalContributions, weeks);
    }
}