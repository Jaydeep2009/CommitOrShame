package com.commitOrShame.backend.repository;

import com.commitOrShame.backend.entity.Friendship;
import com.commitOrShame.backend.entity.FriendshipStatus;
import com.commitOrShame.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // Find friendship between two users regardless of who requested
    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester = :userA AND f.addressee = :userB)
           OR (f.requester = :userB AND f.addressee = :userA)
    """)
    Optional<Friendship> findBetween(User userA, User userB);

    // All accepted friendships for a user
    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester = :user OR f.addressee = :user)
          AND f.status = :status
    """)
    List<Friendship> findAllByUserAndStatus(User user, FriendshipStatus status);

    // Incoming pending requests
    List<Friendship> findAllByAddresseeAndStatus(User addressee, FriendshipStatus status);
}