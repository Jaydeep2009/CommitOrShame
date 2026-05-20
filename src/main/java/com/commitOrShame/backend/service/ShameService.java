package com.commitOrShame.backend.service;

import com.commitOrShame.backend.dto.ContributionGraphResponse;
import com.commitOrShame.backend.dto.StreakResponse;
import com.commitOrShame.backend.entity.Friendship;
import com.commitOrShame.backend.entity.FriendshipStatus;
import com.commitOrShame.backend.entity.ShameRecord;
import com.commitOrShame.backend.entity.User;
import com.commitOrShame.backend.repository.FriendshipRepository;
import com.commitOrShame.backend.repository.ShameRecordRepository;
import com.commitOrShame.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShameService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final ShameRecordRepository shameRecordRepository;
    private final GitHubGraphQLService gitHubGraphQLService;
    private final StreakService streakService;

    public void runDailyShameCheck() {
        log.info("Starting daily shame check...");
        List<User> allUsers = userRepository.findAll();
        LocalDate today = LocalDate.now();
        int shamed = 0;

        for (User user : allUsers) {
            try {
                // Only shame users who have at least one friend
                // (no point shaming someone with no audience)
                List<Friendship> friends = friendshipRepository
                        .findAllByUserAndStatus(user, FriendshipStatus.ACCEPTED);
                if (friends.isEmpty()) continue;

                ContributionGraphResponse graph = gitHubGraphQLService
                        .fetchContributionGraph(user.getUsername(), user.getAccessToken());
                StreakResponse streak = streakService.calculateStreak(graph);

                // Shame condition: didn't commit today AND had an active streak
                if (!streak.isCommittedToday() && streak.getCurrentStreak() == 0
                        && streak.getLongestStreak() > 0) {

                    // Avoid duplicate records
                    boolean alreadyShamed = shameRecordRepository
                            .findByShamedUserAndShameDate(user, today)
                            .isPresent();

                    if (!alreadyShamed) {
                        ShameRecord record = new ShameRecord();
                        record.setShamedUser(user);
                        record.setShameDate(today);
                        record.setMissedStreak(streak.getLongestStreak());
                        record.setNotificationSent(false);
                        shameRecordRepository.save(record);
                        shamed++;
                        log.info("Shamed: {} (missed streak of {})",
                                user.getUsername(), streak.getLongestStreak());
                    }
                }

            } catch (Exception e) {
                log.error("Shame check failed for user {}: {}",
                        user.getUsername(), e.getMessage());
            }
        }

        log.info("Daily shame check complete. {} users shamed.", shamed);
    }

    // Called by ShameController to get a user's shame history
    public List<ShameRecord> getShameHistory(User user) {
        return shameRecordRepository.findByShamedUserOrderByShameDateDesc(user);
    }

    // Returns users in your friend group who were shamed today
    public List<User> getTodaysShamedFriends(User currentUser) {
        List<Friendship> friendships = friendshipRepository
                .findAllByUserAndStatus(currentUser, FriendshipStatus.ACCEPTED);

        List<User> shamedToday = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Friendship f : friendships) {
            User friend = f.getRequester().getId().equals(currentUser.getId())
                    ? f.getAddressee()
                    : f.getRequester();

            shameRecordRepository.findByShamedUserAndShameDate(friend, today)
                    .ifPresent(r -> shamedToday.add(friend));
        }

        return shamedToday;
    }
}