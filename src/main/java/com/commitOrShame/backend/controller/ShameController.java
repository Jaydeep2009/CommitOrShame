package com.commitOrShame.backend.controller;

import com.commitOrShame.backend.entity.ShameRecord;
import com.commitOrShame.backend.entity.User;
import com.commitOrShame.backend.repository.UserRepository;
import com.commitOrShame.backend.service.ShameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shame")
@RequiredArgsConstructor
public class ShameController {

    private final ShameService shameService;
    private final UserRepository userRepository;

    private User getUser(Authentication auth) {
        return userRepository.findByGithubId(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Your personal shame history
    @GetMapping("/history")
    public ResponseEntity<?> getShameHistory(Authentication auth) {
        List<ShameRecord> history = shameService.getShameHistory(getUser(auth));
        return ResponseEntity.ok(history);
    }

    // Which of your friends got shamed today
    @GetMapping("/today")
    public ResponseEntity<?> getTodaysShamedFriends(Authentication auth) {
        List<User> shamed = shameService.getTodaysShamedFriends(getUser(auth));
        return ResponseEntity.ok(shamed);
    }

    // Manually trigger the check (for testing — remove before prod)
    @PostMapping("/trigger")
    public ResponseEntity<?> triggerManually() {
        shameService.runDailyShameCheck();
        return ResponseEntity.ok(Map.of("message", "Shame check triggered"));
    }
}