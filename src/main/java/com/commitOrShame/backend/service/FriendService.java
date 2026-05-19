package com.commitOrShame.backend.service;

import com.commitOrShame.backend.dto.ContributionGraphResponse;
import com.commitOrShame.backend.dto.LeaderboardEntry;
import com.commitOrShame.backend.dto.StreakResponse;
import com.commitOrShame.backend.entity.Friendship;
import com.commitOrShame.backend.entity.FriendshipStatus;
import com.commitOrShame.backend.entity.User;
import com.commitOrShame.backend.repository.FriendshipRepository;
import com.commitOrShame.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final GitHubGraphQLService gitHubGraphQLService;
    private final StreakService streakService;

    public String sendFriendRequest(User requester, String targetUsername) {
        User addressee = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + targetUsername));

        if (requester.getId().equals(addressee.getId())) {
            throw new RuntimeException("You cannot add yourself");
        }

        friendshipRepository.findBetween(requester, addressee).ifPresent(f -> {
            throw new RuntimeException(
                    "Friendship already exists with status: " + f.getStatus());
        });

        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(FriendshipStatus.PENDING);
        friendshipRepository.save(friendship);

        return "Friend request sent to " + targetUsername;
    }

    public String acceptFriendRequest(User currentUser, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!friendship.getAddressee().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Not authorised to accept this request");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new RuntimeException("Request is not pending");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
        return "Friend request accepted";
    }

    public List<Friendship> getPendingRequests(User user) {
        return friendshipRepository.findAllByAddresseeAndStatus(
                user, FriendshipStatus.PENDING);
    }

    public List<LeaderboardEntry> getLeaderboard(User currentUser) {
        List<Friendship> accepted = friendshipRepository
                .findAllByUserAndStatus(currentUser, FriendshipStatus.ACCEPTED);

        // Collect all users: friends + yourself
        List<User> users = new ArrayList<>();
        users.add(currentUser);
        for (Friendship f : accepted) {
            User friend = f.getRequester().getId().equals(currentUser.getId())
                    ? f.getAddressee()
                    : f.getRequester();
            users.add(friend);
        }

        // Fetch streak for each user
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (User user : users) {
            try {
                ContributionGraphResponse graph = gitHubGraphQLService
                        .fetchContributionGraph(user.getUsername(), user.getAccessToken());
                StreakResponse streak = streakService.calculateStreak(graph);

                entries.add(new LeaderboardEntry(
                        user.getUsername(),
                        user.getAvatarUrl(),
                        streak.getCurrentStreak(),
                        streak.getLongestStreak(),
                        streak.isCommittedToday()
                ));
            } catch (Exception e) {
                // Don't let one bad fetch kill the whole leaderboard
                entries.add(new LeaderboardEntry(
                        user.getUsername(),
                        user.getAvatarUrl(),
                        0, 0, false
                ));
            }
        }

        // Sort by current streak descending
        entries.sort(Comparator.comparingInt(LeaderboardEntry::getCurrentStreak).reversed());
        return entries;
    }
}