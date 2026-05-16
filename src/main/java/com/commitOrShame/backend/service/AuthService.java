package com.commitOrShame.backend.service;

import com.commitOrShame.backend.entity.User;
import com.commitOrShame.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final GitHubService gitHubService;
    private final JwtService jwtService;

    public String loginWithGitHub(String code) {

        // Step 1: Exchange code for GitHub access token
        String accessToken = gitHubService.exchangeCodeForToken(code);

        // Step 2: Fetch GitHub user profile
        Map githubUser = gitHubService.fetchGithubUser(accessToken);

        String githubId = String.valueOf(githubUser.get("id"));
        String username = (String) githubUser.get("login");
        String avatarUrl = (String) githubUser.get("avatar_url");
        String name = (String) githubUser.get("name");
        String email = (String) githubUser.get("email");

        // Step 3: Save or update user in Supabase
        Optional<User> existingUser = userRepository.findByGithubId(githubId);

        User user;
        if (existingUser.isPresent()) {
            // User already exists — update their info
            user = existingUser.get();
            user.setAccessToken(accessToken);
            user.setAvatarUrl(avatarUrl);
        } else {
            // New user — create record
            user = new User();
            user.setGithubId(githubId);
            user.setUsername(username);
            user.setAvatarUrl(avatarUrl);
            user.setName(name);
            user.setEmail(email);
            user.setAccessToken(accessToken);
        }

        userRepository.save(user);

        // Step 4: Generate and return JWT
        return jwtService.generateToken(githubId, username);
    }
}