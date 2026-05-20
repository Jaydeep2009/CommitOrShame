package com.commitOrShame.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaderboardEntry {
    private String username;
    private String avatarUrl;
    private int currentStreak;
    private int longestStreak;
    private boolean committedToday;

    // New fields
    private String lastCommitMessage;   // "implement JWT auth filter"
    private String lastCommitRepo;      // "CommitOrShame"
    private int linesAdded;
    private int linesDeleted;
    private boolean privateRepo;        // true → hide message, show placeholder
}