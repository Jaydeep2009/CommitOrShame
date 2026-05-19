package com.commitOrShame.backend.controller;

import com.commitOrShame.backend.entity.Friendship;
import com.commitOrShame.backend.entity.User;
import com.commitOrShame.backend.repository.UserRepository;
import com.commitOrShame.backend.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final UserRepository userRepository;

    private User getUser(Authentication auth) {
        return userRepository.findByGithubId(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Send a friend request
    @PostMapping("/add")
    public ResponseEntity<?> addFriend(@RequestBody Map<String, String> body,
                                       Authentication auth) {
        try {
            String message = friendService.sendFriendRequest(
                    getUser(auth), body.get("username"));
            return ResponseEntity.ok(Map.of("message", message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Accept a pending request
    @PostMapping("/accept/{friendshipId}")
    public ResponseEntity<?> acceptRequest(@PathVariable Long friendshipId,
                                           Authentication auth) {
        try {
            String message = friendService.acceptFriendRequest(getUser(auth), friendshipId);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Incoming pending requests
    @GetMapping("/requests")
    public ResponseEntity<?> getPendingRequests(Authentication auth) {
        List<Friendship> requests = friendService.getPendingRequests(getUser(auth));
        return ResponseEntity.ok(requests);
    }

    // Leaderboard (you + friends ranked by streak)
    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard(Authentication auth) {
        try {
            return ResponseEntity.ok(friendService.getLeaderboard(getUser(auth)));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}