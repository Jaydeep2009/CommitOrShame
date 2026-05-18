package com.commitOrShame.backend.service;

import com.commitOrShame.backend.dto.ContributionDay;
import com.commitOrShame.backend.dto.ContributionGraphResponse;
import com.commitOrShame.backend.dto.ContributionWeek;
import com.commitOrShame.backend.dto.StreakResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class StreakService {

    public StreakResponse calculateStreak(ContributionGraphResponse graph) {

        // Flatten all days into a single ordered list (oldest → newest)
        List<ContributionDay> allDays = new ArrayList<>();
        for (ContributionWeek week : graph.getWeeks()) {
            allDays.addAll(week.getContributionDays());
        }

        if (allDays.isEmpty()) {
            return new StreakResponse(0, 0, false, null, 0);
        }

        LocalDate today = LocalDate.now();

        // --- Longest streak ---
        int longestStreak = 0;
        int runningLongest = 0;
        for (ContributionDay day : allDays) {
            if (day.getContributionCount() > 0) {
                runningLongest++;
                longestStreak = Math.max(longestStreak, runningLongest);
            } else {
                runningLongest = 0;
            }
        }

        // --- Current streak ---
        // Walk backwards from today. If today has no commit yet, we still
        // allow the streak to be alive (user has until midnight).
        // If yesterday has no commit, streak is broken.
        int currentStreak = 0;
        String lastCommitDate = null;

        // Reverse the list so index 0 = most recent day
        List<ContributionDay> reversed = new ArrayList<>(allDays);
        java.util.Collections.reverse(reversed);

        boolean streakAlive = true;
        LocalDate expectedDate = today;

        for (ContributionDay day : reversed) {
            LocalDate date = LocalDate.parse(day.getDate());

            // Skip future dates (shouldn't happen but safety check)
            if (date.isAfter(today)) continue;

            if (!date.equals(expectedDate)) {
                // Gap in dates — streak is broken
                break;
            }

            if (day.getContributionCount() > 0) {
                currentStreak++;
                if (lastCommitDate == null) lastCommitDate = day.getDate();
                expectedDate = date.minusDays(1);
            } else {
                // No commit on this day
                if (date.equals(today)) {
                    // Today hasn't ended yet — don't break, check yesterday
                    expectedDate = date.minusDays(1);
                } else {
                    // Yesterday (or earlier) had no commit — streak broken
                    break;
                }
            }
        }

        boolean committedToday = allDays.stream()
                .filter(d -> d.getDate().equals(today.toString()))
                .findFirst()
                .map(d -> d.getContributionCount() > 0)
                .orElse(false);

        return new StreakResponse(
                currentStreak,
                longestStreak,
                committedToday,
                lastCommitDate,
                graph.getTotalContributions()
        );
    }
}