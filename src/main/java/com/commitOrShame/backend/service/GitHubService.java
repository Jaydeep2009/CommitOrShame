package com.commitOrShame.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class GitHubService {
    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String clientSecret;

    private final WebClient webClient = WebClient.create();

    //step 1: Exchange code for GitHub access token
    public String exchangeCodeForToken(String code) {
        Map response = webClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .header("Accept", "application/json")
                .bodyValue(Map.of(
                        "client_id", clientId,
                        "client_secret", clientSecret,
                        "code", code
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if(response==null || response.containsKey("error")) {
            throw new RuntimeException("Failed to retrieve access token from GitHub");
        }

        return (String)response.get("access_token");
    }

    //step 2: Use access token to fetch user info from GitHub

    public Map fetchGithubUser(String accessToken){
        Map response = webClient.get()
                .uri("https://api.github.com/user")
                .header("Authorization", "token " + accessToken)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if(response==null || response.containsKey("message")) {
            throw new RuntimeException("Failed to retrieve user info from GitHub");
        }

        return response;
    }

}
