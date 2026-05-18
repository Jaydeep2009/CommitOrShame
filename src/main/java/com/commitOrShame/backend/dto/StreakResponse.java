package com.commitOrShame.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreakResponse {
    private int currentStreak;
    private int longestStreak;
    private boolean committedToday;
    private String lastCommitDate;   // "2025-05-17"
    private int totalContributions;
}