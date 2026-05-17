package com.commitOrShame.backend.controller;


import com.commitOrShame.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/github")
    public ResponseEntity<?> githubLogin(@RequestBody Map<String, String> request) {

        try{
            String code = request.get("code");
            if (code == null || code.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Code is required!"));
            }

            String jwt = authService.loginWithGitHub(code);
            return ResponseEntity.ok(Map.of(
                    "token", jwt,
                    "message", "Login successful!"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error", e.getMessage()
                    ));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(Map.of("message", "backend is reachable"));
    }
}
